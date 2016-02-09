package com.wildex999.schematicbuilder.network;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.wildex999.schematicbuilder.ResourceItem;
import com.wildex999.schematicbuilder.tiles.BuilderState;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;
import com.wildex999.utils.ModLog;

import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

//Sent to Client to update the GUI for Schematic Builder

public class MessageUpdateSchematicBuilder extends MessageBase {
	
	enum UpdateType {
		FULL(0), //State, schematic id, message and name
		MESSAGE(1), //State and message
		CONFIG(2), //Config Update
		RESOURCE(3), //Resources update
		RESOURCELIST(4); //Full list of Resources and values
		
		private final int value;
		
		//Map of values for rapid lookup from serialized
		private static Map<Integer, UpdateType> enumMap;
		static {
			enumMap = new HashMap<Integer, UpdateType>();
			for(UpdateType state : EnumSet.allOf(UpdateType.class))
				enumMap.put(state.value, state);
		}
		
		UpdateType(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
		
		public static UpdateType fromValue(int value) {
			return enumMap.get(value);
		}
	};

	TileEntityInfo tileInfo;
	
	BuilderState newState;
	String schematicName;
	String schematicAuthor;
	String schematicId;
	int schematicWidth, schematicHeight, schematicLength;
	String message;
	UpdateType updateType;
	TileSchematicBuilder.Config config;
	boolean hasPreviousOutput;
	
	int energyMax;
	int energyCurrent;
	ArrayList<ResourceItem> resources;
	
	int floorPlaced, floorStored;
	
	//Receive Constructor
	public MessageUpdateSchematicBuilder() {}
	
	//Send Constructor(FULL)
	public MessageUpdateSchematicBuilder(TileSchematicBuilder tile, BuilderState state, String name, String author, String message, String schematicId, int width, int height, int length, TileSchematicBuilder.Config config) {
		tileInfo = new TileEntityInfo(tile);
		newState = state;
		schematicName = name;
		schematicAuthor = author;
		schematicWidth = width;
		schematicHeight = height;
		schematicLength = length;
		if(schematicId != null)
			this.schematicId = schematicId;
		else
			this.schematicId = "";
		this.message = message;
		this.config = config;
		this.hasPreviousOutput = tile.hasPreviousOutput;
		
		updateType = UpdateType.FULL;
	}
	
	//Send Constructor(MESSAGE)
	public MessageUpdateSchematicBuilder(TileSchematicBuilder tile, BuilderState state, String message) {
		tileInfo = new TileEntityInfo(tile);
		newState = state;
		this.message = message;
		updateType = UpdateType.MESSAGE;
	}
	
	//Send Constructor(CONFIG)
	public MessageUpdateSchematicBuilder(TileSchematicBuilder tile, TileSchematicBuilder.Config config) {
		tileInfo = new TileEntityInfo(tile);
		this.config = config;
		updateType = UpdateType.CONFIG;
	}
	
	//Send Constructor(RESOURCE)
	public MessageUpdateSchematicBuilder(TileSchematicBuilder tile, int currentEnergy, int maxEnergy, ArrayList<ResourceItem> resources, int floorPlaced, int floorStored)
	{
		tileInfo = new TileEntityInfo(tile);
		energyCurrent = currentEnergy;
		energyMax = maxEnergy;
		this.resources = resources;
		updateType = UpdateType.RESOURCE;
		
		this.floorPlaced = floorPlaced;
		this.floorStored = floorStored;
	}
	
