package com.wildex999.schematicbuilder.network;

import java.io.ByteArrayOutputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.wildex999.schematicbuilder.network.MessageUpdateSchematicBuilder.UpdateType;
import com.wildex999.schematicbuilder.schematic.Schematic;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

//Upload Schematic to server

//TODO: Allow for splitting into multiple part
//With a Start type to indicate an upload, with name and size(And to check if allowed)
//A Data type to contain data, limited to a certain amount of bytes per message
//And an End indicator

//If the Upload is allowed the server will send an 'Uploading' state to the client uploading,
//While everyone else will get an 'Downloading' state.

public class MessageUploadSchematic extends MessageBase {

	enum UploadType {
		START(0),
		DATA(1),
		END(2);
		
		private final int value;
		
		//Map of values for rapid lookup from serialized
		private static Map<Integer, UploadType> enumMap;
		static {
			enumMap = new HashMap<Integer, UploadType>();
			for(UploadType state : EnumSet.allOf(UploadType.class))
				enumMap.put(state.value, state);
		}
		
		UploadType(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
		
		public static UploadType fromValue(int value) {
			return enumMap.get(value);
		}
	};
	
	public static int packetSize = 30000;
	
	TileEntityInfo tileInfo;
	UploadType type;
	Schematic schematic;
	
	int size;
	byte[] data;
	
	//Receive Constructor
	public MessageUploadSchematic() {
		
	}
	
	//Send Constructor(Start)
	public MessageUploadSchematic(TileEntity tile, int size) {
		tileInfo = new TileEntityInfo(tile);
		this.size = size;
		type = UploadType.START;
	}
	
	//Send Constructor(Data)
	public MessageUploadSchematic(TileEntity tile, byte[] data) {
		tileInfo = new TileEntityInfo(tile);
		this.data = data;
		type = UploadType.DATA;
	}
	
	//Send Constructor(End)
	public MessageUploadSchematic(TileEntity tile) {
		tileInfo = new TileEntityInfo(tile);
		type = UploadType.END;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		tileInfo = readTileEntity(buf);
		type = UploadType.fromValue(buf.readInt());
		
		switch(type) {
		case START:
			size = buf.readInt();
			break;
		case DATA:
			data = new byte[buf.readInt()];
			buf.readBytes(data);
			break;
		case END:
			break;
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		writeTileEntity(buf, tileInfo);
		buf.writeInt(type.getValue());
		
		switch(type) {
		case START:
			buf.writeInt(size);
			break;
		case DATA:
			buf.writeInt(data.length);
			buf.writeBytes(data);
			break;
		case END:
			break;
		}
	}

	public static class Handler implements IMessageHandler<MessageUploadSchematic, IMessage> {
        
        @Override
        public IMessage onMessage(MessageUploadSchematic message, MessageContext ctx) {
        	World world = getWorld(ctx);
        	TileEntity baseTile = message.tileInfo.getTileEntity(world);
        	
        	if(!(baseTile instanceof TileSchematicBuilder))
        		return null;
        	TileSchematicBuilder tile = (TileSchematicBuilder)baseTile;

        	EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        	
        	switch(message.type) {
        	case START:
        		tile.networkOnUploadStart(message.size, player);
        		break;
        	case DATA:
        		tile.networkOnUploadData(message.data, player);
        		break;
        	case END:
        		tile.networkOnUploadEnd(player);
        		break;
        	}
        	
        	return null;
        }
    }
	
}
