package com.wildex999.schematicbuilder.gui.elements;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

//Button that Renders the Block

public class GuiButtonBlock extends GuiButtonStretched {
	private GuiBlockRenderer blockRender;
	private Block block;
	private byte meta;
	
	public GuiButtonBlock(int id, int x, int y,
			Block block, byte meta, boolean showName) {
		super(id, x, y, "");
		setBlock(block, meta, showName);
	}
	
	public GuiButtonBlock(int id, int x, int y,
			int width, int height, Block block, byte meta, boolean showName) {
		super(id, x, y, width, height, "");
		setBlock(block, meta, showName);
	}
	
	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY) {
		this.textOffsetX = 0;
		if(block != null)
			this.textOffsetX = 8;
		
		//Draw the button with an text offset
		super.drawButton(mc, mouseX, mouseY);
		
		//Draw the icon
		try {
			if(blockRender != null) {
				blockRender.setPosition(xPosition, yPosition);
				blockRender.setSize(width, height);
				blockRender.renderBlock();
			}
		} catch(Exception e) {} //Allow to fail without crashing
	}
	
	public Block getBlock() {
		return block;
	}
	
	public byte getMeta() {
		return meta;
	}
	
	public void setBlock(Block block, byte meta, boolean showName) {
		if(block != this.block || meta != this.meta) {
			this.block = block;
			this.meta = meta;
			
			if(block != null)
				blockRender = new GuiBlockRenderer(Minecraft.getMinecraft(), xPosition, yPosition, width, height, block, meta);
			else
				blockRender = null;
		}
		
		if(showName)
		{
			if(block != null)
			{
				//Try to get specific name for the given meta
				ItemStack blockItem = new ItemStack(block, meta);
				
				if(blockItem.getItem() == null)
					this.displayString = block.getLocalizedName();
				else
					this.displayString = blockItem.getDisplayName();
			}
			else
				this.displayString = "None";
		}
		else
			this.displayString = "";
	}
}
