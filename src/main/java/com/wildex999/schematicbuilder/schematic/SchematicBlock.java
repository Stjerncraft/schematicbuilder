package com.wildex999.schematicbuilder.schematic;

import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;

//Data for a block in a Schematic

public class SchematicBlock {
	private static final FMLControlledNamespacedRegistry<Block> BlockRegistry = GameData.getBlockRegistry();
	
	private short block;
	public byte metaData;
	
	public SchematicBlock(Block block, byte metaData) {
		this.block = (short) BlockRegistry.getId(block);
		this.metaData = metaData;
	}
	
	public SchematicBlock(int blockId, byte metaData) {
		this.block = (short) blockId;
		this.metaData = metaData;
	}
	
	public Block getBlock() {
		return BlockRegistry.getObjectById(block);
	}
	
	public int getBlockId() {
		return block;
	}
}
