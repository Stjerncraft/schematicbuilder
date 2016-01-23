package com.wildex999.schematicbuilder.gui.elements;

import com.wildex999.schematicbuilder.ResourceItem;
import com.wildex999.schematicbuilder.gui.GuiSchematicBuilder.GUI;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

/*
 * GUI Element showing a resource, with a Block and Item
 * 
 */

public class GuiResourceEntry extends Gui {
	
	private ResourceItem resource;
	private GuiButtonItem blockButton;
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
	
	public GuiResourceEntry(GuiScreen screen, int x, int y, GuiButtonItem blockButton, ResourceItem resource) {
		this.x = x;
		this.y = y;
		this.screen = screen;
		
		labelBlock = new GuiLabel("Block", x, y, GUI.colorText);
		labelItem = new GuiLabel("Item", x + 130, y, GUI.colorText);
		
		labelPlaced = new GuiLabel("", x + 32, y + 10, GUI.colorText);
		labelStored = new GuiLabel("", x + 32, y + 20, GUI.colorText);
		labelMissing = new GuiLabel("", x + 32, y + 30, GUI.colorText);
		
		
		blockButton.xPosition = x;
		blockButton.yPosition = y + 10;
		this.blockButton = blockButton;
		buttonPressed = false;
		
		setResource(resource);
	}
	
	public void setResource(ResourceItem resource) {
		this.resource = resource;
		
		if(resource != null)
			blockButton.setItem(resource.getItem(), false);
		else
			blockButton.setItem(null, false);
		
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
		labelBlock.draw(screen.mc.fontRenderer);
		labelItem.draw(screen.mc.fontRenderer);
		
		labelPlaced.draw(screen.mc.fontRenderer);
		labelMissing.draw(screen.mc.fontRenderer);
		labelStored.draw(screen.mc.fontRenderer);
	}
}
