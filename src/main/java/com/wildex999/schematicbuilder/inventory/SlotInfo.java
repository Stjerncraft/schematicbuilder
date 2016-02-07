package com.wildex999.schematicbuilder.inventory;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotInfo extends Slot {

	private ItemStack item;
	
	public SlotInfo(IInventory p_i1824_1_, int p_i1824_2_, int p_i1824_3_, int p_i1824_4_) {
		super(p_i1824_1_, p_i1824_2_, p_i1824_3_, p_i1824_4_);
	}

	@Override
	public boolean isItemValid(ItemStack item) {
		return false;
	}
	
	@Override
    public ItemStack getStack()
    {
        return item;
    }

	@Override
    public boolean getHasStack()
    {
        return item != null;
    }

	@Override
    public void putStack(ItemStack item)
    {
        this.item = item;
        this.onSlotChanged();
    }
	
}
