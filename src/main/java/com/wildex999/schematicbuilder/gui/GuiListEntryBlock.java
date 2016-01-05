package com.wildex999.schematicbuilder.gui;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
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
import com.wildex999.schematicbuilder.gui.elements.GuiListEntry;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;

import cpw.mods.fml.common.registry.GameData;

public class GuiListEntryBlock extends GuiListEntry {
	
	ItemStack item;
	
	public static int colorBgNormal = 0xFF000000;
	public static int colorBgSelected = 0xFF3DBF1D;
	
	public GuiListEntryBlock(String name, List<String> tags, ItemStack item) {
		super(name, tags);
		this.item = item;
	}
	
	@Override
	public void draw(Minecraft mc, int x, int y, int width, int height, boolean shade) {
		int fontColor = 0xFFFFFF;
		
		int color;
		if(selected)
			color = colorBgSelected;
		else
			color = colorBgNormal;
		
		if(shade)
			fontColor = 0x808080;
		

		this.drawRect(x, y, (x+width), (y+height - border), color);
		this.drawRect(x, (y+height - border), (x+width), (y+height), 0xFFFFFFFF);
		
		//Draw item
		try {
			if(item != null && item.getItem() != null)
			{
				RenderItem.getInstance().renderItemIntoGUI(mc.fontRenderer, mc.getTextureManager(), item, (x+5), (y+5));
				RenderHelper.disableStandardItemLighting();
			}
		} catch(Exception e) {
			//Don't allow rendering to crash due to incorrect data, as that will happen often without
			//a way for us to stop it(Old block metadata etc.)
		}
		
		int scale = 2;
		GL11.glScalef(1f/scale, 1f/scale, 1);
		
		int fontHeight = mc.fontRenderer.FONT_HEIGHT;
		int offset = 24;
		
		this.drawString(mc.fontRenderer, name, scale*(x + offset), scale*(y + 2), fontColor);
		//this.drawString(mc.fontRenderer, "Missing: " + (resource.blockCount - resource.storedItemCount - resource.placedCount), scale*(x + offset), scale*(y+2)+fontHeight, fontColor);
		//this.drawString(mc.fontRenderer, "Stored: " + resource.storedItemCount, scale*(x + offset), scale*(y+2)+fontHeight*2, fontColor);
		//this.drawString(mc.fontRenderer, "Used  : " + resource.placedCount, scale*(x + offset), scale*(y+2)+fontHeight*3, fontColor);
		
		GL11.glScalef(scale, scale, 1);
		
		if(shade)
			GL11.glColor4f(1f, 1f, 1f, 1f);
	}

}

