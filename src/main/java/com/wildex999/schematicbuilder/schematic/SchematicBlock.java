package com.wildex999.schematicbuilder.schematic;

import com.wildex999.schematicbuilder.blocks.BlockLibrary;

import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

//Data for a block in a Schematic

public class SchematicBlock {
	private static final FMLControlledNamespacedRegistry<Block> BlockRegistry = GameData.getBlockRegistry();
	
	private short block;
	private byte metaData;
	
	public SchematicBlock(Block block, byte metaData) {
		this.block = (short) Block.getIdFromBlock(block);
		this.metaData = metaData;
	}
	
	public SchematicBlock(int blockId, byte metaData) {
		this.block = (short) blockId;
		this.metaData = metaData;
	}
	
	public Block getServerBlock(Schematic schematic) {
		SchematicMap map = schematic.getSchematicMap(block, metaData, false);
		if(map == null || map.blockId == -1)
			return null;
		return BlockRegistry.getObjectById(map.blockId);
	}
	
	public byte getMeta(Schematic schematic) {
		SchematicMap map = schematic.getSchematicMap(block, metaData, false);
		if(map == null || map.blockId == -1)
			return metaData;
		return map.meta;
	}
	
	public short getServerBlockId(Schematic schematic) {
		SchematicMap map = schematic.getSchematicMap(block, metaData, false);
		if(map == null || map.blockId == -1)
			return -1;
		return map.blockId;
	}
	
	//Get original BlockId
	public short getSchematicBlockId() {
		return block;
	}
	
	public byte getSchematicMeta() {
		return metaData;
	}
	
	//Get the original name of block, or an empty string if not known
	public String getOriginalName(Schematic schematic) {
		SchematicMap map = schematic.getSchematicMap(block, metaData, false);
		if(map == null || map.blockId == -1)
			return "Error: No name for " + block + ":" + metaData;
		return map.schematicBlockName;
	}

}
