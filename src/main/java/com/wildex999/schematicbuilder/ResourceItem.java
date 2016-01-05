package com.wildex999.schematicbuilder;

import io.netty.buffer.ByteBuf;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ResourceItem {
	
	private static Random rand = new Random();
	
	//Original BlockID and Meta. Used when changing the placed block from the Schematic original
	//The Resource Entry can potentially define a different block than the one given here.
	private short schematicBlockId;
	private byte schematicMeta;
	
	private ResourceEntry resourceEntry;
	
	public int placedCount; //Number of Blocks of this resource that have been placed
	public int blockCount; //Number of Blocks of this resource that exists in the Schematic
	public int storedItemCount; //Number of items for this Resource that has been stored for use when placing
	
	public ResourceItem(short schematicBlockId, byte schematicMeta, ResourceEntry entry) {
		this.resourceEntry = entry;
		this.schematicBlockId = schematicBlockId;
		this.schematicMeta = schematicMeta;
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
	
	public void toBytes(ByteBuf buf) {
		
	}
	
	public void fromBytes(ByteBuf buf) {
		
	}

}
