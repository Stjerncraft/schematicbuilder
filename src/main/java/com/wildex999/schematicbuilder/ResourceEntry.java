package com.wildex999.schematicbuilder;

import com.wildex999.schematicbuilder.config.ConfigurationManager;
import com.wildex999.schematicbuilder.config.IConfigListener;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ResourceEntry implements IConfigListener {
	public Block block;
	public byte meta;
	
	public ItemStack item;
	public float itemCostPerBlock; //Number of items that are consumed for each Block placed
	
	public boolean valid; //Set to false if a config reload has made this invalid
	
	//Constructor for trying to guess the item and cost for the given block & meta
	public ResourceEntry(Block block, byte meta) {
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
		itemCostPerBlock = 1;
		//TODO: Unknown are defines as "Unobtainium", and can be gained by using an tool on the actual block to feed it.
		//I.e, to get redstone_wire, use the tool on redstone_wire block in world, gaining one of it and destroying the block and any items dropped.
		
		valid = true;
	}
	
	//Constructor for loading Item and cost from config
	public ResourceEntry(Block block, byte meta, ItemStack item, float itemCostPerBlock) {
		this.block = block;
		this.item = item;
		this.meta = meta;
		this.itemCostPerBlock = itemCostPerBlock;
	}

	@Override
	public void onConfigReload(ConfigurationManager configManager) {
		//TODO: Read new value from config
	}
}
