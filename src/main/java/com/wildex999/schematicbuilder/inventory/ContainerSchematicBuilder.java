package com.wildex999.schematicbuilder.inventory;

import com.wildex999.schematicbuilder.gui.IGuiWatchers;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class ContainerSchematicBuilder extends Container {

	protected static int slotsPerRow = 9;
	protected static int slotSize = 18;
	
	private TileSchematicBuilder tile;
	private InventoryPlayer playerInventory;
	
	public ContainerSchematicBuilder(InventoryPlayer playerInventory, TileSchematicBuilder tile) {
		super();
		this.tile = tile;
		this.playerInventory = playerInventory;
		
		initPlayerInventory(playerInventory, 8, 158);
		initContainerInventory(tile.getInventory(), 8, 158);
		
		if(tile instanceof IGuiWatchers)
			((IGuiWatchers)tile).addWatcher(playerInventory.player);
	}
	
	@Override
	public void onContainerClosed(EntityPlayer player) {
		super.onContainerClosed(player);
		
		if(tile instanceof IGuiWatchers)
			((IGuiWatchers)tile).removeWatcher(player);
	}
	
	//Initialize the player inventory
	protected void initPlayerInventory(InventoryPlayer inventory, int playerInventoryX, int playerInventoryY) {
		int i;
		
        for (i = 0; i < 3; ++i)
        {
            for (int j = 0; j < slotsPerRow; ++j)
                this.addSlotToContainer(new Slot(inventory, j + i * slotsPerRow + slotsPerRow, playerInventoryX + j * slotSize, playerInventoryY + i * slotSize));
        }

        for (i = 0; i < slotsPerRow; ++i)
            this.addSlotToContainer(new Slot(inventory, i, playerInventoryX + i * slotSize, playerInventoryY + 58));
	}
	
	//Initialize the Container inventory
	protected void initContainerInventory(IInventory inventory, int invX, int invY) {
		this.addSlotToContainer(new Slot(inventory, 0, invX, invY));
	}
	
	
	
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return true;
	}

}
