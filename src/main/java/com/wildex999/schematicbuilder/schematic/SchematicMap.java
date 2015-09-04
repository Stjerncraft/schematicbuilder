package com.wildex999.schematicbuilder.schematic;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

/*
 * Mapping from Schematic BlockId/name to Server BlockId
 * 
 */

public class SchematicMap {
	public short originalBlockId;
	public byte originalMeta;
	public String originalName; //Name this was mapped to
	
	public short blockId; //The Server BlockId, or -1 if no server id is known
	public byte meta;
	
	
	public void toBytes(ByteBuf buf) {
		buf.writeShort(originalBlockId);
		buf.writeByte(originalMeta);
		if(originalName == null)
			ByteBufUtils.writeUTF8String(buf, "");
		else
			ByteBufUtils.writeUTF8String(buf, originalName);
		
		buf.writeShort(blockId);
		buf.writeByte(meta);
	}


	public void fromBytes(ByteBuf buf) {
		originalBlockId = buf.readShort();
		originalMeta = buf.readByte();
		ByteBufUtils.readUTF8String(buf);
		
		blockId = buf.readShort();
		meta = buf.readByte();
	}
	
}
