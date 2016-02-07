package com.wildex999.schematicbuilder.inventory;

import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInvBasic;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

public class InventorySchematicBuilder implements ISidedInventory {

	private ItemStack[] inventoryContents;
	private String customName;
	private TileSchematicBuilder tile;
	
	public int maxStackSize = 64;
	
	public InventorySchematicBuilder(String name, boolean customName, int slotCount, TileSchematicBuilder tile) {
		inventoryContents = new ItemStack[slotCount];
		
		if(customName)
			this.customName = name;
		else
			this.customName = "";
		
		this.tile = tile;
	}
	
	public ItemStack[] getItems() {
		return this.inventoryContents;
	}

	@Override
	public int getSizeInventory() {
		return inventoryContents.length;
	}

	@Override
	public ItemStack getStackInSlot(int slotIndex) {
		if(slotIndex >= 0 && slotIndex < inventoryContents.length)
			return inventoryContents[slotIndex];
		else
			return null;
	}

	@Override
	public ItemStack decrStackSize(int slotIndex, int count) {
		if(slotIndex < 0 || slotIndex >= inventoryContents.length)
			return null;
		
        if (inventoryContents[slotIndex] != null)
        {
            ItemStack itemstack;

            if (inventoryContents[slotIndex].stackSize <= count)
            {
                itemstack = inventoryContents[slotIndex];
                inventoryContents[slotIndex] = null;
                this.markDirty();
                return itemstack;
            }
            else
            {
                itemstack = inventoryContents[slotIndex].splitStack(count);

                if (inventoryContents[slotIndex].stackSize == 0)
                    inventoryContents[slotIndex] = null;

                this.markDirty();
                return itemstack;
            }
        }
        else
            return null;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slotIndex) {
		if(slotIndex < 0 || slotIndex >= inventoryContents.length)
			return null;
		
        if (inventoryContents[slotIndex] != null)
        {
            ItemStack itemstack = this.inventoryContents[slotIndex];
            inventoryContents[slotIndex] = null;
            return itemstack;
        }
        else
            return null;
	}

	@Override
	public void setInventorySlotContents(int slotIndex, ItemStack item) {
		if(slotIndex < 0 || slotIndex >= inventoryContents.length)
			return;
		
        inventoryContents[slotIndex] = item;

        if (item != null && item.stackSize > this.getInventoryStackLimit())
            item.stackSize = this.getInventoryStackLimit();

        this.markDirty();
	}

	@Override
	public String getInventoryName() {
		return customName;
	}

	@Override
	public boolean hasCustomInventoryName() {
		return !customName.isEmpty();
	}

	@Override
	public int getInventoryStackLimit() {
		return maxStackSize;
	}

	@Override
	public void markDirty() {
		tile.markDirty(); //Pass it up to the tile so inventory content is saved
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		return true; //TODO: Player access control?
	}

	@Override
	public void openInventory() {
	}

	@Override
	public void closeInventory() {
	}

	@Override
	public boolean isItemValidForSlot(int slotIndex, ItemStack item) {
		if(slotIndex == TileSchematicBuilder.inventorySlotOutput)
			return false;
		
		if(!tile.canAcceptItem(item))
			return false;
		
		return true;
	}
	
	public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList itemsNBT = new NBTTagList();
        for (int i = 0; i < inventoryContents.length; ++i)
        {
        	ItemStack item = inventoryContents[i];
            if (item != null)
            {
                NBTTagCompound itemData = new NBTTagCompound();
                itemData.setByte("Slot", (byte)i);
                item.writeToNBT(itemData);
                itemsNBT.appendTag(itemData);
            }
        }
        nbt.setTag("Items", itemsNBT);
	}
	
	public void readFromNBT(NBTTagCompound nbt) {
		NBTTagList itemsNBT = nbt.getTagList("Items", 10); //NBT_TYPE_COMPOUND

		for (int i = 0; i < itemsNBT.tagCount(); ++i) {
			NBTTagCompound itemData = itemsNBT.getCompoundTagAt(i);
			int slotIndex = itemData.getByte("Slot");

			if (slotIndex >= 0 && slotIndex < inventoryContents.length)
				inventoryContents[slotIndex] = ItemStack.loadItemStackFromNBT(itemData);
		}
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return tile.accessibleSlots;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack item, int side) {
		if(slot == tile.inventorySlotOutput)
			return false;
		return this.isItemValidForSlot(slot, item);
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack item, int side) {
		if(slot == tile.inventorySlotInput)
			return false;
		return true;
	}

}