	//Send Constructor(RESOURCELIST)
	public MessageUpdateSchematicBuilder(TileSchematicBuilder tile, ArrayList<ResourceItem> resources)
	{
		tileInfo = new TileEntityInfo(tile);
		this.resources = resources;
		updateType = UpdateType.RESOURCELIST;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		tileInfo = readTileEntity(buf);
		updateType = UpdateType.fromValue(buf.readInt());
		switch(updateType) {
		case CONFIG:
			config = new TileSchematicBuilder.Config();
			config.fromBytes(buf);
			break;
		case MESSAGE:
			newState = BuilderState.fromValue(buf.readInt());
			message = ByteBufUtils.readUTF8String(buf);
			break;
		case FULL:
			newState = BuilderState.fromValue(buf.readInt());
			message = ByteBufUtils.readUTF8String(buf);
			schematicName = ByteBufUtils.readUTF8String(buf);
			schematicAuthor = ByteBufUtils.readUTF8String(buf);
			schematicId = ByteBufUtils.readUTF8String(buf);
			schematicWidth = buf.readInt();
			schematicHeight = buf.readInt();
			schematicLength = buf.readInt();
			config = new TileSchematicBuilder.Config();
			config.fromBytes(buf);
			hasPreviousOutput = buf.readBoolean();
			break;
		case RESOURCE:
			energyCurrent = buf.readInt();
			energyMax = buf.readInt();
			floorPlaced = buf.readInt();
			floorStored = buf.readInt();
			//Fall through to next case, as it's the same code for reading the list
		case RESOURCELIST:
			int resourceCount = buf.readInt();
			resources = new ArrayList<ResourceItem>(resourceCount);
			
			if(resourceCount > 0)
			{
				for(int i=0; i < resourceCount; i++) {
					ResourceItem resource = ResourceItem.fromBytes(buf);
					if(resource == null)
					{
						//TODO: Show to player that an error occurred outside log
						ModLog.logger.error("Error while parsing network packet for SchematicBuilder at X:" + tileInfo.posX + " Y:" + tileInfo.posY + " Z:" + tileInfo.posZ);
						ModLog.logger.error("Unable to parse Resource Item.");
						resources.clear();
						return; //Stop Parsing, better show nothing than only partial data
					}
					resources.add(resource);
				}
			}
			
			break;
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		writeTileEntity(buf, tileInfo);
		
		buf.writeInt(updateType.getValue());
		switch(updateType) {
		case CONFIG:
			config.toBytes(buf);
			break;
		case MESSAGE:
			buf.writeInt(newState.getValue());
			ByteBufUtils.writeUTF8String(buf, message);
			break;
		case FULL:
			buf.writeInt(newState.getValue());
			ByteBufUtils.writeUTF8String(buf, message);
			ByteBufUtils.writeUTF8String(buf, schematicName);
			ByteBufUtils.writeUTF8String(buf, schematicAuthor);
			ByteBufUtils.writeUTF8String(buf, schematicId);
			buf.writeInt(schematicWidth);
			buf.writeInt(schematicHeight);
			buf.writeInt(schematicLength);
			config.toBytes(buf);
			buf.writeBoolean(hasPreviousOutput);
			break;
		case RESOURCE:
			buf.writeInt(energyCurrent);
			buf.writeInt(energyMax);
			buf.writeInt(floorPlaced);
			buf.writeInt(floorStored);
			
			//Fall Trough to ResourceList as it's the same code for writing them.
		case RESOURCELIST:
			//Full list of resources
			if(resources == null || resources.size() == 0)
				buf.writeInt(0);
			else
			{
				buf.writeInt(resources.size());
				
				for(ResourceItem item : resources) {
					item.toBytes(buf);
				}
			}
			resources.clear();
			
			break;
		}
	}
	
	public static class Handler implements IMessageHandler<MessageUpdateSchematicBuilder, IMessage> {
        
        @Override
        public IMessage onMessage(MessageUpdateSchematicBuilder message, MessageContext ctx) {
        	World world = getWorld(ctx);
        	TileEntity baseTile = message.tileInfo.getTileEntity(world);
        	
        	if(!(baseTile instanceof TileSchematicBuilder))
        		return null;
        	TileSchematicBuilder tile = (TileSchematicBuilder)baseTile;
        
        	
        	switch(message.updateType)
        	{
        	case FULL:
        		tile.networkUpdateFull(message.newState, message.schematicName, message.schematicAuthor, message.message, message.schematicId, 
        								message.schematicWidth, message.schematicHeight, message.schematicLength, message.config, message.hasPreviousOutput);
        		break;
        	case MESSAGE:
        		tile.networkUpdateMessage(message.newState, message.message);
        		break;
        	case CONFIG:
        		tile.networkUpdateConfig(message.config);
        		break;
        	case RESOURCE:
        		tile.networkUpdateResource(message.energyCurrent, message.energyMax, message.resources, message.floorPlaced, message.floorStored);
        		break;
        	case RESOURCELIST:
        		tile.networkUpdateResourceList(message.resources);
        		break;
        	}
        	
        	return null;
        }
    }

}
