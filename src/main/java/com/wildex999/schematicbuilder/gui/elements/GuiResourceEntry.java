package com.wildex999.schematicbuilder.gui.elements;

import com.wildex999.schematicbuilder.ResourceItem;
import com.wildex999.schematicbuilder.gui.GuiSchematicBuilder.GUI;
import com.wildex999.schematicbuilder.inventory.SlotInfo;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/*
 * GUI Element showing a resource, with a Block and Item
 * 
 */

public class GuiResourceEntry extends Gui {
	
	private ResourceItem resource;
	private GuiButtonBlock blockButton;
	private SlotInfo slot;
	private int x, y;
	private GuiScreen screen;
	
	private GuiLabel labelBlock;
	private GuiLabel labelItem;
	
	private final String placedPrefix = "Placed: ";
	private GuiLabel labelPlaced;
	
	private final String missingPrefix = "Missing: ";
	private GuiLabel labelMissing;
	
	private final String storedPrefix = "Stored: ";
	private GuiLabel labelStored;
	
	private boolean buttonPressed;
	
	public GuiResourceEntry(GuiScreen screen, int x, int y, GuiButtonBlock blockButton, SlotInfo slot, ResourceItem resource) {
		this.x = x;
		this.y = y;
		this.screen = screen;
		
		labelBlock = new GuiLabel("Block", x, y, GUI.colorText);
		labelItem = new GuiLabel("Item", x + 130, y, GUI.colorText);
		
		labelPlaced = new GuiLabel("", x + 30, y + 10, GUI.colorText);
		labelStored = new GuiLabel("", x + 30, y + 20, GUI.colorText);
		labelMissing = new GuiLabel("", x + 30, y + 30, GUI.colorText);
		
		
		blockButton.xPosition = x;
		blockButton.yPosition = y + 10;
		this.blockButton = blockButton;
		this.slot = slot;
		buttonPressed = false;
		
		setResource(resource);
	}
	
	public void setResource(ResourceItem resource) {
		this.resource = resource;
		
		if(resource != null)
		{
			blockButton.setBlock(resource.getBlock(), resource.getMeta(), false);
			slot.putStack(resource.getItem());
		}
		else
		{
			blockButton.setBlock(null, (byte)0, false);
			slot.putStack(null);
		}
		
		update();
	}
	
	//Call whenever the Block button has been pressed
	public void buttonPressed() {
		buttonPressed = true;
	}
	
	public boolean buttonWasPressed() {
		if(buttonPressed) {
			buttonPressed = false;
			return true;
		}
		return false;
	}
	
	//Update the shown values
	public void update() {
		labelPlaced.label = placedPrefix + (resource != null ? resource.placedCount : "0");
		int missing = 0;
		if(resource != null) missing = resource.blockCount - resource.placedCount - resource.storedCount;
		labelMissing.label = missingPrefix + missing + "(" + (resource != null ? resource.getItemCostPerBlock() : "") + ")";
		labelStored.label = storedPrefix + (resource != null ? resource.storedCount : "0");
	}
	
	public void draw() {
		labelBlock.draw(screen.mc.fontRendererObj);
		labelItem.draw(screen.mc.fontRendererObj);
		
		labelPlaced.draw(screen.mc.fontRendererObj);
		labelMissing.draw(screen.mc.fontRendererObj);
		labelStored.draw(screen.mc.fontRendererObj);
	}
}
