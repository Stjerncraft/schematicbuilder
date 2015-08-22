package com.wildex999.schematicbuilder.tiles;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.mutable.MutableInt;

import cpw.mods.fml.common.Optional;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.ResourceItem;
import com.wildex999.schematicbuilder.SchematicWorldCache;
import com.wildex999.schematicbuilder.exceptions.ExceptionLoad;
import com.wildex999.schematicbuilder.exceptions.ExceptionSave;
import com.wildex999.schematicbuilder.gui.IGuiWatchers;
import com.wildex999.schematicbuilder.gui.GuiSchematicBuilder;
import com.wildex999.schematicbuilder.gui.GuiSchematicBuilder.GUI;
import com.wildex999.schematicbuilder.network.MessageActionSchematicBuilder;
import com.wildex999.schematicbuilder.network.MessageActionSchematicBuilder.ActionType;
import com.wildex999.schematicbuilder.network.MessageBase;
import com.wildex999.schematicbuilder.network.MessageUpdateSchematicBuilder;
import com.wildex999.schematicbuilder.network.MessageUploadSchematic;
import com.wildex999.schematicbuilder.schematic.Schematic;
import com.wildex999.schematicbuilder.schematic.SchematicBlock;
import com.wildex999.schematicbuilder.schematic.SchematicLoader;
import com.wildex999.utils.FileChooser;
import com.wildex999.utils.ModLog;

import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.server.FMLServerHandler;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;

@Optional.Interface(iface = "cofh.api.energy.IEnergyHandler", modid = "CoFHAPI|energy")
public class TileSchematicBuilder extends TileEntity implements IGuiWatchers, cofh.api.energy.IEnergyHandler {

	protected final static String inventoryName = "Schematic Builder";
	protected InventoryBasic inventory;
	
	public BuilderState state;
	public String message;
	
	public static class Config {
		public int passCount = 2;
		public boolean placeFloor;
		public SchematicBlock floorBlock = new SchematicBlock(Blocks.dirt, (byte)0); //Floor to place below Schematic(If not air, moves build up by one)
		public boolean placeAir = true;
		
		public void toBytes(ByteBuf buf) {
			buf.writeInt(passCount);
			buf.writeBoolean(placeFloor);
			buf.writeInt(Block.getIdFromBlock(floorBlock.getBlock()));
			buf.writeByte(floorBlock.metaData);
			buf.writeBoolean(placeAir);
		}
		
		public void fromBytes(ByteBuf buf) {
			passCount = buf.readInt();
			placeFloor = buf.readBoolean();
			Block floorBlockTemp = Block.getBlockById(buf.readInt());
			byte floorBlockMeta = buf.readByte();
			floorBlock = new SchematicBlock(floorBlockTemp, floorBlockMeta);
			placeAir = buf.readBoolean();
		}
	}
	public Config config = new Config();
	
	//Upload to Client
	public static class UploadJob {
		byte[] data; //Data to send(Compressed)
		int offset; //How much has been sent
	}
	HashMap<EntityPlayerMP, UploadJob> uploadManager;
	
	public String schematicName;
	@SideOnly(Side.CLIENT)
	public String schematicAuthor;
	@SideOnly(Side.CLIENT)
	public int schematicWidth, schematicHeight, schematicLength;
	
	@SideOnly(Side.CLIENT)
	public File loadFileLocal;
	@SideOnly(Side.CLIENT)
	public String filePath;
	@SideOnly(Side.CLIENT)
	public SchematicWorldCache schematicCache;
	
	private Set<EntityPlayer> watchers;
	private int resourceUpdateFreq = 20; //Number of ticks between resource update to client
	private int resourceUpdateTimer;
	private ArrayList<ResourceItem> resourceUpdates; //Resources changed since last update
	
	public Schematic loadedSchematic;
	public HashMap<Short, ResourceItem> resources; //Resources for the currently loaded Schematic
	public String cachedSchematicFile; //Local file containing the cached Schematic
	
	public cofh.api.energy.EnergyStorage energyStorage;
	private int maxEnergy = 1000000;
	private int energyCostNOP = 10; //Energy cost of doing nothing(Skip air, block already exists etc.)
	private int energyCostPlace = 50; //Energy cost of placing a non-air block
	private float energyModifierPass2 = 0.5f; //Multiplier for energy cost in the second pass
	
	private SchematicBlock defaultBlock;
	private int direction; //Direction the Builder is facing(Build direction)
	private int lastX, lastY, lastZ; //Current offset in building
	public int buildX, buildY, buildZ; //Start position for building
	private int placedCount; 
	private boolean initialized; //Metadata init

	
	private int currentPass; //Multiple passes are required for some blocks
	
	private int maxSchematicBytes = 10485760;
	private int uploadTimeoutTicks = 200; //How many ticks since last data message before it fails
	private int lastUploadTicks = 0; //Ticks since last data message while receiving upload
	
	private FMLControlledNamespacedRegistry<Block> blockRegistry;
	private boolean debug = true; //TODO: Move to config
	private short tileVersion = 1; //Version number used to detect old saved Tiles and try to correctly handle loading them
	
	//Uploading
	@SideOnly(Side.CLIENT)
	private boolean uploadApproved; //Set to true when server allows us to start uploading
	@SideOnly(Side.CLIENT)
	private boolean uploadComplete; //Set to true when done sending data(But before server notify us of getting all the data)
	@SideOnly(Side.CLIENT)
	public boolean downloadComplete; //Set to true when done downloading Schematic from server
	
	private boolean inUpload; //Set to true when Client is uploading/Server is downloading
	private byte[] uploadData;
	private int uploadOffset;
	private EntityPlayerMP uploader; //Set by server to track who is uploading
	
	public TileSchematicBuilder() {
		inventory = new InventoryBasic(inventoryName, true, 1);
		state = BuilderState.IDLE;
		schematicName = "None";
		message = "";
		
		watchers = new HashSet<EntityPlayer>();
		blockRegistry = GameData.getBlockRegistry();
		defaultBlock = new SchematicBlock(Blocks.air, (byte) 0);
		
		if(ModSchematicBuilder.useEnergy)
			energyStorage = new cofh.api.energy.EnergyStorage(maxEnergy);
		
		loadedSchematic = null;
		resources = new HashMap<Short, ResourceItem>();
		uploadManager = new HashMap<EntityPlayerMP, UploadJob>();
		cachedSchematicFile = "";
		direction = 0;
		lastX = lastY = lastZ = 0;
		buildX = buildY = buildZ = 0;
		initialized = false;
		
		cleanUpload();
		uploader = null;
	}
	
