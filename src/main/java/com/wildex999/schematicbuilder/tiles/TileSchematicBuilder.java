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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.mutable.MutableInt;

import cpw.mods.fml.common.Optional;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.ResourceEntry;
import com.wildex999.schematicbuilder.ResourceItem;
import com.wildex999.schematicbuilder.ResourceManager;
import com.wildex999.schematicbuilder.SchematicWorldCache;
import com.wildex999.schematicbuilder.WorldSchematicVisualizer;
import com.wildex999.schematicbuilder.config.ConfigurationManager;
import com.wildex999.schematicbuilder.config.IConfigListener;
import com.wildex999.schematicbuilder.config.StorageDirectories;
import com.wildex999.schematicbuilder.exceptions.ExceptionLoad;
import com.wildex999.schematicbuilder.exceptions.ExceptionSave;
import com.wildex999.schematicbuilder.gui.IGuiWatchers;
import com.wildex999.schematicbuilder.inventory.InventorySchematicBuilder;
import com.wildex999.schematicbuilder.gui.GuiSchematicBuilder;
import com.wildex999.schematicbuilder.gui.GuiSchematicBuilder.GUI;
import com.wildex999.schematicbuilder.gui.GuiSchematicBuilderResources;
import com.wildex999.schematicbuilder.network.MessageActionSchematicBuilder;
import com.wildex999.schematicbuilder.network.MessageActionSchematicBuilder.ActionType;
import com.wildex999.schematicbuilder.network.MessageBase;
import com.wildex999.schematicbuilder.network.MessageUpdateProgress;
import com.wildex999.schematicbuilder.network.MessageUpdateSchematicBuilder;
import com.wildex999.schematicbuilder.network.MessageUploadSchematic;
import com.wildex999.schematicbuilder.schematic.Schematic;
import com.wildex999.schematicbuilder.schematic.SchematicBlock;
import com.wildex999.schematicbuilder.schematic.SchematicLoader;
import com.wildex999.schematicbuilder.schematic.SchematicLoaderService;
import com.wildex999.schematicbuilder.schematic.SchematicLoaderService.Result;
import com.wildex999.schematicbuilder.schematic.SchematicMap;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;

@Optional.Interface(iface = "cofh.api.energy.IEnergyHandler", modid = "CoFHAPI|energy")
public class TileSchematicBuilder extends TileEntity implements IGuiWatchers, IConfigListener, cofh.api.energy.IEnergyHandler {

	protected final static String inventoryName = "Schematic Builder";
	
	//Inventory
	protected InventorySchematicBuilder inventory;
	public static int inventorySlotInput = 0; //Items Input
	public static int inventorySlotOutput = 1; //Items Output
	public static final int[] accessibleSlots = new int[] {0, 1};
	
	public BuilderState state;
	public String message;
	
	public boolean isCreative = false;
	
	public static class Config {
		public int passCount = 2;
		public boolean placeFloor;
		public SchematicBlock floorBlock = new SchematicBlock(Blocks.dirt, (byte)0); //Floor to place below Schematic(If not air, moves build up by one)
		public boolean placeAir = true;
		
		public void toBytes(ByteBuf buf) {
			buf.writeInt(passCount);
			buf.writeBoolean(placeFloor);
			buf.writeInt(floorBlock.getSchematicBlockId());
			buf.writeByte(floorBlock.getSchematicMeta());
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
		
	    public void writeToNBT(NBTTagCompound nbt)
	    {
			nbt.setInteger("passCount", passCount);
			nbt.setBoolean("placeFloor", placeFloor);
			if(floorBlock != null)
				nbt.setString("floorBlock", Block.blockRegistry.getNameForObject(Block.blockRegistry.getObjectById(floorBlock.getSchematicBlockId())));
			nbt.setByte("floorBlockMeta", floorBlock.getSchematicMeta());
			nbt.setBoolean("placeAir", placeAir);
	    }
	    
	    public void readFromNBT(NBTTagCompound nbt, TileSchematicBuilder tile)
	    {
			passCount = nbt.getInteger("passCount");
			placeFloor = nbt.getBoolean("placeFloor");
			String floorBlockName = nbt.getString("floorBlock");
			int floorBlockId = tile.blockRegistry.getId(floorBlockName);
			if(floorBlockId != -1)
			{
				Block floorBlockTemp = tile.blockRegistry.getObjectById(floorBlockId);
				byte floorBlockMeta = nbt.getByte("floorBlockMeta");
				floorBlock = new SchematicBlock(floorBlockTemp, floorBlockMeta);
			}
			else
			{
				ModLog.logger.warn("Unable to get Block id for floor Block: " + floorBlock);
				//TODO: Mark warning on the floor to inform player of error
				floorBlock = null;
			}
			placeAir = nbt.getBoolean("placeAir");
	    }
	}
	public Config config = new Config();
	
	//Upload to Client
	public static class UploadJob {
		byte[] data; //Data to send(Compressed)
		int offset; //How much has been sent
	}
	HashMap<EntityPlayerMP, UploadJob> uploadManager;
	HashSet<EntityPlayerMP> progressManager;
	
	//Unloaded state
	public BuilderState unloadedState; //The state before unloading
	public int unloadTimeout = 200; //Ticks without activity before unload
	public int unloadCounter; //Ticks since activity
	
	//Preparing
	public BuilderState preparingState; //The state to go into once done loading Schematic
	
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
	
	private Deque<ItemStack> outputQueue; //Queue of items to go into the output slot
	//If hasPreviousOutput is true, there is output remaining from previous Schematic that has to be cleared before new build can start
	public boolean hasPreviousOutput; //TODO: Set when switching to new schematic
	
	public Schematic loadedSchematic;
	
	public HashMap<Short, ResourceItem> resources; //Resources for the currently loaded Schematic
	public HashMap<Short, List<ResourceItem>> resourcesBackMap; //Mapping from Server block & meta to all Resources using it
	
	@SideOnly(Side.CLIENT)
	public int resourceVersion = 0; //Used by GUI to know when it needs to completely refresh Resources list
	
	public String cachedSchematicFile; //Local file containing the cached Schematic
	private Future<SchematicLoaderService.Result> loadSchematicWork; //Async Schematic loading progress/result
	@SideOnly(Side.CLIENT)
	private boolean uploadAfterLoad; //Whether we are to upload the schematic when it has loaded
	@SideOnly(Side.CLIENT)
	private boolean cacheAfterLoad; //Whether to cache Schematic after async load
	
