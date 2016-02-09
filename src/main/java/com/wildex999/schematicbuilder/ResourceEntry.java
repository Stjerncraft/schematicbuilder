package com.wildex999.schematicbuilder;

import com.wildex999.schematicbuilder.config.ConfigurationManager;
import com.wildex999.schematicbuilder.config.IConfigListener;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ResourceEntry implements IConfigListener {
	
	public static ResourceEntry Unknown = new ResourceEntry(Blocks.air, (byte) 0); //No known Server equivalent for Block
	public static ResourceEntry Banned = new ResourceEntry(Blocks.air, (byte) 0); //The chosen entry is banned on this server(TODO: Implement banning of blocks/items)
	
	//The server BlockId and Meta(Immutable due to map consistency)
	public final Block block;
	public final short blockId;
	public final byte meta;
	
	//These values can change
	public ItemStack item;
	public float itemCostPerBlock; //Number of items that are consumed for each Block placed
	public boolean ignoreItemMeta; //Allow item with any meta to be used
	public boolean guessedItem; //True if the item was guessed rather than provided
	
	//Constructor for trying to guess the item and cost for the given block & meta
	public ResourceEntry(Block block, byte meta) {
		//Try to find itemStack for given block
		this.block = block;
		this.blockId = (short) Block.getIdFromBlock(block);
		this.meta = meta;
		
		//TODO: Read config
		setItem(null, 0, false); //Allow it to guess the item
	}
	
	//Constructor when item and cost is known
	public ResourceEntry(Block block, byte meta, ItemStack item, float costPerBlock, boolean ignoreMeta) {
		this.block = block;
		this.blockId = (short) Block.getIdFromBlock(block);
		this.meta = meta;
		
		setItem(item, costPerBlock, ignoreMeta);
	}
	
	public void setItem(ItemStack item, float costPerBlock, boolean ignoreMeta) {
		//Remove previous mapping
		if(this.item != null)
			ModSchematicBuilder.resourceManager.removeItemMapping(this.item.getItem(), this);
		
		//Set Item
		if(item != null && item.getItem() != null)
		{
			this.itemCostPerBlock = costPerBlock;
			this.ignoreItemMeta = ignoreMeta;
			guessedItem = false;
		}
		else
		{
			//Try to guess Item if none provided
			item = new ItemStack(block, 1, meta);
			if(item.getItem() == null)
			{
				Item namedItem = Item.getItemFromBlock(block);
				if(namedItem != null)
					item = new ItemStack(namedItem, 1, meta);
				else
					item = null;
			}
			this.itemCostPerBlock = 1;
			this.ignoreItemMeta = true; //Ignore meta by default(Most rotated blocks) at the cost of allowing exploits(Wool color)	
			guessedItem = true;
			
		}
		
		//Add mapping
		if(item != null)
		{
			ModSchematicBuilder.resourceManager.addItemMapping(item.getItem(), this);
		}
		else
		{
			//TODO: Unknown are defines as "Unobtainium", and can be gained by using an tool on the actual block to feed it.
			//I.e, to get redstone_wire, use the tool on redstone_wire block in world, gaining one of it and destroying the block and any items dropped.
		}
		
		this.item = item;
		
	}

	@Override
	public void onConfigReload(ConfigurationManager configManager) {
		//TODO: Read new value from config
	}
}
