package com.wildex999.schematicbuilder.network;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.wildex999.schematicbuilder.ResourceItem;
import com.wildex999.schematicbuilder.tiles.BuilderState;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

//Sent to Client to update the GUI for Schematic Builder

public class MessageUpdateSchematicBuilder extends MessageBase {
	
	enum UpdateType {
		FULL(0), //State, schematic id, message and name
		MESSAGE(1), //State and message
		CONFIG(2), //Config Update
		RESOURCE(3); //Resources update
		
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
	
	int energyMax;
	int energyCurrent;
	ArrayList<ResourceItem> resources;
	
	//Receive Constructor
	public MessageUpdateSchematicBuilder() {}
	
	//Send Constructor(FULL)
	public MessageUpdateSchematicBuilder(TileEntity tile, BuilderState state, String name, String author, String message, String schematicId, int width, int height, int length, TileSchematicBuilder.Config config) {
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
		updateType = UpdateType.FULL;
	}
	
	//Send Constructor(MESSAGE)
	public MessageUpdateSchematicBuilder(TileEntity tile, BuilderState state, String message) {
		tileInfo = new TileEntityInfo(tile);
		newState = state;
		this.message = message;
		updateType = UpdateType.MESSAGE;
	}
	
	//Send Constructor(CONFIG)
	public MessageUpdateSchematicBuilder(TileEntity tile, TileSchematicBuilder.Config config) {
		tileInfo = new TileEntityInfo(tile);
		this.config = config;
		updateType = UpdateType.CONFIG;
	}
	
	//Send Constructor(RESOURCE)
	public MessageUpdateSchematicBuilder(TileEntity tile, int currentEnergy, int maxEnergy, ArrayList<ResourceItem> resources)
	{
		tileInfo = new TileEntityInfo(tile);
		energyCurrent = currentEnergy;
		energyMax = maxEnergy;
		this.resources = resources;
		updateType = UpdateType.RESOURCE;
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
			break;
		case RESOURCE:
			energyCurrent = buf.readInt();
			energyMax = buf.readInt();
			//TODO: Read Resources
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
			break;
		case RESOURCE:
			buf.writeInt(energyCurrent);
			buf.writeInt(energyMax);
			//TODO: Write resources
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
        		tile.networkUpdateFull(message.newState, message.schematicName, message.schematicAuthor, message.message, message.schematicId, message.schematicWidth, message.schematicHeight, message.schematicLength, message.config);
        		break;
        	case MESSAGE:
        		tile.networkUpdateMessage(message.newState, message.message);
        		break;
        	case CONFIG:
        		tile.networkUpdateConfig(message.config);
        		break;
        	case RESOURCE:
        		tile.networkUpdateResource(message.energyCurrent, message.energyMax, null);
        		break;
        	}
        	
        	return null;
        }
    }

}
