package com.wildex999.schematicbuilder.gui.elements;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

public class GuiLabel extends Gui {

	public String label;
	public int x, y;
	public int color;
	
	public boolean centered;
	public boolean castShadow;
	
	public GuiLabel(String label, int x, int y, int color)
	{
		this.label = label;
		this.x = x;
		this.y = y;
		this.color = color;
	}
	
	public GuiLabel(String label, int x, int y)
	{
		this(label, x, y, 0xFFFFFF);
	}
	
	public GuiLabel(String label)
	{
		this(label, 0, 0);
	}

	public void draw(FontRenderer font)
	{
		int posX = x;
		
		if(this.centered)
			posX = (posX - font.getStringWidth(label)) / 2;

		font.drawString(label, posX, y, color, castShadow);
	}
}