	public cofh.api.energy.EnergyStorage energyStorage;
	private int maxEnergy;
	private int energyCostNOP; //Energy cost of doing nothing(Skip air, block already exists etc.)
	private int energyCostPlace; //Energy cost of placing a non-air block
	private float energyModifierPass2; //Multiplier for energy cost in the second pass
	
	private int chunkSize = 16;
	
	private SchematicBlock defaultBlock;
	private int direction; //Direction the Builder is facing(Build direction)
	private int chunkX, chunkY, chunkZ; //Cache of current chunk position
	private int lastX, lastY, lastZ; //Current block offset in building
	public int buildX, buildY, buildZ; //Start position for building
	private int placedCount; 
	private boolean initialized; //Metadata init

	
	private int currentPass; //Multiple passes are required for some blocks
	
	private int uploadTimeoutTicks = 200; //How many ticks since last data message before it fails
	private int lastUploadTicks = 0; //Ticks since last data message while receiving upload
	
	private FMLControlledNamespacedRegistry<Block> blockRegistry;
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
		inventory = new InventorySchematicBuilder(inventoryName, true, 2, this);
		state = BuilderState.WAITINGFORSERVER;
		schematicName = "None";
		message = "";
		
		watchers = new HashSet<EntityPlayer>();
		blockRegistry = GameData.getBlockRegistry();
		defaultBlock = new SchematicBlock(Blocks.air, (byte) 0);
		
		if(ModSchematicBuilder.useEnergy)
			energyStorage = new cofh.api.energy.EnergyStorage(maxEnergy);
		
		loadedSchematic = null;
		loadSchematicWork = null;
		unloadCounter = 0;
		
		resources = new HashMap<Short, ResourceItem>();
		resourcesBackMap = new HashMap<Short, List<ResourceItem>>();
		
		resourceUpdates = new ArrayList<ResourceItem>();
		outputQueue = new ArrayDeque<ItemStack>();
		
		uploadManager = new HashMap<EntityPlayerMP, UploadJob>();
		progressManager = new HashSet<EntityPlayerMP>();
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
			if(!worldObj.isRemote && state == BuilderState.WAITINGFORSERVER)
				state = BuilderState.IDLE;
			
			ModSchematicBuilder.configGeneral.addConfigListener(this);
			onConfigReload(ModSchematicBuilder.configGeneral);
			
			direction = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
			buildX = xCoord;
			buildY = yCoord;
			if(config.placeFloor && currentPass >= 0)
				buildY++; //Take into account that floor has already been placed
			buildZ = zCoord;
			if(loadedSchematic != null)
				updateDirection();
			initialized = true;
			markDirty();
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
		else if(state == BuilderState.PREPARING || state == BuilderState.RELOADING)
			serverLoadingUpdate();
		
		//Consume input and push output
		if(serverConsumeInput())
			markDirty();
		if(serverPushOutput())
			markDirty();
		
		//Upload to client
		serverSendSchematicUpdate();
		
		if(++resourceUpdateTimer >= resourceUpdateFreq)
		{
			if(watchers.size() > 0)
				sendNetworkUpdateResources(null);
			resourceUpdateTimer = 0;
		}
		
		//TODO: Unload check
		/*if(canUnload() && unloadCounter++ >= unloadTimeout)
		{
			serverUnload();
		}*/
	}
	
	//Take input from the Input slot and put it into Resources
	//Returns true if somethign was consumed(I.e, TileEntity changed and needs to update)
	private boolean serverConsumeInput() {
		ItemStack item = inventory.getStackInSlot(inventorySlotInput);
		
		if(loadedSchematic == null || item == null || item.getItem() == null)
			return false;
		
		if(item.stackSize == 0)
		{
			inventory.setInventorySlotContents(inventorySlotInput, null);
			return false;
		}

		List<ResourceEntry> entryList = ModSchematicBuilder.resourceManager.getItemMapping(item.getItem());
		if(entryList == null)
			return false;

		for(ResourceEntry entry : entryList) {
			if(!entry.ignoreItemMeta && entry.meta != item.getItemDamage())
				continue; //The item mapping does not take into account meta, but our entry might
			
			List<ResourceItem> resourceList = ResourceManager.getResourcesUsing(resourcesBackMap, entry.blockId, entry.meta);
			if(resourceList == null)
				continue; //No resources using entry
			
			for(ResourceItem resource : resourceList) {
				int required = (int) Math.floor((resource.blockCount - resource.placedCount - resource.storedCount) * resource.getItemCostPerBlock());
				if(required <= 0)
					continue;
				
				if(required >= item.stackSize)
				{
					inventory.setInventorySlotContents(inventorySlotInput, null);
					System.out.println("Consume Stack. Pre: " + resource.storedCount);
					resource.storedCount += Math.ceil(item.stackSize/resource.getItemCostPerBlock());
					System.out.println("Consume Stack. Post: " + resource.storedCount);
					resourceUpdates.add(resource); //Inform clients of change
					
					return true; //We only consume once per tick
				}
				else
				{
					System.out.println("Bite Stack. Pre: stackSize " + item.stackSize + " Stored: " + resource.storedCount);
					resource.storedCount += Math.ceil(required/resource.getItemCostPerBlock());
					item.stackSize -= required;
					System.out.println("Bite Stack. Post: stackSize " + item.stackSize + " Stored: " + resource.storedCount);
					inventory.setInventorySlotContents(inventorySlotInput, item); //Make sure inventory updates
					resourceUpdates.add(resource); //Inform clients of change
					
					return true; //We only consume once per tick
				}
			}
		}
		
		//If we still have an item in input, but no Resource that can take it, move it to output
		outputQueue.push(item);
		inventory.setInventorySlotContents(inventorySlotInput, null);
		
		return true;
	}
	
	//Take item from the output queue and put them into the output slot
	private boolean serverPushOutput() {
		if(inventory.getStackInSlot(inventorySlotOutput) != null || outputQueue.size() == 0)
			return false;
		
		inventory.setInventorySlotContents(inventorySlotOutput, outputQueue.pop());
		
		return true;
	}
	