	public IInventory getInventory() {
		return inventory;
	}
	
	//Get the GUI for this tile, if it's the current one
	@SideOnly(Side.CLIENT)
	public GuiSchematicBuilder.GUI getCurrentGUI() {
		net.minecraft.client.gui.GuiScreen currentGui = net.minecraft.client.Minecraft.getMinecraft().currentScreen;
		if(!(currentGui instanceof GuiSchematicBuilder.GUI))
			return null;
		
		return (GuiSchematicBuilder.GUI)currentGui;
	}
	
	private void updateGui() {
		GuiSchematicBuilder.GUI gui = getCurrentGUI();
		if(gui != null)
			gui.updateGui();
	}
	
	@Override
	public void updateEntity() {	
		if(!initialized) {
			direction = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
			buildX = xCoord;
			buildY = yCoord;
			buildZ = zCoord;
			if(loadedSchematic != null)
				updateDirection();
			initialized = true;
		}
		
		if(worldObj.isRemote)
		{
			clientUpdate();
			return;
		}
		
		if(state == BuilderState.BUILDING || state == BuilderState.NEEDRESOURCES)
			serverBuildUpdate();
		else if(state == BuilderState.DOWNLOADING)
			serverUploadUpdate();
		
		//Upload to client
		serverSendSchematicUpdate();
		
		if(++resourceUpdateTimer >= resourceUpdateFreq)
		{
			sendNetworkUpdateResources(null);
			resourceUpdateTimer = 0;
		}
	}
	
	public void onPlaceCached(String cachedFile, String schematicName) {
		this.schematicName = schematicName;
		this.cachedSchematicFile = cachedFile;
		
		loadSavedSchematic();
	}
	
