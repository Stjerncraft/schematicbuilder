package com.wildex999.schematicbuilder.gui;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import com.wildex999.schematicbuilder.ResourceItem;
import com.wildex999.schematicbuilder.gui.elements.GuiBlockRenderer;
import com.wildex999.schematicbuilder.gui.elements.GuiListEntry;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;

public class GuiListEntryResource extends GuiListEntry {
	
	public ResourceItem resource;
	private GuiBlockRenderer blockRender;
	
	public static int colorBgNormal = 0xFF000000;
	public static int colorBgUnknown = 0xCBE02E00;
	public static int colorBgBanned = 0xAF2A2300;
	public static int colorBgSelected = 0xFF3DBF1D;
	
	public GuiListEntryResource(String name, String[] tags, ResourceItem resource) {
		super(name, tags);
		this.resource = resource;
		if(!resource.isUnknown())
		{
			blockRender = new GuiBlockRenderer(Minecraft.getMinecraft(), 0, 0, 0, 0, resource.getBlock(), resource.getMeta());
			blockRender.rotate = false;
		}
	}
	
	@Override
	public void draw(Minecraft mc, int x, int y, int width, int height, boolean shade) {
		int fontColor = 0xFFFFFF;
		
		int color;
		if(selected)
			color = colorBgSelected;
		else if(resource.isUnknown())
			color = colorBgUnknown;
		else if(resource.isBanned())
			color = colorBgBanned;
		else
			color = colorBgNormal;
		
		if(shade)
			fontColor = 0x808080;
		

		this.drawRect(x, y, (x+width), (y+height - border), color);
		this.drawRect(x, (y+height - border), (x+width), (y+height), 0xFFFFFFFF);
		
		if(blockRender != null) {
			blockRender.setPosition(x, y);
			blockRender.setSize(24, 24);
			blockRender.renderBlock();
		}
		
		int scale = 2;
		GL11.glScalef(1f/scale, 1f/scale, 1);
		
		int fontHeight = mc.fontRendererObj.FONT_HEIGHT;
		int offset = 24;
		
		this.drawString(mc.fontRendererObj, name, scale*(x + offset), scale*(y + 2), fontColor);
		this.drawString(mc.fontRendererObj, "Missing: " + (resource.blockCount - resource.storedCount - resource.placedCount) + "(" + resource.getItemCostPerBlock() + ")", scale*(x + offset), scale*(y+2)+fontHeight, fontColor);
		this.drawString(mc.fontRendererObj, "Stored: " + resource.storedCount, scale*(x + offset), scale*(y+2)+fontHeight*2, fontColor);
		this.drawString(mc.fontRendererObj, "Used  : " + resource.placedCount, scale*(x + offset), scale*(y+2)+fontHeight*3, fontColor);
		
		GL11.glScalef(scale, scale, 1);
		
		if(shade)
			GL11.glColor4f(1f, 1f, 1f, 1f);
	}

}
