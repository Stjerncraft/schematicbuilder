package com.wildex999.schematicbuilder;

import io.netty.buffer.ByteBuf;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ResourceItem {
	
	private static Random rand = new Random();
	
	//Original BlockID and Meta. Used when chainging the placed block from the Schematic original
	private short schematicBlockId;
	private byte shematicMeta;
	
	private ResourceEntry resourceEntry;
	
	public int placedCount; //Number of Blocks of this resource that have been placed
	public int blockCount; //Number of Blocks of this resource that exists in the Schematic
	public int storedItemCount; //Number of items for this Resource that has been stored for use when placing
	
	public ResourceItem(ResourceEntry entry) {
		this.resourceEntry = entry;
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
	
	public void toBytes(ByteBuf buf) {
		
	}
	
	public void fromBytes(ByteBuf buf) {
		
	}

}
