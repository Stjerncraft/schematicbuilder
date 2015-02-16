package com.wildex999.schematicbuilder.tiles;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.wildex999.schematicbuilder.exceptions.ExceptionLoad;
import com.wildex999.schematicbuilder.exceptions.ExceptionRead;
import com.wildex999.schematicbuilder.gui.IGuiWatchers;
import com.wildex999.schematicbuilder.gui.SchematicBuilderGui;
import com.wildex999.schematicbuilder.gui.SchematicBuilderGui.GUI;
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
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class TileSchematicBuilder extends TileEntity implements IGuiWatchers {

	protected final static String inventoryName = "Schematic Builder";
	protected InventoryBasic inventory;
	
	public BuilderState state;
	public String message;
	public String schematicName;
	
	@SideOnly(Side.CLIENT)
	private FileChooser currentChooser;
	@SideOnly(Side.CLIENT)
	public String filePath;
	
	private Set<EntityPlayer> watchers;
	
	private Schematic loadedSchematic;
	private SchematicBlock defaultBlock;
	private List<SchematicBlock> postBlocks; //Blocks to be placed after everything has been placed
	private int direction; //Direction the Builder is facing(Build direction)
	private int lastX, lastY, lastZ; //Current offset in building
	private int buildX, buildY, buildZ; //Start position for building
	private int placedCount; 
	private boolean initialized;
	
	private int currentPass; //Multiple passes are required for some blocks
	private int passCount = 2;
	
	private FMLControlledNamespacedRegistry<Block> blockRegistry;
	
	//Uploading
	@SideOnly(Side.CLIENT)
	private boolean uploadApproved; //Set to true when server allows us to start uploading
	private boolean inUpload; //Set to true when Client is uploading/Server is downloading
	private byte[] uploadData;
	private int uploadOffset;
	private EntityPlayerMP uploader; //Set by server to track who is uploading
	
	public TileSchematicBuilder() {
		inventory = new InventoryBasic(inventoryName, true, 27);
		state = BuilderState.IDLE;
		schematicName = "None";
		message = "";
		
		watchers = new HashSet<EntityPlayer>();
		blockRegistry = GameData.getBlockRegistry();
		defaultBlock = new SchematicBlock(Blocks.air, (byte) 0);
		
		loadedSchematic = null;
		direction = 0;
		lastX = lastY = lastZ = 0;
		buildX = buildY = buildZ = 0;
		initialized = false;
		
		inUpload = false;
		uploader = null;
	}
	
	public IInventory getInventory() {
		return inventory;
	}
	
	//Get the GUI for this tile, if it's the current one
	@SideOnly(Side.CLIENT)
	public SchematicBuilderGui.GUI getCurrentGUI() {
		net.minecraft.client.gui.GuiScreen currentGui = net.minecraft.client.Minecraft.getMinecraft().currentScreen;
		if(!(currentGui instanceof SchematicBuilderGui.GUI))
			return null;
		
		return (SchematicBuilderGui.GUI)currentGui;
	}
	
	private void updateGui() {
		SchematicBuilderGui.GUI gui = getCurrentGUI();
		if(gui != null)
			gui.update();
	}
	
	@Override
	public void updateEntity() {
		if(worldObj.isRemote)
		{
			clientUpdate();
			return;
		}
		
		if(!initialized)
			initialize();
		
		if(state == BuilderState.BUILDING)
			serverBuildUpdate();
		
	}
	
	//Initialize a newly created Schematic Builder
	public void initialize() {
		direction = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
		initialized = true;
	}
	
	//Get the World equivalent of the Schematic position, this depends on the direction,
	//and using lastX and lastY.
	//Direction:
    //0 = South(Positive Z)
    //1 = West (Negative X)
    //2 = North(Negative Z)
    //3 = East (Positive X)
	/*private int getWorldX() {
		switch(direction)
		{
		case 0:
			return xCoord - lastX;
		case 1:
			return xCoord - lastZ;
		case 2:
			return xCoord + lastX;
		case 3:
			return xCoord + lastZ;
		}
		return lastX;
	}
	private int getWorldZ() {
		switch(direction)
		{
		case 0:
			return zCoord + (lastZ + 1);
		case 1:
			return zCoord - (lastX + 1);
		case 2:
			return zCoord - (lastZ + 1);
		case 3:
			return zCoord + (lastX + 1);
		}
		return lastZ;
	}*/
	
	//Do the Building on the server
	public void serverBuildUpdate() {
		int width = loadedSchematic.getWidth();
		int height = loadedSchematic.getHeight();
		int length = loadedSchematic.getLength();
		int airRepeat = 5; //Allow 5 Air blocks per tick (TODO: Place in config)
		if(currentPass == 1)
			airRepeat = 10; //10 Air blocks per tick on pass 2
		SchematicBlock block = null;
		Block newBlock = null;
		
		do {
			int x = buildX + lastX;
			int y = buildY + lastY;
			int z = buildZ + lastZ + 1;

			block = loadedSchematic.getBlock(lastX, lastY, lastZ);
			
			switch(currentPass) {
			case 0: //Pass 1: Initial placement
				//Place the block
				if(block == null)
					block = defaultBlock;
				newBlock = block.getBlock();
				
				Block oldBlock = worldObj.getBlock(x, y, z);
				newBlock = block.getBlock();
				
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
				break;
			case 1: //Pass 2: Place missing blocks, and correct metadata
				if(block == null)
					block = defaultBlock;
				newBlock = block.getBlock();
				
				if(newBlock != Blocks.air)
				{
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
			}
			if(lastY >= height)
			{
				lastY = 0;
				if(++currentPass >= passCount)
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
		} while(airRepeat-- > 0 && newBlock == Blocks.air);
		
		//Send progress update
		int count = width*height*length;
		count *= passCount;
		
		float percentage = placedCount / (float)count;
		NumberFormat format = new DecimalFormat("0.00");
		message = format.format(percentage*100.0) + "%(Pass " + (currentPass+1) + ")";
		
		sendNetworkUpdateFull(null);
		
	}
	
	@SideOnly(Side.CLIENT)
	public void clientUpdate() {
		if(currentChooser != null && state != BuilderState.CHOOSINGLOCAL)
			currentChooser = null;
		updateSchematicChoosing();
		
		if(state == BuilderState.UPLOADING)
			updateUploadSchematic();
			
	}
	
	@SideOnly(Side.CLIENT)
	private void updateUploadSchematic() {
		if(!inUpload || !uploadApproved)
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
			inUpload = false;
			uploadApproved = false;
			uploadData = null;
			
			msg = new MessageUploadSchematic(this);
			msg.sendToServer();
		}
	}
	
	@SideOnly(Side.CLIENT)
	public void updateSchematicChoosing() {
		if(currentChooser != null)
		{
			if(currentChooser.isFileChoosen())
			{
				//Start Loading
				filePath = currentChooser.getFileChoosen();
				if(currentChooser.getReturnValue() == currentChooser.returnCanceled)
				{
					state = BuilderState.IDLE;
					//TODO: If a schematic was already loaded, go back to READY
				}
				else
				{
					File file = new File(currentChooser.getFileChoosen());
					state = BuilderState.READING;
					//TODO: Do reading in thread?
					
					Schematic newSchematic = null;
					try {
						newSchematic = SchematicLoader.loadSchematic(file);
					} catch (Exception e) {
						state = BuilderState.ERROR;
						message = e.getMessage();
						ModLog.logger.warn(message);
					} 
					
					if(state != BuilderState.ERROR)
					{
						state = BuilderState.LOADING;
						loadedSchematic = newSchematic;
						//Send Schematic to server
						//TODO: Do the send over multiple message and with rate limit
						uploadSchematicToServer();
					}
				}
				currentChooser = null;

				updateGui();
			}
		}
	}
	
	@SideOnly(Side.CLIENT)
	public void uploadSchematicToServer() {
		inUpload = true;
		uploadApproved = false;
		uploadOffset = 0;
		
		ByteBuf buf = Unpooled.buffer();
		loadedSchematic.toBytes(buf);
		loadedSchematic = null; //Clear the used memory
		uploadData = buf.array();
		
		MessageBase uploadMessage = new MessageUploadSchematic(this, uploadData.length);
		uploadMessage.sendToServer();
		
		state = BuilderState.UPLOADING;
		message = "...";
	}
	
	//Show user dialog to load file from disk
	@SideOnly(Side.CLIENT)
	public void chooseSchematicFile() {
		state = BuilderState.CHOOSINGLOCAL;
		
		currentChooser = FileChooser.getFile(false);
		
		updateGui();
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
			return;
		
		watchers.remove(player);
	}
	
	public void sendNetworkUpdateFull(EntityPlayerMP player) {
		MessageBase netMessage = new MessageUpdateSchematicBuilder(this, state, schematicName, message);
		
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

	@SideOnly(Side.CLIENT)
	public void networkUpdateFull(BuilderState newState, String schematicName, String message) {
		state = newState;
		this.schematicName = schematicName;
		this.message = message;
		
		if(state == BuilderState.UPLOADING && inUpload)
			uploadApproved = true;
		
		updateGui();
	}

	@SideOnly(Side.CLIENT)
	public void networkUpdateMessage(BuilderState newState, String message) {
		state = newState;
		this.message = message;
		
		if(state == BuilderState.UPLOADING && inUpload)
			uploadApproved = true;
		
		updateGui();
	}
	
	public boolean canAcceptUpload() {
		return state == BuilderState.IDLE || state == BuilderState.ERROR || state == BuilderState.READY
				|| state == BuilderState.DONE || state == BuilderState.DONEMISSING || state == BuilderState.STOPPED;
	}

	
	public void networkOnUploadStart(int size, EntityPlayerMP uploader) {
		//Only allow Uploads if we aren't doing anything else
		if(!canAcceptUpload())
		{
			sendNetworkUpdateFull(uploader);
			return;
		}
		
		state = BuilderState.DOWNLOADING;
		message = "0%";
		inUpload = true;
		uploadOffset = 0;
		this.uploader = uploader;
		//TODO: Check for max size
		uploadData = new byte[size];
		sendNetworkUpdateMessage(null);
	}
	
	public void networkOnUploadData(byte[] data, EntityPlayerMP uploader) {
		if(!inUpload || this.uploader != uploader)
			return;
		
		System.arraycopy(data, 0, uploadData, uploadOffset, data.length);
		uploadOffset += data.length;
		
		if(uploadOffset > uploadData.length)
		{
			inUpload = false;
			uploadData = null;
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
	
	public void networkOnUploadEnd(EntityPlayerMP uploader) {
		if(!inUpload || this.uploader != uploader)
			return;
		
		inUpload = false;
		uploader = null;
		message = "";
		
		//Read the Schematic
		ByteBuf buf = Unpooled.wrappedBuffer(uploadData);
		uploadData = null; //Clear upload data
		loadedSchematic = Schematic.fromBytes(buf);
		
		state = BuilderState.READY;
		
		sendNetworkUpdateFull(null);
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
			currentPass = 0;
			postBlocks = new ArrayList<SchematicBlock>();
			
			buildX = xCoord;
			buildY = yCoord;
			buildZ = zCoord;
			
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
				buildX++;
				buildZ--;
			}
				
			
		}
		
		state = BuilderState.BUILDING;
		sendNetworkUpdateMessage(null);
	}

	public void actionStop() {
		if(state != BuilderState.BUILDING)
			return;
		
		state = BuilderState.STOPPED;
		sendNetworkUpdateMessage(null);
	}
	
}