	//Start sending the current schematic to a player
	//player: null to send to all watchers
	public void startSendSchematic(EntityPlayerMP player) {
		ArrayList<EntityPlayerMP> players = new ArrayList<EntityPlayerMP>();
		if(player == null)
		{
			for(EntityPlayer pl : watchers)
				players.add((EntityPlayerMP)pl);
		}
		else
			players.add(player);
		
		if(players.isEmpty() || loadedSchematic == null)
			return;
		
		//Prepare Schematic data
		ByteBuf buf = Unpooled.buffer();
		loadedSchematic.toBytes(buf);
		
		//Compress the Schematic data
		ByteArrayOutputStream byteStream;
		try {
			byteStream = new ByteArrayOutputStream();
			GZIPOutputStream compressedStream = new GZIPOutputStream(byteStream);
			compressedStream.write(buf.array());
			compressedStream.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		byte[] sendData = byteStream.toByteArray();
		
		for(EntityPlayerMP pl : players)
		{
			//Abort existing
			abortSendSchematic(pl);
			
			UploadJob job = new UploadJob();
			job.data = sendData;
			job.offset = 0;
			
			MessageBase netMessage = new MessageUploadSchematic(this, sendData.length);
			netMessage.sendToPlayer(pl);
			uploadManager.put(pl, job);
		}
	}
	
	public void serverSendSchematicUpdate() {
		Iterator<Entry<EntityPlayerMP,UploadJob>> it = uploadManager.entrySet().iterator();
		while(it.hasNext())
		{
			Entry<EntityPlayerMP,UploadJob> entry = it.next();
			EntityPlayerMP player = entry.getKey();
			UploadJob job = entry.getValue();
			
			//Check if player is still online(Handle players who have crashed)
			List<EntityPlayerMP> players = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
			if(!players.contains(player))
				it.remove();
			
			int dataLen = MessageUploadSchematic.packetSize;
			if(job.offset + dataLen > job.data.length)
				dataLen = job.data.length - job.offset;
			
			byte[] data = new byte[dataLen];
			System.arraycopy(job.data, job.offset, data, 0, dataLen);
			MessageBase msg = new MessageUploadSchematic(this, data);
			msg.sendToPlayer(player);
			
			job.offset += dataLen;
			
			if(job.offset >= job.data.length)
			{
				msg = new MessageUploadSchematic(this, false);
				msg.sendToPlayer(player);
				it.remove();
			}
			
		}
	}
	
	//Abort currently sending schematic
	//player: null to send to all watchers
	public void abortSendSchematic(EntityPlayerMP player) {
		ArrayList<EntityPlayerMP> players = new ArrayList<EntityPlayerMP>();
		if(player == null)
		{
			for(EntityPlayer pl : watchers)
				players.add((EntityPlayerMP)pl);
		}
		else
			players.add(player);
		
		for(EntityPlayerMP pl : players)
		{
			UploadJob job = uploadManager.get(pl);
			if(job == null)
				continue;
			
			MessageBase netMessage = new MessageUploadSchematic(this, true);
			netMessage.sendToPlayer(pl);
			uploadManager.remove(pl);
		}
		
	}
	
	//Do the Building on the server
	public void serverBuildUpdate() {
		if(loadedSchematic == null)
		{
			state = BuilderState.ERROR;
			message = "No Schematic loaded, incompatible state.";
			sendNetworkUpdateFull(null);
			return;
		}
		
		int width = loadedSchematic.getWidth();
		int height = loadedSchematic.getHeight();
		int length = loadedSchematic.getLength();
		int airRepeat = 5; //Allow 5 Air blocks per tick (TODO: Place in config)
		if(currentPass == 1)
			airRepeat = 10; //10 Air blocks per tick on pass 2
		SchematicBlock block = null;
		Block newBlock = null;
		
		int prevPlacedCount = placedCount;
		boolean needEnergy = false;
		
		do {
			int x = buildX + lastX;
			int y = buildY + lastY;
			int z = buildZ + lastZ + 1;

			if(currentPass >= 0)
				block = loadedSchematic.getBlock(lastX, lastY, lastZ);
			else
				block = config.floorBlock;
			
			switch(currentPass) {
			case -1: //Pass 0: Floor
			case 0: //Pass 1: Initial placement
				//Place the block
				if(block == null)
					block = defaultBlock;
				newBlock = block.getBlock();
				
				if(newBlock != Blocks.air  || config.placeAir)
				{
					if(ModSchematicBuilder.useEnergy)
					{
						if(energyStorage.getEnergyStored() < energyCostPlace)
						{
							needEnergy = true;
							break;
						}
						energyStorage.modifyEnergyStored(-energyCostPlace);
					}
					
					Block oldBlock = worldObj.getBlock(x, y, z);
					if(oldBlock != Blocks.air)
					{
						int updateFlag = 0;
						if(newBlock == Blocks.air)
							updateFlag = 2; //If we are only placing air, notify that we did so 
						worldObj.setBlock(x, y, z, Blocks.air, 0, updateFlag); //Make sure it's air before we do the "canPlace" test(Do not notify)
					}
					if(newBlock != Blocks.air) //Only place block if it isn't air(So we don't go replacing air blocks with air blocks)
					{
						if(newBlock.canPlaceBlockAt(worldObj, x, y, z))
							worldObj.setBlock(x, y, z, newBlock, block.metaData, 2);
					}
				}
				else 
				{
					if(ModSchematicBuilder.useEnergy)
					{
						if(energyStorage.getEnergyStored() < energyCostNOP)
							needEnergy = true;
						else
							energyStorage.modifyEnergyStored(-energyCostNOP);
					}
				}
				
				break;
			case 1: //Pass 2: Place missing blocks, and correct metadata
				if(block == null)
					block = defaultBlock;
				newBlock = block.getBlock();
				
				if(newBlock != Blocks.air)
				{
					if(ModSchematicBuilder.useEnergy)
					{
						if(energyStorage.getEnergyStored() < energyCostPlace*energyModifierPass2)
						{
							needEnergy = true;
							break;
						}
						energyStorage.modifyEnergyStored((int)-(energyCostPlace*energyModifierPass2));
					}
					
					Block placedBlock = worldObj.getBlock(x, y, z);
					if(placedBlock != newBlock)
						worldObj.setBlock(x, y, z, newBlock, block.metaData, 2);
					
					int placedMeta = worldObj.getBlockMetadata(x, y, z);
					if(placedMeta != block.metaData) //Reinforce the metadata, as some blocks change it when placed
					{
						worldObj.setBlockMetadataWithNotify(x, y, z, block.metaData, 0);
						worldObj.markBlockForUpdate(x, y, z); //Using the flags for notify will cause neighboring blocks to drop/change
					}
				}
				else 
				{
					if(ModSchematicBuilder.useEnergy)
					{
						if(energyStorage.getEnergyStored() < energyCostNOP*energyModifierPass2)
							needEnergy = true;
						else
							energyStorage.modifyEnergyStored((int)-(energyCostNOP*energyModifierPass2));
					}
				}
				
				break;
			}
			
			if(needEnergy)
			{
				stateMissingEnergy(true);
				break; 
			}

			lastX++;
			if(lastX >= width)
			{
				lastX = 0;
				lastZ++;
			}
			if(lastZ >= length)
			{
				lastZ = 0;
				lastY++;
				if(config.placeFloor)
				{
					//Start from correct Schematic position
					currentPass = 0;
					placedCount = 0;
					buildY++;
					lastY = 0;
				}
			}
			if(lastY >= height)
			{
				lastY = 0;
				if(++currentPass >= config.passCount)
				{
					//TODO: Do Finalizing checks etc.
					state = BuilderState.DONE;
					loadedSchematic = null; //Clear loaded schematic data
					sendNetworkUpdateFull(null);
				}
				else
				{
					lastX = lastY = lastZ = 0;
				}
				break;
			}
			
			placedCount++;
			stateContinue(false);
		} while(airRepeat-- > 0 && newBlock == Blocks.air);
		
		if(state == BuilderState.NEEDRESOURCES && prevPlacedCount != placedCount)
		{
			markDirty();
			return;
		}
		else if(state == BuilderState.NEEDRESOURCES)
			return;

		//Send progress update
		int count = width*height*length;
		count *= config.passCount;
		
		float percentage = placedCount / (float)count;
		NumberFormat format = new DecimalFormat("0.00");
		
		if(currentPass >= 0)
			message = format.format(percentage*100.0) + "%(Pass " + (currentPass+1) + ")";
		else
			message = "Placing Floor";
		
		markDirty();
		sendNetworkUpdateFull(null);
		
	}
	
	public void stateMissingEnergy(boolean send) {
		state = BuilderState.NEEDRESOURCES;
		message = "Energy";
		if(send)
			sendNetworkUpdateMessage(null);
	}
	
	public void stateMissingMaterial( boolean send) {
		state = BuilderState.NEEDRESOURCES;
		message = "Resources";
		if(send)
			sendNetworkUpdateMessage(null);
	}
	
	public void stateContinue(boolean send) {
		state = BuilderState.BUILDING;
		message = "";
		if(send)
			sendNetworkUpdateMessage(null);
	}
	
	//Do timeout check of uploads
	public void serverUploadUpdate() {
		lastUploadTicks++;
		if(lastUploadTicks > uploadTimeoutTicks)
		{
			state = BuilderState.ERROR;
			message = "Upload timed out!";
			cleanUpload();
			
			//Update all watchers
			sendNetworkUpdateFull(null);
		}
	}
	
	private void cleanUpload() {
		inUpload = false;
		uploadData = null;
	}
	
	@SideOnly(Side.CLIENT)
	public void clientUpdate() {
		if(state == BuilderState.CHOOSINGLOCAL)
			updateSchematicChoosing();
		
		if(state == BuilderState.UPLOADING)
			updateUploadSchematic();
			
	}
	
	@SideOnly(Side.CLIENT)
	public boolean loadSchematic(File file) {
		if(!canAcceptLoad())
			return false;
		
		state = BuilderState.CHOOSINGLOCAL;
		loadFileLocal = file;
		
		return true;
	}
	
	@SideOnly(Side.CLIENT)
	private void updateUploadSchematic() {
		if(!inUpload || !uploadApproved || uploadComplete)
			return;
		
		int dataLen = MessageUploadSchematic.packetSize;
		if(uploadOffset + dataLen > uploadData.length)
			dataLen = uploadData.length - uploadOffset;
		
		byte[] data = new byte[dataLen];
		System.arraycopy(uploadData, uploadOffset, data, 0, dataLen);
		MessageBase msg = new MessageUploadSchematic(this, data);
		msg.sendToServer();
		
		uploadOffset += dataLen;
		
		if(uploadOffset >= uploadData.length)
		{
			uploadComplete = true;
			msg = new MessageUploadSchematic(this, false);
			msg.sendToServer();
		}
	}
	
	@SideOnly(Side.CLIENT)
	public void updateSchematicChoosing() {
		if(state != BuilderState.CHOOSINGLOCAL || loadFileLocal == null)
			return;
		
		Schematic newSchematic = null;
		//TODO: Populate resources from Server(As they might have been swapped)
		HashMap<Short, MutableInt> blockCount = new HashMap<Short, MutableInt>();
		try {
			newSchematic = SchematicLoader.loadSchematic(loadFileLocal, blockCount);
			if(newSchematic.name.trim().isEmpty())
			{
				int lastIndex = loadFileLocal.getName().lastIndexOf('.');
				if(lastIndex == -1)
					newSchematic.name = loadFileLocal.getName();
				else
					newSchematic.name = loadFileLocal.getName().substring(0, lastIndex);
			}
		} catch (Exception e) {
			state = BuilderState.ERROR;
			message = e.getMessage();
			ModLog.logger.warn(message);
		}
		
		if(state != BuilderState.ERROR)
		{
			state = BuilderState.LOADING;
			loadedSchematic = newSchematic;
			populateResources(blockCount); //TODO: Get from server
			
			//Send Schematic to server
			uploadSchematicToServer();
		}
		
		loadFileLocal = null;
		updateGui();
	}
	
	@SideOnly(Side.CLIENT)
	public void uploadSchematicToServer() {
		inUpload = true;
		uploadApproved = false;
		uploadComplete = false;
		uploadOffset = 0;
		
		ByteBuf buf = Unpooled.buffer();
		loadedSchematic.toBytes(buf);
		//TODO: Do clear memory and save to cache file for later preview?
		//loadedSchematic = null; //Clear the used memory
		
		//Compress the Schematic data
		ByteArrayOutputStream byteStream;
		try {
			byteStream = new ByteArrayOutputStream();
			GZIPOutputStream compressedStream = new GZIPOutputStream(byteStream);
			compressedStream.write(buf.array());
			compressedStream.close();
		} catch (IOException e) {
			state = BuilderState.ERROR;
			message = "Failed to serialize and compress Schematic!";
			e.printStackTrace();
			return;
		}
		uploadData = byteStream.toByteArray();
		
		System.out.println("Uploading Schematic. Original Size: " + buf.array().length + ", Compressed Size: " + uploadData.length);
		
		MessageBase uploadMessage = new MessageUploadSchematic(this, uploadData.length);
		uploadMessage.sendToServer();
		
		state = BuilderState.UPLOADING;
		message = "Waiting for server...";
	}

	@SideOnly(Side.CLIENT)
	public void sendConfigToServer() {
		MessageBase msg = new MessageActionSchematicBuilder(this, config);
		msg.sendToServer();
	}
	
	@Override
	public void addWatcher(EntityPlayer player) {
		if(worldObj.isRemote)
			return;
		
		watchers.add(player);
		
		//Send initial Update
		sendNetworkUpdateFull((EntityPlayerMP)player);
	}

	@Override
	public void removeWatcher(EntityPlayer player) {
		if(worldObj.isRemote)
		{
			downloadComplete = true; //Stop download when no longer in GUI
			return;
		}
		
		//Stop sending Schematic to player
		abortSendSchematic((EntityPlayerMP)player);
		
		watchers.remove(player);
	}
	
	public void sendNetworkUpdateFull(EntityPlayerMP player) {
		
		String author;
		int width, height, length;
		if(loadedSchematic != null)
		{
			author = loadedSchematic.author;
			width = loadedSchematic.getWidth();
			height = loadedSchematic.getHeight();
			length = loadedSchematic.getLength();
		}
		else
		{
			author = "None";
			width = 0;
			height = 0;
			length = 0;
		}
		
		MessageBase netMessage = new MessageUpdateSchematicBuilder(this, state, schematicName, author, message, cachedSchematicFile, width, height, length, config);
		
		if(player == null)
		{
			for(EntityPlayer watcher : watchers)
			{
				if(state == BuilderState.DOWNLOADING && watcher == uploader)
						(new MessageUpdateSchematicBuilder(this, BuilderState.UPLOADING, message)).sendToPlayer((EntityPlayerMP)watcher);
				else
					netMessage.sendToPlayer((EntityPlayerMP)watcher);
			}
		}
		else
		{
			if(state == BuilderState.DOWNLOADING && player == uploader)
				(new MessageUpdateSchematicBuilder(this, BuilderState.UPLOADING, message)).sendToPlayer((EntityPlayerMP)player);
			else
				netMessage.sendToPlayer(player);
		}
	}
	
	public void sendNetworkUpdateMessage(EntityPlayerMP player) {
		MessageBase netMessage = new MessageUpdateSchematicBuilder(this, state, message);
		
		if(player == null)
		{
			for(EntityPlayer watcher : watchers)
			{
				if(state == BuilderState.DOWNLOADING && watcher == uploader)
					(new MessageUpdateSchematicBuilder(this, BuilderState.UPLOADING, message)).sendToPlayer((EntityPlayerMP)watcher);
				else
					netMessage.sendToPlayer((EntityPlayerMP)watcher);
			}
		}
		else
		{
			if(state == BuilderState.DOWNLOADING && player == uploader)
				(new MessageUpdateSchematicBuilder(this, BuilderState.UPLOADING, message)).sendToPlayer((EntityPlayerMP)player);
			else
				netMessage.sendToPlayer(player);
		}
	}
	
	public void sendNetworkUpdateResources(EntityPlayerMP player) {
		
		int energyCurrent = 0, energyMax = 0;
		if(ModSchematicBuilder.useEnergy)
		{
			energyCurrent = energyStorage.getEnergyStored();
			energyMax = energyStorage.getMaxEnergyStored();
		}
		
		//TODO: Compile list of changed Resources and send them
		
		MessageBase netMessage = new MessageUpdateSchematicBuilder(this, energyCurrent, energyMax, null);
		
		if(player == null)
		{
			for(EntityPlayer watcher : watchers)
			{
				if(state == BuilderState.DOWNLOADING && watcher == uploader)
					(new MessageUpdateSchematicBuilder(this, BuilderState.UPLOADING, message)).sendToPlayer((EntityPlayerMP)watcher);
				else
					netMessage.sendToPlayer((EntityPlayerMP)watcher);
			}
		}
		else
		{
			if(state == BuilderState.DOWNLOADING && player == uploader)
				(new MessageUpdateSchematicBuilder(this, BuilderState.UPLOADING, message)).sendToPlayer((EntityPlayerMP)player);
			else
				netMessage.sendToPlayer(player);
		}
	}

	@SideOnly(Side.CLIENT)
	public void networkUpdateFull(BuilderState newState, String schematicName, String schematicAuthor, String message, String schematicId, int schematicWidth, int schematicHeight, int schematicLength, Config config) {
		this.message = message;
		
		if(cachedSchematicFile == null || !cachedSchematicFile.equals(schematicId))
		{
			//Switched to new Schematic
			//Delete old cache
			if(cachedSchematicFile != null)
			{
				File cacheFolder = getCacheFolderClient();
				File cachedFile = new File(cacheFolder, cachedSchematicFile);
				if(cachedFile.exists())
					cachedFile.delete();
			}
			
			if(inUpload && uploadApproved)
			{
				if(schematicId.length() > 0)
				{
					//Write our Upload to cache
					File cacheFolder = getCacheFolderClient();
					File cacheFile = new File(cacheFolder, schematicId);
					try {
						SchematicLoader.saveSchematic(cacheFile, loadedSchematic);
					} catch (Exception e) {
						ModLog.printTileErrorPrefix(this);
						System.err.println("Failed to save cached Schematic file: " + schematicId);
						e.printStackTrace();
					}
				}
			}
			else
			{
				if(loadedSchematic != null)
					loadedSchematic = null; //Someone else swapped the Schematic, we need to re-download
				
				if(schematicId.length() > 0)
				{
					//Check if we have this schematic cached
					File cacheFolder = getCacheFolderClient();
					File cacheFile = new File(cacheFolder, schematicId);
					if(cacheFile.exists())
					{
						try {
							HashMap<Short, MutableInt> blockCount = new HashMap<Short, MutableInt>();
							loadedSchematic = SchematicLoader.loadSchematic(cacheFile, blockCount);
							populateResources(blockCount);
						} catch (Exception e) {
							ModLog.printTileErrorPrefix(this);
							System.err.println("Failed to load cached Schematic file: " + schematicId);
							e.printStackTrace();
						} 
					}
					else
					{
						//Request Schematic download
						//TODO: Config whether this is enabled or not
						MessageBase netMessage = new MessageActionSchematicBuilder(this, ActionType.DOWNLOAD);
						netMessage.sendToServer();
					}
				}
			}
		}
		this.cachedSchematicFile = schematicId;
		this.schematicName = schematicName;
		this.schematicAuthor = schematicAuthor;
		this.schematicWidth = schematicWidth;
		this.schematicHeight = schematicHeight;
		this.schematicLength = schematicLength;
		this.config = config;
		
		onClientStateChange(newState);
		updateGui();
	}

	@SideOnly(Side.CLIENT)
	public void networkUpdateMessage(BuilderState newState, String message) {
		this.message = message;
		
		onClientStateChange(newState);
		updateGui();
	}
	
	@SideOnly(Side.CLIENT)
	public void networkUpdateConfig(Config config) {
		this.config = config;
		updateGui();
	}
	
	@SideOnly(Side.CLIENT)
	public void networkUpdateResource(int energyCurrent, int energyMax, ArrayList<ResourceItem> resources) {
		if(ModSchematicBuilder.useEnergy)
		{
			energyStorage.setCapacity(energyMax);
			energyStorage.setEnergyStored(energyCurrent);
		}
		
		//TODO: Resources
		
		updateGui();
	}
	
	@SideOnly(Side.CLIENT)
	private void onClientStateChange(BuilderState newState) {
		//Check if Upload was accepted, Aborted or completed
		if(inUpload)
		{
			if(newState == BuilderState.UPLOADING)
				uploadApproved = true;
			else if(uploadApproved == true && newState == BuilderState.PREPARING)
			{
				//Server has received data, parsing it
			}
			else if(uploadApproved == true && newState == BuilderState.READY)
			{
				//Cleanup
				inUpload = false;
				uploadApproved = false;
			}
			else
			{
				//Upload either Aborted or done
				inUpload = false;
				uploadApproved = false;
			}
		}
		
		state = newState;
	}
	
	@SideOnly(Side.CLIENT)
	public boolean canAcceptLoad() {
		return state == BuilderState.IDLE || state == BuilderState.ERROR || state == BuilderState.READY
				|| state == BuilderState.DONE || state == BuilderState.DONEMISSING || state == BuilderState.STOPPED;
	}
	
	public boolean canAcceptUpload() {
		return state == BuilderState.IDLE || state == BuilderState.ERROR || state == BuilderState.READY
				|| state == BuilderState.DONE || state == BuilderState.DONEMISSING || state == BuilderState.STOPPED;
	}

	
	public void networkOnUploadStart(int size, EntityPlayerMP uploader) {
		//Only allow Uploads if we aren't doing anything else
		if(!canAcceptUpload())
		{
			if(debug)
			{
				ModLog.printTileInfoPrefix(this);
				System.out.println("Start upload failed due to not accepted from player: " + uploader.getDisplayName());
			}
			sendNetworkUpdateFull(uploader);
			return;
		}
		
		if(size < 0 || size > maxSchematicBytes)
		{
			if(debug)
			{
				ModLog.printTileInfoPrefix(this);
				System.out.println("Start upload failed due to file size(" + size + " bytes) from player: " + uploader.getDisplayName());
			}
			state = BuilderState.ERROR;
			message = "Upload failed due to illegal size: " + size + " bytes. Server max: " + maxSchematicBytes + " bytes.";
			sendNetworkUpdateFull(null);
			return;
		}
		if(debug)
		{
			ModLog.printTileInfoPrefix(this);
			System.out.println("Start upload(" + size + " bytes) from player: " + uploader.getDisplayName());
		}
		
		//Stop ant sending of previous Schematic
		abortSendSchematic(null);
		
		//Clear any existing Schematic
		loadedSchematic = null;
		cachedSchematicFile = "";
		
		//Set upload/downlaod state
		state = BuilderState.DOWNLOADING;
		message = "0%";
		inUpload = true;
		uploadOffset = 0;
		this.uploader = uploader;
		lastUploadTicks = 0;
		
		uploadData = new byte[size];
		markDirty();
		sendNetworkUpdateMessage(null);
	}
	
	public void networkOnUploadData(byte[] data, EntityPlayerMP uploader) {
		if(!inUpload || this.uploader != uploader)
			return;
		
		System.arraycopy(data, 0, uploadData, uploadOffset, data.length);
		uploadOffset += data.length;
		lastUploadTicks = 0;
		
		if(uploadOffset > uploadData.length)
		{
			cleanUpload();
			this.uploader = null;
			state = BuilderState.ERROR;
			message = "Recevied too much data during upload.";
			sendNetworkUpdateMessage(null);
			return;
		}
		
		//Update everyone on progress
		float progress = uploadOffset / (float) uploadData.length;
		NumberFormat format = new DecimalFormat("0.00");
		message = format.format(progress*100.0) + "%";
		
		sendNetworkUpdateMessage(null);
	}
	
	public void networkOnUploadEnd(EntityPlayerMP uploader, boolean abort) {
		if(!inUpload || this.uploader != uploader)
			return;
		
		//Client aborted
		if(abort)
		{
			state = BuilderState.IDLE;
			cleanUpload();
			markDirty();
			sendNetworkUpdateFull(null);
		}
		
		//Not received enough data
		if(uploadOffset != uploadData.length)
		{
			state = BuilderState.ERROR;
			message = "Did not receive full Schematic upload. Expected: " + uploadData.length + " bytes, got: " + uploadOffset;
			cleanUpload();
			markDirty();
			sendNetworkUpdateFull(null);
		}
		
		//Read the Schematic
		try {
			state = BuilderState.PREPARING;
			message = "Parsing";
			markDirty();
			sendNetworkUpdateFull(null);
			
			ByteArrayOutputStream decompressedStream = new ByteArrayOutputStream();
			
			IOUtils.copy(new GZIPInputStream(new ByteArrayInputStream(uploadData)), decompressedStream);

			
			ByteBuf buf = Unpooled.wrappedBuffer(decompressedStream.toByteArray());
			HashMap<Short, MutableInt> blockCount = new HashMap<Short, MutableInt>();
			loadedSchematic = Schematic.fromBytes(buf, blockCount);
			populateResources(blockCount);
			schematicName = loadedSchematic.name;
			
			cleanUpload();
			
			//Cleanup old cached schematic
			if(!cachedSchematicFile.isEmpty())
			{
				File saveFolder = getSaveFolderServer();
				File cachedFile = new File(saveFolder, cachedSchematicFile);
				if(cachedFile.exists())
					Files.delete(cachedFile.toPath());
			}
			
			//Cache uploaded schematic
			if(!saveLoadedSchematic())
				cachedSchematicFile = "";
			else
				System.out.println("Server cached name: " + cachedSchematicFile);
			
			message = "";
			
		} catch(Exception e) {
			state = BuilderState.ERROR;
			message = "Error while reading uploaded Schematic";
			
			ModLog.printTileErrorPrefix(this);
			System.err.println("Error while reading uploaded Schematic from: " + uploader.getDisplayName());
			e.printStackTrace();
		}
		
		uploader = null;
		state = BuilderState.READY;
		
		markDirty();
		sendNetworkUpdateFull(null);
	}
	
	@SideOnly(Side.CLIENT)
	public void networkOnDownloadStart(int size) {
		if(size > maxSchematicBytes || size <= 0)
			return;
		
		downloadComplete = false;
		uploadOffset = 0;
		uploadData = new byte[size];
	}
	
	@SideOnly(Side.CLIENT)
	public void networkOnDownloadData(byte[] data) {
		if(downloadComplete)
			return;
		
		System.arraycopy(data, 0, uploadData, uploadOffset, data.length);
		uploadOffset += data.length;
		
		if(uploadOffset > uploadData.length)
		{
			uploadData = null;
			downloadComplete = true;
			return;
		}
		
	}
	
	@SideOnly(Side.CLIENT)
	public void networkOnDownloadEnd(boolean abort) {
		if(downloadComplete)
			return;
		
		if(abort)
		{
			downloadComplete = true;
			uploadData = null;
			return;
		}
		
		//Not received enough data
		if(uploadOffset != uploadData.length)
		{
			downloadComplete = true;
			ModLog.printTileErrorPrefix(this);
			System.err.println("Failed to download Schematic. Download ended without enough data. Expected: " + uploadData.length + ". Got: " + uploadOffset);
			uploadData = null;
			return;
		}
		
		//Read the Schematic
		try {
			ByteArrayOutputStream decompressedStream = new ByteArrayOutputStream();
			IOUtils.copy(new GZIPInputStream(new ByteArrayInputStream(uploadData)), decompressedStream);
			
			ByteBuf buf = Unpooled.wrappedBuffer(decompressedStream.toByteArray());
			HashMap<Short, MutableInt> blockCount = new HashMap<Short, MutableInt>();
			loadedSchematic = Schematic.fromBytes(buf, blockCount);
			populateResources(blockCount);
			
			//Cache downloaded Schematic
			File cacheFolder = getCacheFolderClient();
			File cacheFile = new File(cacheFolder, cachedSchematicFile);
			try {
				SchematicLoader.saveSchematic(cacheFile, loadedSchematic);
			} catch (Exception e) {
				ModLog.printTileErrorPrefix(this);
				System.err.println("Failed to save cached Schematic file: " + cachedSchematicFile);
				e.printStackTrace();
			}
			
		} catch(Exception e) {
			ModLog.printTileErrorPrefix(this);
			System.err.println("Error while reading downloaded Schematic");
			e.printStackTrace();
		}
		
		downloadComplete = true;
		uploadData = null;
		
	}
	
	//Returns the percentage of download progress
	@SideOnly(Side.CLIENT)
	public String getDownloadProgress() {
		if(uploadData == null)
			return "";
		if(downloadComplete)
			return "100%";
		
		float progress = uploadOffset / (float) uploadData.length;
		NumberFormat format = new DecimalFormat("0.00");
		return format.format(progress*100.0) + "%";
	}
	
	public boolean canBuild() {
		return state == BuilderState.READY || state == BuilderState.STOPPED;
	}
	
	public void actionBuild() {
		if(!canBuild())
			return;
		
		if(state != BuilderState.STOPPED)
		{
			lastX = lastY = lastZ = 0;
			placedCount = 0;
			if(!config.placeFloor)
				currentPass = 0;
			else
				currentPass = -1;
		}
		
		
		buildX = xCoord;
		buildY = yCoord;
		buildZ = zCoord;
		
		if(config.placeFloor && currentPass >= 0)
			buildY++; //Take into account that floor has already been placed

		updateDirection();
		
		state = BuilderState.BUILDING;
		sendNetworkUpdateMessage(null);
	}

	public void actionStop() {
		if(state != BuilderState.BUILDING && state != BuilderState.NEEDRESOURCES)
			return;
		
		state = BuilderState.STOPPED;
		sendNetworkUpdateMessage(null);
	}
	
	//Config update from client
	public void actionConfig(Config config) {
		if(this.config.passCount != config.passCount)
		{
			this.config.passCount = config.passCount;
		}
		if(this.config.placeFloor != config.placeFloor)
		{
			if(state != BuilderState.BUILDING && state != BuilderState.STOPPED)
			{
				this.config.placeFloor = config.placeFloor;
			}
		}
		if(this.config.floorBlock != config.floorBlock)
		{
			if(state != BuilderState.BUILDING && state != BuilderState.STOPPED && state != BuilderState.NEEDRESOURCES)
			{
				this.config.floorBlock = config.floorBlock;
			}
		}
		if(this.config.placeAir != config.placeAir)
		{
			this.config.placeAir = config.placeAir;
		}
		
		//Inform watchers about config change
		sendNetworkUpdateFull(null);
	}
	
	//Client request Schematic download
	public void actionDownload(EntityPlayerMP player) {
		//TODO: Config whether this is enabled or not
		startSendSchematic(player);
	}
	
	//Update the internal values used for Building direction depending on the direction value(Metadata)
	public void updateDirection() {
		if(loadedSchematic == null)
			return;
		//Set the start position according to direction and size
		//0 = South(Positive Z)
		//1 = West (Negative X)
		//2 = North(Negative Z)
		//3 = East (Positive X)
		if(direction == 0)
		{
			buildX = xCoord - (loadedSchematic.getWidth()-1);
		}
		else if(direction == 1)
		{
			buildX = xCoord - loadedSchematic.getWidth();
			buildZ = zCoord - loadedSchematic.getLength();
		}
		else if(direction == 2)
		{
			buildZ = zCoord - (loadedSchematic.getLength() + 1);
		}
		else if(direction == 3)
		{
			buildX = xCoord + 1;
			buildZ = zCoord - 1;
		}
	}
	
	
	public static File getSaveFolderServer() {
		File saveFolder = new File(DimensionManager.getCurrentSaveRootDirectory(), "schematics");
		if(!saveFolder.exists())
			saveFolder.mkdirs();
		return saveFolder;
	}
	
	@SideOnly(Side.CLIENT)
	public static File getSaveFolderClient() {
		File mcHome = (File)FMLInjectionData.data()[6]; //TODO: Find a cleaner way
		File saveFolder = new File(mcHome, "saves/SchematicBuilder/local"); //TODO: Per server local cache
		if(!saveFolder.exists())
			saveFolder.mkdirs();
		return saveFolder;
	}
	
	@SideOnly(Side.CLIENT)
	public static File getLoadFolderClient() {
		File mcHome = (File)FMLInjectionData.data()[6]; //TODO: Find a cleaner way
		File loadFolder = new File(mcHome, "schematics");
		if(!loadFolder.exists())
			loadFolder.mkdirs();
		return loadFolder;
	}
	
	@SideOnly(Side.CLIENT)
	public static File getCacheFolderClient() {
		File mcHome = (File)FMLInjectionData.data()[6]; //TODO: Find a cleaner way
		
		ServerData serverData = Minecraft.getMinecraft().func_147104_D();
		String server;
		if(serverData == null)
			server = "local";
		else
			server = serverData.serverIP;
		
		File cacheFolder = new File(mcHome, "schematicsCache/" + server);
		if(!cacheFolder.exists())
			cacheFolder.mkdirs();
		return cacheFolder;
	}

	public boolean saveLoadedSchematic() {
		if(loadedSchematic == null)
			return false;
		
		String uploadedBy = "unknown";
		if(uploader != null)
			uploadedBy = uploader.getDisplayName();
		
		File saveFolder = getSaveFolderServer();
		File schematicFile;
		try {
			schematicFile = File.createTempFile("upload_" + uploadedBy + "_", loadedSchematic.name + ".schematic", saveFolder);
		} catch (IOException e) {
			ModLog.printTileErrorPrefix(this);
			System.err.println("SchematicBuilder failed to store uploaded Schematic: " + loadedSchematic.name + ", uploaded by: " + uploader);
			e.printStackTrace();
			return false;
		}
		
		//Write the data
		try {
			SchematicLoader.saveSchematic(schematicFile, loadedSchematic);
		} catch(Exception e) {
			ModLog.printTileErrorPrefix(this);
			System.err.println("SchematicBuilder failed to store uploaded Schematic: " + loadedSchematic.name + ", uploaded by: " + uploader);
			e.printStackTrace();
			return false;
		}
		
		cachedSchematicFile = schematicFile.getName();
		
		return true;
	}
	
	public boolean loadSavedSchematic() {
		if(cachedSchematicFile.isEmpty())
			return false;
		
		File saveFolder = getSaveFolderServer();
		File schematicFile = new File(saveFolder, cachedSchematicFile);
		
		if(!schematicFile.exists())
			return false;
		
		try {
			HashMap<Short, MutableInt> blockCount = new HashMap<Short, MutableInt>();
			loadedSchematic = SchematicLoader.loadSchematic(schematicFile, blockCount);
			populateResources(blockCount);
			schematicName = loadedSchematic.name;
			
			state = BuilderState.READY;
		} catch (Exception e) {
			state = BuilderState.ERROR;
			message = "Failed to load cached Schematic";
			ModLog.printTileErrorPrefix(this);
			System.err.println("SchematicBuilder failed to load cached Schematic: " + schematicFile);
			e.printStackTrace();
		}
		
		markDirty();
		sendNetworkUpdateFull(null);
		
		/*try {
			byte[] data = Files.readAllBytes(schematicFile.toPath());
			ByteBuf readBuffer = Unpooled.wrappedBuffer(data);
			loadedSchematic = Schematic.fromBytes(readBuffer);
			schematicName = loadedSchematic.name;
			
			state = BuilderState.READY;
			
			markDirty();
			sendNetworkUpdateFull(null);
		} catch (Exception e) {
			System.err.println("Failed to read Cached Schematic from file: " + schematicFile.getAbsolutePath());
			System.err.println(e.getMessage());
		}*/
		
		return true;
	}
	
	//Creates a Resource list for the currently loaded Schematic
	public void populateResources(HashMap<Short, MutableInt> blockCount) {
		resources.clear();
		if(loadedSchematic == null || blockCount == null)
			return;
		
		for(Entry<Short, MutableInt> entry : blockCount.entrySet())
		{
			int count = entry.getValue().getValue();
			if(count == 0)
				continue;
			
			short blockIndex = entry.getKey();
			short blockId = (short) (blockIndex >> 4);
			byte meta = (byte) (blockIndex & 0x0F);
			
			Block block = Block.getBlockById(blockId);
			if(block == null)
			{
				if(debug)
				{
					ModLog.printTileInfoPrefix(this);
					ModLog.logger.warn("Could not find block for blockId: " + blockId);
				}
				continue;
			}
			
			ResourceItem item = new ResourceItem(block, meta);
			item.blockCount = count;
			//TODO: Load cost from config and determine missing resources
			//Number of items already loaded and used are read from Tile NBT
			resources.put(blockIndex, item);
		}
	}
	
	@Override
	public void onChunkUnload() {
		abortSendSchematic(null);
		super.onChunkUnload();
	}
	
	@Override
	public void invalidate() {
		abortSendSchematic(null);
		super.invalidate();
	}
	
	@Override
    public void readFromNBT(NBTTagCompound nbt)
    {
		super.readFromNBT(nbt);
		
		//Read and handle version differences
		short version = nbt.getShort("tileVersion");
		//TODO: Handle conversion here
		//Inform of incompatible version
		if(version != tileVersion)
		{
			ModLog.printTileErrorPrefix(this);
			System.err.println("Loading incompatible TileEntity version for TileSchematicBuilder. Expected version: " + tileVersion + ", saved: " + version+ ". Data might be lost!");
			//For now we allow it to continue loading, hoping it will go well
		}
		
		//Read state and progress
		BuilderState prevState = state.fromValue(nbt.getInteger("state"));
		schematicName = nbt.getString("schematicName");
		cachedSchematicFile = nbt.getString("cachedSchematicFile");
		
		//Read cached Schematic if needed
		if(prevState != BuilderState.ERROR || !cachedSchematicFile.trim().isEmpty())
			loadSavedSchematic();
		
		lastX = nbt.getInteger("lastX");
		lastY = nbt.getInteger("lastY");
		lastZ = nbt.getInteger("lastZ");
		placedCount = nbt.getInteger("placedCount");
		currentPass = nbt.getInteger("currentPass");
		message = nbt.getString("message");
		
		//Config
		config.passCount = nbt.getInteger("passCount");
		config.placeFloor = nbt.getBoolean("placeFloor");
		String floorBlock = nbt.getString("floorBlock");
		int floorBlockId = blockRegistry.getId(floorBlock);
		if(floorBlockId != -1)
		{
			Block floorBlockTemp = blockRegistry.getObjectById(floorBlockId);
			byte floorBlockMeta = nbt.getByte("floorBlockMeta");
			config.floorBlock = new SchematicBlock(floorBlockTemp, floorBlockMeta);
		}
		else
		{
			ModLog.logger.warn("Unable to get Block id for floor Block: " + floorBlock);
			//TODO: Mark warning on the floor to inform player of error
			config.floorBlock = null;
		}
		config.placeAir = nbt.getBoolean("placeAir");
		
		//Continue Building
		if(prevState == BuilderState.BUILDING)
		{
			//TODO: Config option to force them into STOPPED state until player manually starts it again
			state = BuilderState.STOPPED;
			actionBuild();
		}
		else if(prevState == BuilderState.DOWNLOADING)
		{
			state = BuilderState.ERROR;
			message = "Download incomplete before chunk unload!";
		}
		else
			state = prevState;
		
		//TODO: Read resource list
    }

	@Override
    public void writeToNBT(NBTTagCompound nbt)
    {
		super.writeToNBT(nbt);
		
		//Write state and progress
		nbt.setShort("tileVersion", tileVersion);
		nbt.setInteger("state", state.getValue());
		if(cachedSchematicFile == null)
			cachedSchematicFile = "";
		nbt.setString("schematicName", schematicName);
		nbt.setString("cachedSchematicFile", cachedSchematicFile);
		
		nbt.setInteger("lastX", lastX);
		nbt.setInteger("lastY", lastY);
		nbt.setInteger("lastZ", lastZ);
		nbt.setInteger("placedCount", placedCount);
		nbt.setInteger("currentPass", currentPass);
		nbt.setString("message", message);
		
		//Config
		nbt.setInteger("passCount", config.passCount);
		nbt.setBoolean("placeFloor", config.placeFloor);
		if(config.floorBlock != null)
			nbt.setString("floorBlock", Block.blockRegistry.getNameForObject(config.floorBlock.getBlock()));
		nbt.setByte("floorBlockMeta", config.floorBlock.metaData);
		nbt.setBoolean("placeAir", config.placeAir);
		
		//TODO: Write resource list with resource name, resource needed and resources remaining

    }

	@Override
	public boolean canConnectEnergy(ForgeDirection from) {
		//TODO: Don't connect energy in the direction we are building
		return true;
	}

	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive,
			boolean simulate) {
		return energyStorage.receiveEnergy(maxReceive, simulate);
	}

	@Override
	public int extractEnergy(ForgeDirection from, int maxExtract,
			boolean simulate) {
		return energyStorage.extractEnergy(maxExtract, simulate);
	}

	@Override
	public int getEnergyStored(ForgeDirection from) {
		return energyStorage.getEnergyStored();
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from) {
		return energyStorage.getMaxEnergyStored();
	}
	
}
