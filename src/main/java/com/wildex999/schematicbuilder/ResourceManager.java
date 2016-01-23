package com.wildex999.schematicbuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.wildex999.schematicbuilder.config.ConfigurationManager;
import com.wildex999.schematicbuilder.config.IConfigListener;

public class ResourceManager implements IConfigListener{

	private HashMap<String, ResourceEntry> resourceMap;
	private HashMap<Item, List<ResourceEntry>> resourceItemMap;
	
	public ResourceManager() {
		resourceMap = new HashMap<String, ResourceEntry>();
		resourceItemMap = new HashMap<Item, List<ResourceEntry>>();
		//TODO: Get ConfigurationManagerResource and load in known all entries
		//TODO: Save to Config any local changes/new entries
		//TODO: Allow for dumping all unknown blocks/items to file
	}
	
	//Returns the old entry or null
	public ResourceEntry setEntry(Block block, byte meta, ResourceEntry entry)
	{
		return setEntry(Block.blockRegistry.getNameForObject(block), meta, entry);
	}
	
	//Set the new entry, and return the old entry or null
	public ResourceEntry setEntry(String blockName, byte meta, ResourceEntry entry)
	{
		ResourceEntry oldEntry = resourceMap.put(blockName+"_"+meta, entry);
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
	
	public ResourceEntry getOrCreate(Block block, byte meta) {
		return getOrCreate(Block.blockRegistry.getNameForObject(block), meta);
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
	
	public void addItemMapping(Item item, ResourceEntry resourceEntry) {
		List<ResourceEntry> entryList = resourceItemMap.get(item);
		
		if(entryList == null)
		{
			entryList = new ArrayList<ResourceEntry>();
			resourceItemMap.put(item, entryList);
		}
		
		entryList.add(resourceEntry);
	}
	
	//Remove mapping between item and entry
	//Returns true if an entry was removed, or false if none existed
	public boolean removeItemMapping(Item item, ResourceEntry resourceEntry) {
		List<ResourceEntry> entryList = resourceItemMap.get(item);
		
		if(entryList == null)
			return false;
		
		return entryList.remove(resourceEntry);
	}
	
	public List<ResourceEntry> getItemMapping(Item item) {
		return resourceItemMap.get(item);
	}
	
	//Set the resource for the given resource map, using the schematic block id and meta inside the given resource as key
	public static void setResource(HashMap<Short, ResourceItem> resourceMap, HashMap<Short, List<ResourceItem>> backMap, ResourceItem resource) {
		short resourceIndex = (short) ((resource.getSchematicBlockId() << 4) | (resource.getSchematicMeta() & 0xF));
		ResourceItem oldItem = resourceMap.get(resourceIndex);
		
		if(oldItem != null)
			removeResource(resourceMap, backMap, oldItem);
		
		resourceMap.put(resourceIndex, resource);

		if(resource.isUnknown() || resource.isBanned())
			return;
		
		//Back map
		if(backMap == null)
			return;
		short serverBlockId = resource.getBlockId();
		List<ResourceItem> resources = getResourcesUsing(backMap, serverBlockId, resource.getMeta());
		if(resources == null)
		{
			resources = new ArrayList<ResourceItem>();
			short backIndex = (short) ((serverBlockId << 4) | (resource.getMeta() & 0xF));
			backMap.put(backIndex, resources);
		}
		resources.add(resource);
		//TODO: Verify we have no duplicates
	}
	
	//Returns the resource for the given schematic block id and meta, using them as key.
	//Returns null if no resource exists for the given combination
	public static ResourceItem getResource(HashMap<Short, ResourceItem> resourceMap, short schematicBlockId, byte schematicMeta) {
		short resourceIndex = (short) ((schematicBlockId << 4) | (schematicMeta & 0xF));
		return resourceMap.get(resourceIndex);
	}
	
	//Returns a list of resources using the given server block id and meta.
	public static List<ResourceItem> getResourcesUsing(HashMap<Short, List<ResourceItem>> backMap, short serverBlockId, byte serverMeta) {
		short resourceIndex = (short) ((serverBlockId << 4) | (serverMeta & 0xF));
		return backMap.get(resourceIndex);
	}
	
	public static boolean removeResource(HashMap<Short, ResourceItem> resourceMap, HashMap<Short, List<ResourceItem>> backMap, ResourceItem resource) {
		short resourceIndex = (short) ((resource.getSchematicBlockId() << 4) | (resource.getSchematicMeta() & 0xF));
		resource.valid = false;
		if(resourceMap.remove(resourceIndex) == null)
			return false;
		
		List<ResourceItem> items = backMap.get(resourceIndex);
		if(items == null)
			return true;
		
		items.remove(resource);
		if(items.isEmpty())
			backMap.remove(resourceIndex);
		
		return true;
	}
	
	@Override
	public void onConfigReload(ConfigurationManager configManager) {
		//TODO: Update all the existing entries
		//TODO: Read any new entries
	}

}
