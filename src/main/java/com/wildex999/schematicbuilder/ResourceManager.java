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
		//TODO: Get ConfigurationManagerResource and load in known all entries
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
		if(blockName == null || blockName.isEmpty())
			return ResourceEntry.Unknown;
		
		ResourceEntry entry = getEntry(blockName, meta);
		if(entry == null)
		{
			Block block = (Block) Block.blockRegistry.getObject(blockName);
			if(block != null)
				entry = new ResourceEntry(block, meta);
			else
				entry = ResourceEntry.Unknown;
				
			setEntry(blockName, meta, entry);
		}
		return entry;
	}
	
	//Get the Resource Entry for the given block and metadata
	//Returns null if no entry matching entry exists
	public ResourceEntry getEntry(String blockName, byte meta)
	{
		return resourceMap.get(blockName+"_"+meta);
	}
	
	//Get the Resource Entry for the given block and metadata
	//Returns null if no entry matching entry exists
	public ResourceEntry getEntry(Block block, byte meta) {
		return getEntry(Block.blockRegistry.getNameForObject(block), meta);
	}
	
	//Set the resource for the given resource map, using the schematic block id and meta inside the given resource as key
	public static void setResource(HashMap<Short, ResourceItem> resourceMap, ResourceItem resource) {
		short resourceIndex = (short) ((resource.getSchematicBlockId() << 4) | (resource.getSchematicMeta() & 0xF));
		resourceMap.put(resourceIndex, resource);
	}
	
	//Returns the resource for the given schematic block id and meta, using them as key.
	//Returns null if no resource exists for the given combination
	public static ResourceItem getResource(HashMap<Short, ResourceItem> resourceMap, short schematicBlockId, byte schematicMeta) {
		short resourceIndex = (short) ((schematicBlockId << 4) | (schematicMeta & 0xF));
		return resourceMap.get(resourceIndex);
	}
	
	@Override
	public void onConfigReload(ConfigurationManager configManager) {
		//TODO: Update all the existing entries
		//TODO: Read any new entries
	}

}
