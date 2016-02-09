package com.wildex999.schematicbuilder.schematic;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;

/*
 * Mapping from Schematic BlockId/name to Server BlockId
 * 
 */

public class SchematicMap {
	public short schematicBlockId;
	public byte schematicMeta;
	public String schematicBlockName; //Name this was originally mapped to
	
	public short blockId; //The Server BlockId, or -1 if no server id is known
	public byte meta;
	
	
	public void toBytes(ByteBuf buf) {
		buf.writeShort(schematicBlockId);
		buf.writeByte(schematicMeta);
		if(schematicBlockName == null)
			ByteBufUtils.writeUTF8String(buf, "");
		else
			ByteBufUtils.writeUTF8String(buf, schematicBlockName);
		
		//Assumption: This map is only serialized to be sent between server and client, not stored between restarts.
		//Thus, the block ID's should not change, and we need not serialize the current name of the block.
		buf.writeShort(blockId);
		buf.writeByte(meta);
	}


	public void fromBytes(ByteBuf buf) {
		schematicBlockId = buf.readShort();
		schematicMeta = buf.readByte();
		schematicBlockName = ByteBufUtils.readUTF8String(buf);
		
		blockId = buf.readShort();
		meta = buf.readByte();
	}
	
}
