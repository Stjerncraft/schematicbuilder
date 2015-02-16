package com.wildex999.schematicbuilder.network;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

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
		FULL(0), //State, message and name
		MESSAGE(1); //State and message
		
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
	String message;
	UpdateType updateType;
	
	//Receive Constructor
	public MessageUpdateSchematicBuilder() {}
	
	//Send Constructor(FULL)
	public MessageUpdateSchematicBuilder(TileEntity tile, BuilderState state, String name, String message) {
		tileInfo = new TileEntityInfo(tile);
		newState = state;
		schematicName = name;
		this.message = message;
		updateType = UpdateType.FULL;
	}
	
	//Send Constructor(MESSAGE)
	public MessageUpdateSchematicBuilder(TileEntity tile, BuilderState state, String message) {
		tileInfo = new TileEntityInfo(tile);
		newState = state;
		this.message = message;
		updateType = UpdateType.MESSAGE;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		tileInfo = readTileEntity(buf);
		
		updateType = UpdateType.fromValue(buf.readInt());
		switch(updateType) {
		case MESSAGE:
			newState = BuilderState.fromValue(buf.readInt());
			message = ByteBufUtils.readUTF8String(buf);
			break;
		case FULL:
			newState = BuilderState.fromValue(buf.readInt());
			message = ByteBufUtils.readUTF8String(buf);
			schematicName = ByteBufUtils.readUTF8String(buf);
			break;
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		writeTileEntity(buf, tileInfo);
		
		buf.writeInt(updateType.getValue());
		switch(updateType) {
		case MESSAGE:
			buf.writeInt(newState.getValue());
			ByteBufUtils.writeUTF8String(buf, message);
			break;
		case FULL:
			buf.writeInt(newState.getValue());
			ByteBufUtils.writeUTF8String(buf, message);
			ByteBufUtils.writeUTF8String(buf, schematicName);
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
        		tile.networkUpdateFull(message.newState, message.schematicName, message.message);
        		break;
        	case MESSAGE:
        		tile.networkUpdateMessage(message.newState, message.message);
        		break;
        	}
        	
        	return null;
        }
    }

}
