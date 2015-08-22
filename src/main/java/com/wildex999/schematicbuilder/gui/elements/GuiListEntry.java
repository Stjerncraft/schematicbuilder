package com.wildex999.schematicbuilder.gui.elements;

import java.io.File;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

//Entry in a GuiList

public class GuiListEntry extends Gui {
	
	public String name;
	public List<String> tags;
	
	public Object value;
	public GuiList list;
	public int border = 1; //Space between two entries
	public boolean selected;
	
	public GuiListEntry(String name, List<String> tags) {
		this.name = name;
		this.tags = tags;
	}

	//Called when the entry is visible
	//public void onEntryVisible() {}
	
	//Called when the entry is no longer visible
	//public void onEntryInvisible() {}
	
	public void onSelect() {
		selected = true;
	}
	
	public void onUnselect() {
		selected = false;
	}
	
	//Draw the entry
	//x and y is the top left corner of the entry in screen coordinates.
	//width and height is the size given to the entry by the List.
	public void draw(Minecraft mc, int x, int y, int width, int height, boolean shade) {
	}
	
}
