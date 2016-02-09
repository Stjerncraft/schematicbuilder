package com.wildex999.schematicbuilder.blocks;


import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class BlockLibrary {
	
	public static BlockSchematicBuilder schematicBuilder;
	public static BlockCreativeSchematicBuilder creativeSchematicBuilder;
	public static BlockSchematicCreator schematicCreator;
	
	public static void init()
	{
		schematicBuilder = new BlockSchematicBuilder(true);
		creativeSchematicBuilder = new BlockCreativeSchematicBuilder();
		schematicCreator = new BlockSchematicCreator(true);
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
