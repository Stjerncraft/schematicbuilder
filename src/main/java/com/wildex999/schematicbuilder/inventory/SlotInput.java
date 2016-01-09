package com.wildex999.schematicbuilder.inventory;

import java.util.List;

import com.wildex999.schematicbuilder.ResourceItem;
import com.wildex999.schematicbuilder.ResourceManager;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotInput extends Slot {

	private TileSchematicBuilder tile;
	
	public SlotInput(IInventory p_i1824_1_, int p_i1824_2_, int p_i1824_3_, int p_i1824_4_, TileSchematicBuilder tile) {
		super(p_i1824_1_, p_i1824_2_, p_i1824_3_, p_i1824_4_);
		this.tile = tile;
	}
	
	@Override
	public boolean isItemValid(ItemStack item) {
		//Check if item is required for current schematic
		if(!tile.canAcceptItem(item))
			return false;
		
		return true;
	}

}
