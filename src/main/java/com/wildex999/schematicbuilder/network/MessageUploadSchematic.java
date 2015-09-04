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
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

//Upload Schematic to server


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
	
	public static int packetSize = 1024;
	
	TileEntityInfo tileInfo;
	UploadType type;
	Schematic schematic;
	
	int size;
	byte[] data;
	boolean abort;
	
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
	public MessageUploadSchematic(TileEntity tile, boolean abort) {
		tileInfo = new TileEntityInfo(tile);
		this.abort = abort;
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
			abort = buf.readBoolean();
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
			buf.writeBoolean(abort);
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

        	EntityPlayerMP player;
        	if(ctx.side == Side.SERVER)
        		player = ctx.getServerHandler().playerEntity;
        	else
        		player = null;
        	
        	switch(message.type) {
        	case START:
        		if(ctx.side == Side.SERVER)
        			tile.networkOnUploadStart(message.size, player);
        		else
        			tile.networkOnDownloadStart(message.size);
        		break;
        	case DATA:
        		if(ctx.side == Side.SERVER)
        			tile.networkOnUploadData(message.data, player);
        		else
        			tile.networkOnDownloadData(message.data);
        		break;
        	case END:
        		if(ctx.side == Side.SERVER)
        			tile.networkOnUploadEnd(player, message.abort);
        		else
        			tile.networkOnDownloadEnd(message.abort);
        		break;
        	}
        	
        	return null;
        }
    }
	
}
