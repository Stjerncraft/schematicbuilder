package com.wildex999.schematicbuilder.gui.elements;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

//Button that Renders the icon and name of an itemStack

public class GuiButtonItem extends GuiButtonStretched {
	private ItemStack item;
	
	public GuiButtonItem(int id, int x, int y,
			ItemStack item, boolean showName) {
		super(id, x, y, "");
		setItem(item, showName);
	}
	
	public GuiButtonItem(int id, int x, int y,
			int width, int height, ItemStack item, boolean showName) {
		super(id, x, y, width, height, "");
		setItem(item, showName);
	}
	
	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY) {
		this.textOffsetX = 0;
		if(item != null && item.getItem() != null)
			this.textOffsetX = 8;
		
		//Draw the button with an text offset
		super.drawButton(mc, mouseX, mouseY);
		
		//Draw the icon
		try {
		if(item != null && item.getItem() != null)
			mc.getRenderItem().renderItemIntoGUI(item, xPosition + 5, yPosition + (height/2) - 8);
		} catch(Exception e) {} //Allow to fail without crashing
	}
	
	public ItemStack getItem() {
		return item;
	}
	
	public void setItem(ItemStack item, boolean showName) {
		this.item = item;
		if(showName)
		{
			if(item != null && item.getItem() != null)
				this.displayString = item.getDisplayName();
			else
				this.displayString = "None";
		}
		else
			this.displayString = "";
	}
}
