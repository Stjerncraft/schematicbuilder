package com.wildex999.schematicbuilder;

import java.util.HashMap;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import com.wildex999.schematicbuilder.config.ConfigurationManager;
import com.wildex999.schematicbuilder.config.IConfigListener;

public class ResourceManager implements IConfigListener{

	private HashMap<String, ResourceEntry> resourceMap;
	
	public ResourceManager() {
		resourceMap = new HashMap<String, ResourceEntry>();
		//TODO: Get ConfigurationManagerResource and load inn known all entries
		//TODO: Save to Config any local changes/new entries
	}
	
	//Returns the old entry or null
	public ResourceEntry setEntry(Block block, byte meta, ResourceEntry entry)
	{
		return setEntry(Block.blockRegistry.getNameForObject(block), meta, entry);
	}
	
	//Returns the old entry or null
	public ResourceEntry setEntry(String blockName, byte meta, ResourceEntry entry)
	{
		ResourceEntry oldEntry = resourceMap.put(blockName+"_"+meta, entry);
		if(oldEntry != null)
			oldEntry.valid = false;
		
		return oldEntry;
	}
	
	public ResourceEntry getOrCreate(String blockName, byte meta)
	{
		ResourceEntry entry = getEntry(blockName, meta);
		if(entry == null)
		{
			Block block = (Block) Block.blockRegistry.getObject(blockName);
			entry = new ResourceEntry(block, meta);
			setEntry(blockName, meta, entry);
		}
		return entry;
	}
	
	//Get the Resource Entry for the given block and metadata
	public ResourceEntry getEntry(String blockName, byte meta)
	{
		return resourceMap.get(blockName+"_"+meta);
	}
	
	//Get the Resource Entry for the given block and metadata
	public ResourceEntry getEntry(Block block, byte meta) {
		return getEntry(Block.blockRegistry.getNameForObject(block), meta);
	}
	
	@Override
	public void onConfigReload(ConfigurationManager configManager) {
		//TODO: Update all the existing entries
		//TODO: Read any new entries
	}

}
