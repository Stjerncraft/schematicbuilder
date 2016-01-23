package com.wildex999.schematicbuilder.items;

import com.wildex999.schematicbuilder.blocks.BlockBase;
import com.wildex999.schematicbuilder.blocks.BlockLibrary;
import com.wildex999.schematicbuilder.blocks.BlockSchematicBuilder;
import com.wildex999.schematicbuilder.blocks.BlockUnknown;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;

public class ItemLibrary {	
	public static void init()
	{
	}
	
	public static void register(Item item, String name)
	{
		GameRegistry.registerItem(item, name);
	}
}
