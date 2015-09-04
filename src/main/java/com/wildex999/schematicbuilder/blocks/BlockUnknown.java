package com.wildex999.schematicbuilder.blocks;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;

import com.wildex999.schematicbuilder.items.ItemSchematicBuilder;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;

public class BlockUnknown extends BlockBase {
	public final String name = "Unknown Schematic Block";
	
	public BlockUnknown()
	{
		this.setBlockName(name);
		this.setHardness(1f);
		this.setResistance(3f);
		this.setCreativeTab(CreativeTabs.tabBlock);
		this.setStepSound(Block.soundTypeMetal);
		
		BlockLibrary.register(this, null);
	}
}
