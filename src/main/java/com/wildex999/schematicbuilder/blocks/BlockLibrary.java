package com.wildex999.schematicbuilder.blocks;


import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;

public class BlockLibrary {
	
	public static BlockSchematicBuilder schematicBuilder;
	public static BlockCreativeSchematicBuilder creativeSchematicBuilder;
	public static BlockSchematicCreator schematicCreator;
	public static BlockUnknown unknown;
	
	public static void init()
	{
		schematicBuilder = new BlockSchematicBuilder(true);
		creativeSchematicBuilder = new BlockCreativeSchematicBuilder();
		schematicCreator = new BlockSchematicCreator(true);
		unknown = new BlockUnknown();
	}
	
	public static void register(BlockBase block)
	{
		GameRegistry.registerBlock(block, block.getProperName());
	}
	
	public static void register(BlockBase block, Class<? extends ItemBlock> itemclass) 
	{
		GameRegistry.registerBlock(block, itemclass, block.getProperName());
	}
}
