package com.wildex999.schematicbuilder;

import io.netty.buffer.ByteBuf;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ResourceItem {
	
	private static Random rand = new Random();
	
	private Block block;
	private byte meta;
	
	private ItemStack item;
	private float itemCostPerBlock; //Number of items that are consumed for each Block placed
	
	public int placedCount; //Number of Blocks of this resource that have been placed
	public int blockCount; //Number of Blocks of this resource that exists in the Schematic
	public int storedItemCount; //Number of items for this Resource that has been stored for use when placing
	
	public ResourceItem(Block block, byte meta) {
		//Try to find itemStack for given block
		this.block = block;
		this.meta = meta;
		item = new ItemStack(block, 1, meta);
		if(item.getItem() == null)
		{
			Item namedItem = (Item)Item.itemRegistry.getObject(Block.blockRegistry.getNameForObject(block));
			if(namedItem != null)
				item = new ItemStack(namedItem, 1, meta);
			else
				item = null;
		}
		//TODO: Load replacement from Config
		//TODO: Unknown are defines as "Unobtainium", and can be gained by using an tool on the actual block to feed it.
		//I.e, to get redstone_wire, use the tool on redstone_wire block in world, gaining one of it and destroying the block and any items dropped.
	}
	
	public ResourceItem(Block block, ItemStack item, byte meta) {
		this.block = block;
		this.item = item;
		this.meta = meta;
	}
	
	public Block getBlock() {
		return block;
	}
	
	public byte getMeta() {
		return meta;
	}
	
	public ItemStack getItem() {
		return item;
	}
	
	public void toBytes(ByteBuf buf) {
		
	}
	
	public void fromBytes(ByteBuf buf) {
		
	}

}
