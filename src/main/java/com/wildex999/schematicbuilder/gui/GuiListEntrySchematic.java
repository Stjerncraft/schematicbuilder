package com.wildex999.schematicbuilder.gui;

import java.io.File;
import java.util.List;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL11;

import com.wildex999.schematicbuilder.gui.elements.GuiListEntry;

public class GuiListEntrySchematic extends GuiListEntry {
	public String author;
	public int scWidth, scHeight, scLength;
	public File file;
	
	public GuiListEntrySchematic(String name, String author, int scWidth, int scHeight, int scLength, List<String> tags, File file) {
		super(name, tags);

		this.author = author;
		this.scWidth = scWidth;
		this.scHeight = scHeight;
		this.scLength = scLength;
		this.file = file;
	}
	
	@Override
	public void draw(Minecraft mc, int x, int y, int width, int height, boolean shade) {
		int color = 0xFF000000;
		int fontColor = 0xFFFFFF;
		
		if(selected)
			color = 0xFF3DBF1D;
		
		if(shade)
			fontColor = 0x808080;
		
		int scale = 2;
		
		GL11.glScalef(1f/scale, 1f/scale, 1);
		
		this.drawRect(scale*x, scale*y, scale*(x+width), scale*(y+height - border), color);
		this.drawRect(scale*x, scale*(y+height - border), scale*(x+width), scale*(y+height), 0xFFFFFFFF);
		
		//TODO: Actually draw preview
		this.drawRect(scale*(x+1), scale*(y+1), scale*(x+33), scale*(y+33), 0xFFDDDDDD);
		
		int fontHeight = mc.fontRenderer.FONT_HEIGHT;
		this.drawString(mc.fontRenderer, name, scale*(x + 34), scale*(y + 2), fontColor);
		this.drawString(mc.fontRenderer, "Author: " + author, scale*(x + 34), scale*(y + 2) + fontHeight, fontColor);
		this.drawString(mc.fontRenderer, "Width: " + scWidth + ", Height: " + scHeight + ", Length: " + scLength, scale*(x + 2), scale*(y + 34), fontColor);
		
		StringBuilder sb = new StringBuilder();
		if(tags != null)
		{
			for(String tag : tags)
				sb.append(tag + ", ");
		}
		
		this.drawString(mc.fontRenderer, "Tags: " + sb.toString(), scale*(x+2), scale*(y+34) + fontHeight, fontColor);
		
		GL11.glScalef(scale, scale, 1);
		
		if(shade)
			GL11.glColor4f(1f, 1f, 1f, 1f);
	}
}
