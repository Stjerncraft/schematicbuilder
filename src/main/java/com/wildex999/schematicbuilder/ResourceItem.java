package com.wildex999.schematicbuilder;

import io.netty.buffer.ByteBuf;

import java.util.Random;

import com.wildex999.utils.ModLog;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ResourceItem {
	
	//Original BlockID and Meta. Used when changing the placed block from the Schematic original
	//The Resource Entry can potentially define a different block than the one given here.
	//Immutable due to mapping consistency
	private final short schematicBlockId;
	private final byte schematicMeta;
	private final ResourceEntry resourceEntry;
	
	public int placedCount; //Number of Blocks of this resource that have been placed
	public int blockCount; //Number of Blocks of this resource that exists in the Schematic
	public int storedCount; //Number of items for this Resource that has been stored for use when placing
	
	public boolean valid; //Set to false when removed from mapping(Used mostly by client GUI)
	
	public ResourceItem(short schematicBlockId, byte schematicMeta, ResourceEntry entry) {
		this.resourceEntry = entry;
		this.schematicBlockId = schematicBlockId;
		this.schematicMeta = schematicMeta;
		
		valid = true;
	}
	
	public boolean isUnknown() {
		return resourceEntry == ResourceEntry.Unknown;
	}
	
	public boolean isBanned() {
		return resourceEntry == ResourceEntry.Banned;
	}
	
	public Block getBlock() {
		return resourceEntry.block;
	}
	
	public short getBlockId() {
		return resourceEntry.blockId;
	}
	
	public byte getMeta() {
		return resourceEntry.meta;
	}
	
	public ItemStack getItem() {
		return resourceEntry.item;
	}
	
	public float getItemCostPerBlock() {
		return resourceEntry.itemCostPerBlock;
	}
	
	public short getSchematicBlockId() {
		return schematicBlockId;
	}
	
	public byte getSchematicMeta() {
		return schematicMeta;
	}
	
	//public void setEntry(ResourceEntry entry) NO: A Resource should be immutable once created, or else the maps will be corrupt
	
	public void toBytes(ByteBuf buf) {
		buf.writeShort(schematicBlockId);
		buf.writeByte(schematicMeta);
		
		//Entry
		if(resourceEntry == ResourceEntry.Unknown)
			buf.writeShort(-1);
		else if(resourceEntry == ResourceEntry.Banned)
			buf.writeShort(-2);
		else
			buf.writeShort(resourceEntry.blockId); //Assumption: Block ID is the same between server and client(Save us from sending 1000+ strings)
		
		buf.writeByte(resourceEntry.meta);
		
		//Counting
		buf.writeInt(blockCount);
		buf.writeInt(placedCount);
		buf.writeInt(storedCount);
	}
	
	public static ResourceItem fromBytes(ByteBuf buf) {
		short schematicBlockId = buf.readShort();
		byte schematicMeta = buf.readByte();
		ResourceEntry entry;
		
		//Entry
		short blockId = buf.readShort();
		byte meta = buf.readByte();
		
		if(blockId == -1)
			entry = ResourceEntry.Unknown;
		else if(blockId == -2)
			entry = ResourceEntry.Banned;
		else
		{
			Block block = (Block)Block.blockRegistry.getObjectById(blockId);
			if(block == null)
			{
				ModLog.logger.error("Failed to get block with BlockID: " + blockId);
				return null;
			}
			entry = ModSchematicBuilder.resourceManager.getOrCreate(Block.blockRegistry.getNameForObject(block), meta);
		}
		
		//Counting
		ResourceItem item = new ResourceItem(schematicBlockId, schematicMeta, entry);
		item.blockCount = buf.readInt();
		item.placedCount = buf.readInt();
		item.storedCount = buf.readInt();
		
		return item;
	}

}