	//Unload the loaded Schematic and save the current state
	private void serverUnload() {
		unloadedState = state;
		state = BuilderState.UNLOADED;
		loadedSchematic = null;
	}

	//Server is loading Schematic either from Serialized upload or from file
	private void serverLoadingUpdate() {
		if(loadSchematicWork == null)
		{
			serverStateError("Unknown error while loading async Schematic.", true);
			return ;
		}
		
		if(!loadSchematicWork.isDone())
			return;
		
		SchematicLoaderService.Result result;
		try {
			result = loadSchematicWork.get();
		} catch(Exception e) {
			if(uploader != null)
				serverStateError("Error while reading uploaded Schematic", false);
			else
				serverStateError("Error while reading Schematic", false);
			
			ModLog.printTileErrorPrefix(this);
			if(uploader != null)
				System.err.println("Error while reading uploaded Schematic from: " + uploader.getDisplayName());
			else
				System.err.println("Error while reading Schematic from file: " + cachedSchematicFile);
			e.printStackTrace();
			
			uploader = null;
			cachedSchematicFile = "";
			sendNetworkUpdateFull(null);
			sendNetworkResourcesList(null);
			return;
		}
		loadSchematicWork = null;
		
		if(result.schematic == null)
		{
			serverStateError(result.message, true);
			markDirty();
			uploader = null;
			return;
		}

		loadedSchematic = result.schematic;
		if(state == BuilderState.RELOADING)
		{ //Reloading from unloaded state
			if(ModSchematicBuilder.debug)
				ModLog.logger.info("Reloading from unloaded state: " + (loadedSchematic != null ? loadedSchematic.name : "None") + ". State: " + state + " -> " + unloadedState);
			state = unloadedState;
		}
		else if(preparingState != BuilderState.IDLE)
		{ //Reloading from saved state
			if(ModSchematicBuilder.debug)
				ModLog.logger.info("Reloading from saved state: " + (loadedSchematic != null ? loadedSchematic.name : "None") + ". State: " + state + " -> " + preparingState);
			state = preparingState;
			
			//Continue Building
			if(state == BuilderState.BUILDING)
			{
				//TODO: Config option to force them into STOPPED state until player manually starts it again
				state = BuilderState.STOPPED;
				actionBuild();
			}
		}
		else
		{
			if(ModSchematicBuilder.debug)
				ModLog.logger.info("Loaded Schematic: " + loadedSchematic.name + ". Populating resources.");
			
			if(!populateResources(result.blockCount)) {
				cachedSchematicFile = "";
				schematicName = "";
				uploader = null;
				loadedSchematic = null;
				serverStateError("Failed to create resources list for Schematic! Schematic might be invalid.", true);
				markDirty();
				
				return;
			}
			
			//Cache uploaded schematic
			if(!saveLoadedSchematic())
				cachedSchematicFile = "";
			else if(ModSchematicBuilder.debug)
				ModLog.logger.info("Server cached Schematic: " + cachedSchematicFile);
			
			//Don't allow build to start until output queue has been cleared
			if(outputQueue.size() > 0)
				hasPreviousOutput = true;
				
			state = BuilderState.READY;
		}
			
		
		schematicName = loadedSchematic.name;
		message = "";
		uploader = null;
		
		markDirty();
		sendNetworkUpdateFull(null);
		sendNetworkResourcesList(null);
	}
	
	public void onPlaceCached(String cachedFile, String schematicName) {
		if(worldObj.isRemote)
			return;
			
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
		
		if(sendData.length > ModSchematicBuilder.configGeneral.maxSendSizeBytes)
			return;
		
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
			serverStateError("No Schematic loaded, incompatible state.", true);
			return;
		}
		
		int width = loadedSchematic.getWidth();
		int height = loadedSchematic.getHeight();
		int length = loadedSchematic.getLength();
		int airRepeat = ModSchematicBuilder.configGeneral.nopRepeat;
		if(currentPass == 1)
			airRepeat = ModSchematicBuilder.configGeneral.nopRepeatPassTwo;
		SchematicBlock block = null;
		Block newBlock = null;
		byte meta = 0;
		
		int prevPlacedCount = placedCount;
		boolean needEnergy = false;
		boolean needMaterial = false;
		String missingBlocks = null;
		boolean wasAir; //Check if any block was placed(Was Air if none placed)
		
