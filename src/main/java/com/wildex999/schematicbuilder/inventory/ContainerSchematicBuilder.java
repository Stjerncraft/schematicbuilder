package com.wildex999.schematicbuilder.inventory;

import com.wildex999.schematicbuilder.gui.IGuiWatchers;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerSchematicBuilder extends Container {

	protected static int slotsPerRow = 9;
	protected static int slotSize = 18;
	
	private int playerInventoryIndex = 0;
	private int playerInventoryX = 0;
	private int playerInventoryY = 0;
	
	private int inventoryIndex = 0;
	private int inventoryX = 0;
	private int inventoryY = 0;
	
	private TileSchematicBuilder tile;
	private InventoryPlayer playerInventory;
	
	public ContainerSchematicBuilder(InventoryPlayer playerInventory, TileSchematicBuilder tile) {
		super();
		this.tile = tile;
		this.playerInventory = playerInventory;
		
		initPlayerInventory(playerInventory, 6, 172);
		initContainerInventory(tile.getInventory(), 150, 130);
		
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
		
		this.playerInventoryIndex = this.inventorySlots.size();
		this.playerInventoryX = playerInventoryX;
		this.playerInventoryY = playerInventoryY;
		
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
		inventoryIndex = inventorySlots.size();
		this.addSlotToContainer(new SlotInput(inventory, tile.inventorySlotInput, 0, 0, tile)); //Input
		this.addSlotToContainer(new SlotOutput(inventory, tile.inventorySlotOutput, 0, 0)); //Output
		setInventoryPosition(invX, invY);
	}
	
	public void setPlayerInventoryPosition(int x, int y) {
		Slot slot;
		int offset = playerInventoryIndex;
		int i;
		
		playerInventoryX = x;
		playerInventoryY = y;
		
        for (i = 0; i < 3; ++i)
        {
            for (int j = 0; j < 9; ++j)
            {
            	slot = (Slot)this.inventorySlots.get(offset++);
            	slot.xDisplayPosition = x + j * 18;
            	slot.yDisplayPosition = y + i * 18;
            }
        }

        for (i = 0; i < 9; ++i)
        {
        	slot = (Slot)this.inventorySlots.get(offset++);
        	slot.xDisplayPosition = x + i * 18;
        	slot.yDisplayPosition = y + 58;
        }
	}
	
	public void setInventoryPosition(int x, int y) {
		Slot slot;
		int offset = inventoryIndex;
		
		inventoryX = x;
		inventoryY = y;
		
		//Input
		slot = (Slot)inventorySlots.get(offset++);
		slot.xDisplayPosition = x;
		slot.yDisplayPosition = y;
		
		//Output
		slot = (Slot)inventorySlots.get(offset++);
		slot.xDisplayPosition = x;
		slot.yDisplayPosition = y + 18;
	}
	
	public void hidePlayerInventory(boolean hide) {
		if(hide)
		{
			int oldX = playerInventoryX;
			int oldY = playerInventoryY;
			setPlayerInventoryPosition(-1000, -1000);
			//We want the stored X and Y to remain the same
			playerInventoryX = oldX;
			playerInventoryY = oldY;
		}
		else
			setPlayerInventoryPosition(playerInventoryX, playerInventoryY);
	}
	
	public void hideInventory(boolean hide) {
		if(hide)
		{
			int oldX = inventoryX;
			int oldY = inventoryY;
			setInventoryPosition(-1000, -1000);
			inventoryX = oldX;
			inventoryY = oldY;
		}
		else
			setInventoryPosition(inventoryX, inventoryY);
	}
	
	public int getPlayerInventoryX() {
		return playerInventoryX;
	}
	public int getPlayerInventoryY() {
		return playerInventoryY;
	}
	
	
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return true;
	}

}
