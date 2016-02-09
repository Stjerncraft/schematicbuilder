package com.wildex999.schematicbuilder.items;

import net.minecraft.item.Item;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class ItemLibrary {	
	public static void init()
	{
	}
	
	public static void register(Item item, String name)
	{
		GameRegistry.registerItem(item, name);
	}
}