		do {
			int x = buildX + lastX;
			int y = buildY + lastY;
			int z = buildZ + lastZ + 1;
			wasAir = true;

			if(lastY >= 0)
				block = loadedSchematic.getBlock(lastX, lastY, lastZ);
			else
				block = config.floorBlock;
			
			if(block == null)
			{
				if(ModSchematicBuilder.debug)
				{
					ModLog.printTileErrorPrefix(this);
					ModLog.logger.error("No block for Schematic at X: " + lastX + " Y: " + lastY + " Z: " + lastZ);
				}
				continue; //For now we just skip if we have nothing to place
			}
			//TODO: Allow setting block to "Nothing" block which does nothing(Not even place air)
			
			//Check if we have the resources to place
			ResourceItem resource = null;
			if(block != config.floorBlock)
			{
				resource = ResourceManager.getResource(this.resources, block.getSchematicBlockId(), block.getSchematicMeta());
				if(resource == null)
				{
					serverStateError("Failed to get resource for Schematic block: " + block.getOriginalName(loadedSchematic), true);
					return;
				}
				
				newBlock = resource.getBlock();
				meta = resource.getMeta();
			}
			else
			{
				newBlock = Block.getBlockById(block.getSchematicBlockId()); //Floor block
				meta = block.getSchematicMeta();
			}
			
			
			switch(currentPass) {
			case -1: //Pass 0: Floor
			case 0: //Pass 1: Initial placement
				
				//Place the block	
				if(newBlock != Blocks.air  || config.placeAir)
				{
					if(ModSchematicBuilder.useEnergy) { //Energy
						if(energyStorage.getEnergyStored() < energyCostPlace)
						{
							needEnergy = true;
							break;
						}
						energyStorage.modifyEnergyStored(-energyCostPlace);
					}
					
					Block oldBlock = worldObj.getBlock(x, y, z);
					if(oldBlock != newBlock || meta != worldObj.getBlockMetadata(x, y, z))
					{
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
							{
								if(resource != null) {
									if(!this.isCreative && resource.storedCount <= 0)
									{
										needMaterial = true;
										missingBlocks = Block.blockRegistry.getNameForObject(resource.getBlock()) + "(" + resource.getMeta() + ")";
										break;
									}
									resource.storedCount--;
								}
								
								worldObj.setBlock(x, y, z, newBlock, meta, 2);
								
								if(resource != null) {
									resource.placedCount++;
									resourceUpdates.add(resource);
								}
							}
						}
						wasAir = false;
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
					{
						//Check if we have the material to place the block
						if(resource != null) {
							if(!this.isCreative && resource.storedCount <= 0)
							{
								needMaterial = true;
								missingBlocks = Block.blockRegistry.getNameForObject(resource.getBlock()) + "(" + resource.getMeta() + ")";
								break;
							}
							resource.storedCount--;
							resource.placedCount++;
							resourceUpdates.add(resource);
						}
						
						worldObj.setBlock(x, y, z, newBlock, meta, 2);
						wasAir = false;
					}
					
					int placedMeta = worldObj.getBlockMetadata(x, y, z);
					if(placedMeta != meta) //Reinforce the metadata, as some blocks change it when placed
					{
						worldObj.setBlockMetadataWithNotify(x, y, z, meta, 0);
						worldObj.markBlockForUpdate(x, y, z); //Using the flags for notify will cause neighboring blocks to drop/change
						wasAir = false;
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
			else if(needMaterial)
			{
				stateMissingMaterial(true, missingBlocks);
				break;
			}

			
			//Work one chunk at a time
			lastX++;
			if(lastX >= width || lastX >= (chunkX+1)*chunkSize)
			{
				lastX = chunkX*chunkSize;
				lastZ++;
			}
			if(lastZ >= length || lastZ >= (chunkZ+1)*chunkSize)
			{
				lastZ = chunkZ*chunkSize;
				lastY++;
			}
			if(lastY >= height || lastY >= (chunkY+1)*chunkSize)
			{
				//End of current chunk
				chunkX++;
				if(chunkX >= Math.ceil((float)width/chunkSize))
				{
					chunkX = 0;
					chunkZ++;
				}
				lastX = chunkX*chunkSize;
				if(chunkZ >= Math.ceil((float)length/chunkSize))
				{
					chunkZ = 0;
					chunkY++;
					
					if(chunkY >= Math.ceil((float)height/chunkSize)) {
						//End of pass
						lastY = 0;
						if(++currentPass >= config.passCount)
						{
							//TODO: Do Finalizing checks etc.
							state = BuilderState.DONE;
							loadedSchematic = null; //Clear loaded schematic data
							cachedSchematicFile = "";
							markDirty();
							sendNetworkUpdateFull(null);
							stopProgressUpdate();
							return;
						}
						else
						{
							lastX = lastY = lastZ = 0;
							chunkX = chunkY = chunkZ = 0;
						}
						break; //End of pass we always stop the build tick
					}
				}
				lastZ = chunkZ*chunkSize;
				lastY = chunkY*chunkSize;
				if(config.placeFloor && lastY == 0)
					lastY = -1;
			}
			
			placedCount++;
			System.out.println("PLACED: " + placedCount);
			if(state != BuilderState.BUILDING)
				stateContinue(false);
		} while(airRepeat-- > 0 && wasAir);
		
		if(state == BuilderState.NEEDRESOURCES && prevPlacedCount != placedCount)
		{
			markDirty();
			return;
		}
		else if(state == BuilderState.NEEDRESOURCES)
			return;

		//Send progress update
		serverSendProgressUpdate(buildX+lastX, buildY+lastY, buildZ+lastZ);
		int count = width*height*length;
		count *= config.passCount;
		
		float percentage = placedCount / (float)count;
		NumberFormat format = new DecimalFormat("0.00");
		
		if(currentPass >= 0)
			message = format.format(percentage*100.0) + "%(Pass " + (currentPass+1) + ")";
		else
			message = "Placing Floor";
		
		markDirty();
		sendNetworkUpdateMessage(null);
		
	}
	
	public void stateMissingEnergy(boolean send) {
		state = BuilderState.NEEDRESOURCES;
		message = "Energy";
		if(send)
			sendNetworkUpdateMessage(null);
	}
	
	public void stateMissingMaterial(boolean send, String blockName) {
		state = BuilderState.NEEDRESOURCES;
		message = "Blocks: " + blockName;
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
			serverStateError("Upload timed out!", true);
			cleanUpload();
		}
	}
	
	private void cleanUpload() {
		inUpload = false;
		uploadData = null;
	}
	
	@SideOnly(Side.CLIENT)
	public void clientUpdate() {
		if(schematicCache != null && schematicCache.getSchematic() != loadedSchematic)
			schematicCache = null;
		
		if(state == BuilderState.CHOOSINGLOCAL)
			updateSchematicChoosing();
		
		if(state == BuilderState.UPLOADING)
			updateUploadSchematic();
		
		if(loadSchematicWork != null)
		{
			if(loadSchematicWork.isDone())
			{
				
				Result result;
				try {
					result = loadSchematicWork.get();
					
					if(result.schematic != null)
					{
						if(ModSchematicBuilder.debug)
							ModLog.logger.info("Loaded Schematic: " + result.schematic.name);
						
						loadedSchematic = result.schematic;
						
						if(uploadAfterLoad)
						{ //Upload the loaded Schematic to the server
							
							//Give it a name if hasn't got one
							if(loadedSchematic.name.trim().isEmpty())
							{
								int lastIndex = loadFileLocal.getName().lastIndexOf('.');
								if(lastIndex == -1)
									loadedSchematic.name = loadFileLocal.getName();
								else
									loadedSchematic.name = loadFileLocal.getName().substring(0, lastIndex);
							}
							loadFileLocal = null;
							
							//Send Schematic to server
							uploadSchematicToServer();
						}
						
						if(cacheAfterLoad)
						{
							//Cache downloaded Schematic
							File cacheFolder = StorageDirectories.getCacheFolderClient();
							File cacheFile = new File(cacheFolder, cachedSchematicFile);
							try {
								SchematicLoader.saveSchematic(cacheFile, loadedSchematic);
							} catch (Exception e) {
								ModLog.printTileErrorPrefix(this);
								System.err.println("Failed to save cached Schematic file: " + cachedSchematicFile);
								e.printStackTrace();
							}
						}
						
						//Skip populating Resources on client for now
						/*if(populateResources(result.blockCount)) 
						{
							
						}
						else
						{
							ModLog.printTileErrorPrefix(this);
							String error = "Failed to load resources for Schematic: " + loadedSchematic.name;
							System.err.println(error);
							loadedSchematic = null;
							clientStateError(error);
						}*/
					}
					else
					{
						ModLog.printTileErrorPrefix(this);
						String error = "Failed to load async schematic file: " + cachedSchematicFile + " Error: " + result.message;
						System.err.println(error);
						clientStateError(error);
					}
				} catch (Exception e) {
					ModLog.printTileErrorPrefix(this);
					String error = "Failed to load Schematic file: " + cachedSchematicFile;
					System.err.println(error);
					clientStateError(error);
					e.printStackTrace();
				} 
	
				loadSchematicWork = null;
			}
		}
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

		uploadAfterLoad = true;
		cacheAfterLoad = false;
		
		//TODO: Populate resources from Server(As they might have been swapped)
		HashMap<Short, MutableInt> blockCount = new HashMap<Short, MutableInt>();
		loadSchematicWork = SchematicLoaderService.instance.loadFile(loadFileLocal, blockCount);
		if(loadSchematicWork == null)
			clientStateError("Failed to start async loading of choosen Schematic.");
		else
			state = BuilderState.PREPARING;
		
		
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
		sendNetworkResourcesList((EntityPlayerMP)player);
	}

	@Override
	public void removeWatcher(EntityPlayer player) {
		if(worldObj.isRemote)
			return;
		
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
		
		MessageBase netMessage = new MessageUpdateSchematicBuilder(this, energyCurrent, energyMax, resourceUpdates);
		
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
	
	//Send full list of Resources for the currently loaded Schematic
	public void sendNetworkResourcesList(EntityPlayerMP player) {
		ArrayList<ResourceItem> resourceList = new ArrayList<ResourceItem>(this.resources.values());
		MessageBase netMessage = new MessageUpdateSchematicBuilder(this, resourceList);
		
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
	public void networkUpdateFull(BuilderState newState, String schematicName, String schematicAuthor, String message, String schematicId, 
									int schematicWidth, int schematicHeight, int schematicLength, Config config, boolean hasPreviousOutput) {
		this.message = message;
		
		if(cachedSchematicFile == null || !cachedSchematicFile.equals(schematicId))
		{
			//Switched to new Schematic
			//Delete old cache
			if(cachedSchematicFile != null)
			{
				File cacheFolder = StorageDirectories.getCacheFolderClient();
				File cachedFile = new File(cacheFolder, cachedSchematicFile);
				if(cachedFile.exists())
					cachedFile.delete();
			}
			
			if(inUpload && uploadApproved)
			{
				if(schematicId.length() > 0)
				{
					//Write our Upload to cache
					File cacheFolder = StorageDirectories.getCacheFolderClient();
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
				if(loadSchematicWork != null)
				{
					//Abort any currently loading Schematic before starting new task
					loadSchematicWork.cancel(true);
					loadSchematicWork = null; 
				}
				
				if(schematicId.length() > 0)
				{
					//Check if we have this schematic cached
					File cacheFolder = StorageDirectories.getCacheFolderClient();
					File cacheFile = new File(cacheFolder, schematicId);
					if(cacheFile.exists())
					{
						if(ModSchematicBuilder.debug)
							ModLog.logger.info("Loading locally cached Schematic: " + cacheFile.getAbsolutePath());
						
						uploadAfterLoad = false; //We are loading to display localy
						cacheAfterLoad = false;
						HashMap<Short, MutableInt> blockCount = new HashMap<Short, MutableInt>();
						loadSchematicWork = SchematicLoaderService.instance.loadFile(cacheFile, blockCount);
					}
					else
					{
						//Request Schematic download
						if(ModSchematicBuilder.configGeneral.acceptSendSchematic)
						{
							MessageBase netMessage = new MessageActionSchematicBuilder(this, ActionType.DOWNLOAD);
							netMessage.sendToServer();
						}
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
		this.hasPreviousOutput = hasPreviousOutput;
		
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

		//Resources
		for(ResourceItem item : resources) {
			ResourceItem resource = ResourceManager.getResource(this.resources, item.getSchematicBlockId(), item.getSchematicMeta());	
			ResourceManager.setResource(this.resources, this.resourcesBackMap, item); //Will replace old item
		}
		
		updateGui();
	}
	
	@SideOnly(Side.CLIENT)
	public void networkUpdateResourceList(ArrayList<ResourceItem> resources) {
		this.clearResources();
		resourceVersion++;
		
		if(ModSchematicBuilder.debug)
		{
			ModLog.printTileInfoPrefix(this);
			ModLog.logger.info("Received full Resource list from server, " + resources.size() + " entries.");
		}
		
		for(ResourceItem item : resources) {
			ResourceManager.setResource(this.resources, this.resourcesBackMap, item);
		}
		
		this.updateGui();
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
		return canAcceptUpload();
	}
	
	public boolean canAcceptUpload() {
		return state == BuilderState.IDLE || state == BuilderState.ERROR || state == BuilderState.READY
				|| state == BuilderState.DONE || state == BuilderState.DONEMISSING || state == BuilderState.STOPPED
				|| state == BuilderState.UNLOADED;
	}
	public boolean canUnload() {
		//TODO: Read from Config whether to allow unload
		return loadedSchematic != null && (state == BuilderState.READY || state == BuilderState.STOPPED || state == BuilderState.ERROR
				|| state == BuilderState.DONE || state == BuilderState.IDLE);
	}

	
	public void networkOnUploadStart(int size, EntityPlayerMP uploader) {
		//Only allow Uploads if we aren't doing anything else
		if(!canAcceptUpload())
		{
			if(ModSchematicBuilder.debug)
			{
				ModLog.printTileInfoPrefix(this);
				System.out.println("Start upload failed due to not accepted from player: " + uploader.getDisplayName());
			}
			sendNetworkUpdateFull(uploader);
			return;
		}
		
		if(size < 0 || size > ModSchematicBuilder.configGeneral.maxUploadSizeBytes)
		{
			if(ModSchematicBuilder.debug)
			{
				ModLog.printTileInfoPrefix(this);
				System.out.println("Start upload failed due to file size(" + size + " bytes) from player: " + uploader.getDisplayName());
			}
			serverStateError("Upload failed due to illegal size: " + size + " bytes. Server max: " + ModSchematicBuilder.configGeneral.maxUploadSizeBytes + " bytes.", true);
			return;
		}
		if(ModSchematicBuilder.debug)
		{
			ModLog.printTileInfoPrefix(this);
			System.out.println("Start upload(" + size + " bytes) from player: " + uploader.getDisplayName());
		}
		
		//Stop sending of previous Schematic
		abortSendSchematic(null);
		
		//Clear any existing Schematic
		loadedSchematic = null;
		
		//Cleanup old cached schematic
		if(!cachedSchematicFile.isEmpty())
		{
			File saveFolder = StorageDirectories.getSaveFolderServer();
			File cachedFile = new File(saveFolder, cachedSchematicFile);
			if(cachedFile.exists())
			{
				try {
					Files.delete(cachedFile.toPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
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
			serverStateError("Recevied too much data during upload.", true);
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
			uploader = null;
			return;
		}
		
		//Not received enough data
		if(uploadOffset != uploadData.length)
		{
			serverStateError("Did not receive full Schematic upload. Expected: " + uploadData.length + " bytes, got: " + uploadOffset, true);
			cleanUpload();
			markDirty();
			uploader = null;
			return;
		}
		
		//Received all data
		state = BuilderState.PREPARING;
		preparingState = BuilderState.IDLE; 
		message = "Parsing";
		markDirty();
		sendNetworkUpdateFull(null);
		
		//Send to loader service
		HashMap<Short, MutableInt> blockCount = new HashMap<Short, MutableInt>();
		loadSchematicWork = SchematicLoaderService.instance.loadSerialized(uploadData, blockCount);
		
		if(loadSchematicWork == null)
			serverStateError("Failed to schedule serialized async schematic load.", true);
		
		cleanUpload();
	}
	
	//Set state to error with the given message, and update all watching players
	private void serverStateError(String error, boolean update) {
		state = BuilderState.ERROR;
		message = error;
		if(update)
			sendNetworkUpdateFull(null);
	}
	
	@SideOnly(Side.CLIENT)
	public void clientStateError(String error) {
		state = BuilderState.ERROR;
		message = error;
	}
	
	@SideOnly(Side.CLIENT)
	public void networkOnDownloadStart(int size) {
		if(size > ModSchematicBuilder.configGeneral.maxSendSizeBytes || size <= 0)
			return;
		
		downloadComplete = false;
		uploadOffset = 0;
		uploadData = new byte[size];
	}
	
	@SideOnly(Side.CLIENT)
	public void networkOnDownloadData(byte[] data) {
		if(downloadComplete || uploadData == null)
			return;
		
		System.arraycopy(data, 0, uploadData, uploadOffset, data.length);
		uploadOffset += data.length;
		
		if(uploadOffset > uploadData.length)
		{
			uploadData = null;
			downloadComplete = false;
			cachedSchematicFile = "";
			return;
		}
		
	}
	
	@SideOnly(Side.CLIENT)
	public void networkOnDownloadEnd(boolean abort) {
		if(downloadComplete || uploadData == null)
			return;
		
		if(abort)
		{
			downloadComplete = false;
			uploadData = null;
			cachedSchematicFile = "";
			return;
		}
		
		//Not received enough data
		if(uploadOffset != uploadData.length)
		{
			downloadComplete = false;
			ModLog.printTileErrorPrefix(this);
			String error = "Failed to download Schematic. Download ended without enough data. Expected: " + uploadData.length + ". Got: " + uploadOffset;
			System.err.println(error);
			clientStateError(error);
			uploadData = null;
			cachedSchematicFile = "";
			return;
		}
		
		//Read the Schematic
		uploadAfterLoad = false;
		cacheAfterLoad = true;
		
		if(ModSchematicBuilder.debug)
			ModLog.logger.info("Parsing downloaded Schematic.");
		
		HashMap<Short, MutableInt> blockCount = new HashMap<Short, MutableInt>();
		loadSchematicWork = SchematicLoaderService.instance.loadSerialized(uploadData, blockCount);
		if(loadSchematicWork == null)
		{
			String error = "Failed to start async load of downloaded Schematic: " + schematicName;
			System.err.println(error);
			clientStateError(error);
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
		if(hasPreviousOutput)
			return false;
		return state == BuilderState.READY || state == BuilderState.STOPPED;
	}
	
	public void actionBuild() {
		if(!canBuild())
			return;
		
		if(state != BuilderState.STOPPED)
		{
			lastX = lastY = lastZ = 0;
			if(config.placeFloor)
				lastY = -1;
			placedCount = 0;
			currentPass = 0;
		}
		
		//Calculate current chunk from last build position
		chunkX = lastX/chunkSize;
		chunkY = lastY/chunkSize;
		chunkZ = lastZ/chunkSize;
		
		buildX = xCoord;
		buildY = yCoord;
		buildZ = zCoord;
		
		if(config.placeFloor)
			buildY++; //Take into account that floor has already been placed

		updateDirection();
		
		state = BuilderState.BUILDING;
		sendNetworkUpdateMessage(null);
	}

	public void actionStop() {
		if(state != BuilderState.BUILDING && state != BuilderState.NEEDRESOURCES)
			return;
		
		
		state = BuilderState.STOPPED;
		stopProgressUpdate();
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
			if(state != BuilderState.BUILDING && state != BuilderState.STOPPED && state != BuilderState.PREPARING)
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
	
	//Called when player request add/remove from progress updates
	public void actionProgress(EntityPlayerMP player) {
		if(progressManager.contains(player))
		{
			progressManager.remove(player);
			MessageBase msg = new MessageUpdateProgress(this);
			msg.sendToPlayer(player);
		}
		else
		{
			progressManager.add(player);
			//Send initial progress update in case we are in a stopped state
			MessageBase msg = new MessageUpdateProgress(this, buildX+lastX, buildY+lastY, buildZ+lastZ);
			msg.sendToPlayer(player);
		}
	}
	
	//Reload after Unload
	public void actionReload() {
		if(state != BuilderState.UNLOADED)
			return;
		//Reload the Schematic if we can
		
	}
	
	//Client request Schematic download
	public void actionDownload(EntityPlayerMP player) {
		if(ModSchematicBuilder.configGeneral.acceptSendSchematic)
			startSendSchematic(player);
	}
	
	//Send progress update to all who have requested it
	//Will remove any players who are not close enough/offline/another world
	public void serverSendProgressUpdate(int targetX, int targetY, int targetZ) {
		if(loadedSchematic == null)
		{
			stopProgressUpdate();
			return;
		}
		
		int centerX = buildX + loadedSchematic.getWidth()/2;
		int centerZ = buildZ + loadedSchematic.getLength()/2;
		MessageBase msg = new MessageUpdateProgress(this, targetX, targetY, targetZ);
		
		Iterator<EntityPlayerMP> it = progressManager.iterator();
		while(it.hasNext())
		{
			EntityPlayerMP player = it.next();
			
			//Check if player is still online(Handle players who have crashed/disconnected)
			List<EntityPlayerMP> players = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
			if(!players.contains(player))
			{
				it.remove();
				continue;
			}
			
			//Remove players who are too far away
			boolean removePlayer = false;
			if(player.worldObj != this.worldObj)
				removePlayer = true;
			else
			{
				int dx = (int) (centerX - player.posX);
				int dz = (int) (centerZ - player.posZ);

				if(dx*dx > loadedSchematic.getWidth()*loadedSchematic.getWidth() || dz*dz > loadedSchematic.getLength()*loadedSchematic.getLength())
					removePlayer = true;
			}
			if(removePlayer)
			{
				MessageBase errmsg = new MessageUpdateProgress(this);
				errmsg.sendToPlayer(player);
				it.remove();
				continue;
			}
			
			//Send update to player
			msg.sendToPlayer(player);
		}
	}
	
	//Stop progress update for all players
	public void stopProgressUpdate() {
		if(progressManager.size() > 0)
		{
			MessageBase msg = new MessageUpdateProgress(this);
			for(EntityPlayerMP player : progressManager)
				msg.sendToPlayer(player);
			progressManager.clear();
		}
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

	public boolean saveLoadedSchematic() {
		if(loadedSchematic == null)
			return false;
		
		String uploadedBy = "unknown";
		if(uploader != null)
			uploadedBy = uploader.getDisplayName();
		
		File saveFolder = StorageDirectories.getSaveFolderServer();
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
		
		File saveFolder = StorageDirectories.getSaveFolderServer();
		File schematicFile = new File(saveFolder, cachedSchematicFile);
		
		if(!schematicFile.exists())
			return false;
		
		if(ModSchematicBuilder.debug)
			ModLog.logger.info("Loading cached Schematic: " + schematicFile);
		
		HashMap<Short, MutableInt> blockCount = new HashMap<Short, MutableInt>();
		loadSchematicWork = SchematicLoaderService.instance.loadFile(schematicFile, blockCount);
		if(loadSchematicWork != null)
		{
			preparingState = state;
			state = BuilderState.PREPARING;
			return true;
		}
		else
		{
			serverStateError("Error while starting async loading of Cached Schematic: " + schematicFile, true);
			return false;
		}
	}
	
	//Creates a Resource list for the currently loaded Schematic
	//Return false on error
	public boolean populateResources(HashMap<Short, MutableInt> blockCount) {
		resources.clear();
		resourcesBackMap.clear();
		
		if(loadedSchematic == null || blockCount == null)
			return false;
		
		for(Entry<Short, MutableInt> entry : blockCount.entrySet())
		{
			int count = entry.getValue().getValue();
			if(count == 0)
				continue;
			
			short blockIndex = entry.getKey();
			short blockId = (short) ((blockIndex & 0xFFFF) >>> 4); //Make sure we zero fill a short on the left side
			byte meta = (byte) (blockIndex & 0x0F);
			
			//Get the server block
			SchematicMap mapping = loadedSchematic.getSchematicMap(blockId, meta, true);
			ResourceEntry resourceEntry;
			
			if(mapping == null)
			{
				ModLog.printTileErrorPrefix(this);
				ModLog.logger.warn("Assert error: No mapping for Block ID: " + blockId + " and meta: " + meta + " index: " + blockIndex);
				return false;
			}
			
			
			if(mapping.blockId >= 0)
			{
				Block block = blockRegistry.getRaw(mapping.blockId);
				if(block == null)
				{
					if(ModSchematicBuilder.debug)
					{
						ModLog.printTileInfoPrefix(this);
						ModLog.logger.warn("Could not find block for blockId: " + blockId);
					}
					continue;
				}
				
				resourceEntry = ModSchematicBuilder.resourceManager.getOrCreate(Block.blockRegistry.getNameForObject(block), meta);
			}
			else
			{
				resourceEntry = ResourceEntry.Unknown;
			}
			
			ResourceItem item = new ResourceItem(blockId, meta, resourceEntry);
			item.blockCount = count;
			item.placedCount = 0;
			if(mapping.blockId != 0)
				item.storedCount = 0;
			else
				item.storedCount = count; //We always have enough air
			ResourceManager.setResource(resources,resourcesBackMap, item);
		}
		
		return true;
	}
	
	public void clearResources() {
		resources.clear();
		resourcesBackMap.clear();
	}
	
	//Returns true if there are at least one Resource still missing this item
	public boolean canAcceptItem(ItemStack item) {
		if(loadedSchematic == null || item == null || item.getItem() == null)
			return false;

		List<ResourceEntry> entryList = ModSchematicBuilder.resourceManager.getItemMapping(item.getItem());
		if(entryList == null)
			return false;

		for(ResourceEntry entry : entryList) {
			if(!entry.ignoreItemMeta && entry.meta != item.getItemDamage())
				continue; //The item mapping does not take into account meta, but our entry might
			
			List<ResourceItem> resourceList = ResourceManager.getResourcesUsing(resourcesBackMap, entry.blockId, entry.meta);
			if(resourceList == null)
				continue; //No resources using entry
			
			for(ResourceItem resource : resourceList) {
				if((resource.blockCount - resource.placedCount - resource.storedCount) > 0)
					return true;
			}
		}
		
		return false;
	}
	
	@Override
	public void onChunkUnload() {
		if(worldObj.isRemote)
			return;
		
		abortSendSchematic(null);
		stopProgressUpdate();
		super.onChunkUnload();
	}
	
	@Override
	public void invalidate() {
		ModSchematicBuilder.configGeneral.removeConfigListener(this);
		
		if(worldObj.isRemote)
			return;
		
		abortSendSchematic(null);
		stopProgressUpdate();
		super.invalidate();
	}
	
	public void onBreak() {
		if(state == BuilderState.BUILDING)
			state = BuilderState.STOPPED;
		
		if(worldObj.isRemote)
		{
			WorldSchematicVisualizer visualizer = WorldSchematicVisualizer.instance;
			if(visualizer.vTile == this)
				visualizer.vTile = null;
			if(visualizer.fTile == this)
				visualizer.fTile = null;
		}
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
		if(prevState != BuilderState.ERROR && prevState != BuilderState.UNLOADED && !cachedSchematicFile.trim().isEmpty())
			loadSavedSchematic();
		
		lastX = nbt.getInteger("lastX");
		lastY = nbt.getInteger("lastY");
		lastZ = nbt.getInteger("lastZ");
		placedCount = nbt.getInteger("placedCount");
		currentPass = nbt.getInteger("currentPass");
		message = nbt.getString("message");
		
		//Config
		config.readFromNBT(nbt, this);
		

		if(prevState == BuilderState.DOWNLOADING)
		{
			state = BuilderState.ERROR;
			message = "Download incomplete before chunk unload!";
		}
		else if(prevState == BuilderState.UNLOADED)
		{
			state = BuilderState.UNLOADED;
			unloadedState = state.fromValue(nbt.getInteger("unloadedState"));
		}
		else
		{
			if(state == BuilderState.PREPARING)
				preparingState = prevState;
			else
				state = prevState;
		}
		
		//Inventory
		inventory.readFromNBT(nbt);
		
		//Read resources
		NBTTagList nbtResources = nbt.getTagList("resourceList", Constants.NBT.TAG_COMPOUND);
		for(int t = 0; t < nbtResources.tagCount(); t++)
		{
			NBTTagCompound tag = nbtResources.getCompoundTagAt(t);
			
			String blockName = tag.getString("blockName");
			if(blockName.isEmpty())
				continue;
			
			byte meta = tag.getByte("meta");
			short schematicBlockId = tag.getShort("schematicBlockId");
			byte schematicMeta = tag.getByte("schematicMeta");
			
			ResourceEntry resourceEntry = ModSchematicBuilder.resourceManager.getOrCreate(blockName, meta);
			
			ResourceItem item = new ResourceItem(schematicBlockId, schematicMeta, resourceEntry);
			item.blockCount = tag.getInteger("count");
			item.placedCount = tag.getInteger("placed");
			item.storedCount = tag.getInteger("stored");
			
			//Verify that all values are sane(Placed < count etc.)
			if(item.placedCount + item.storedCount > item.blockCount)
			{
				if(ModSchematicBuilder.debug)
				{
					ModLog.printTileInfoPrefix(this);
					ModLog.logger.warn("Values of placed and stored blocks are higher than total for schematic while loading state.");
					ModLog.logger.warn("Expected: " + item.blockCount + " Got: " + (item.placedCount + item.storedCount));
				}
			}
			
			ResourceManager.setResource(resources, resourcesBackMap, item);
		}
		
		//Read output queue
		NBTTagList itemsNBT = nbt.getTagList("outputQueue", 10); //NBT_TYPE_COMPOUND
		for (int i = 0; i < itemsNBT.tagCount(); ++i) {
			NBTTagCompound itemData = itemsNBT.getCompoundTagAt(i);
			ItemStack item = ItemStack.loadItemStackFromNBT(itemData);
			if(item != null)
				outputQueue.add(item);
			else
			{
				ModLog.printTileErrorPrefix(this);
				ModLog.logger.error("Failed to load valid Item for Output Queue! TileEntity might be corrupt, or server items/blocks changed.");
			}
		}
		hasPreviousOutput = nbt.getBoolean("hasPreviousOutput");
		
    }

	@Override
    public void writeToNBT(NBTTagCompound nbt)
    {
		super.writeToNBT(nbt);
		
		//Write state and progress
		nbt.setShort("tileVersion", tileVersion);
		nbt.setInteger("state", state.getValue());
		if(state == BuilderState.UNLOADED)
			nbt.setInteger("unloadedState", unloadedState.getValue());
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
		
		config.writeToNBT(nbt);
		inventory.writeToNBT(nbt);
		
		//Write Resources
		NBTTagList nbtResources = new NBTTagList();
		for(ResourceItem item : resources.values())
		{
			if(item == null)
				continue;
			NBTTagCompound tag = new NBTTagCompound();
			
			tag.setShort("schematicBlockId", item.getSchematicBlockId());
			tag.setByte("schematicMeta", item.getSchematicMeta());
			tag.setString("blockName", blockRegistry.getNameForObject(item.getBlock()));
			tag.setByte("meta", item.getMeta());
			
			tag.setInteger("placed", item.placedCount);
			tag.setInteger("count", item.blockCount);
			tag.setInteger("stored", item.storedCount);
			
			nbtResources.appendTag(tag);
		}
		nbt.setTag("resourceList", nbtResources);
		
		//Write output queue
		NBTTagList itemsNBT = new NBTTagList();
		for(ItemStack item : outputQueue) {
            NBTTagCompound itemData = new NBTTagCompound();
            item.writeToNBT(itemData);
            itemsNBT.appendTag(itemData);
		}
		nbt.setTag("outputQueue", itemsNBT);
		nbt.setBoolean("hasPreviousOutput", hasPreviousOutput);
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

	@Override
	public void onConfigReload(ConfigurationManager configManager) {
		if(!ModSchematicBuilder.useEnergy)
			return;
		
		ModSchematicBuilder mod = ModSchematicBuilder.instance;
		energyStorage.setCapacity(mod.configGeneral.builderMaxEnergy);
		energyCostNOP = mod.configGeneral.builderEnergyCostNOP;
		energyCostPlace = mod.configGeneral.builderEnergyCostPlace;
		energyModifierPass2 = mod.configGeneral.builderEnergyModifierPass2;
	}
	
}
