package com.wildex999.schematicbuilder.gui;

import java.awt.Desktop;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.glu.Project;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.ChunkCache;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.ResourceItem;
import com.wildex999.schematicbuilder.SchematicWorldCache;
import com.wildex999.schematicbuilder.blocks.BlockLibrary;
import com.wildex999.schematicbuilder.exceptions.ExceptionLoad;
import com.wildex999.schematicbuilder.gui.elements.GuiButtonCustom;
import com.wildex999.schematicbuilder.gui.elements.GuiButtonItem;
import com.wildex999.schematicbuilder.gui.elements.GuiButtonStretched;
import com.wildex999.schematicbuilder.gui.elements.GuiLabel;
import com.wildex999.schematicbuilder.gui.elements.GuiList;
import com.wildex999.schematicbuilder.gui.elements.GuiListEntry;
import com.wildex999.schematicbuilder.gui.elements.GuiScreenExt;
import com.wildex999.schematicbuilder.gui.elements.IGuiTabEntry;
import com.wildex999.schematicbuilder.network.MessageActionSchematicBuilder;
import com.wildex999.schematicbuilder.network.MessageBase;
import com.wildex999.schematicbuilder.network.MessageActionSchematicBuilder.ActionType;
import com.wildex999.schematicbuilder.schematic.Schematic;
import com.wildex999.schematicbuilder.schematic.SchematicBlock;
import com.wildex999.schematicbuilder.schematic.SchematicLoader;
import com.wildex999.schematicbuilder.schematic.SchematicMap;
import com.wildex999.schematicbuilder.tiles.BuilderState;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;
import com.wildex999.utils.ModLog;

import cpw.mods.fml.client.config.GuiButtonExt;
import cpw.mods.fml.common.registry.GameData;


public class GuiBlockSelector extends GuiScreenExt implements IGuiModal {
	
	public static final ResourceLocation backgroundImage = new ResourceLocation(ModSchematicBuilder.MODID, "textures/gui/block_selector.png");
	
	private static final short backgroundWidth = 256;
	private static final short backgroundHeight = 239;
	
	private IGuiModalHandler modalHandler;
	
	public static ItemStack selectedItem;
	private GuiList blockList;
	private GuiButtonStretched buttonScrollbar;
	
	private GuiButton currentButton;
	private GuiButton buttonSelect;
	
	private GuiLabel labelFilter;
	private GuiTextField textFieldSearch;
	
	public GuiBlockSelector() {
		blockList = new GuiList(this, 0, 0, 230, 211);
		blockList.entryHeight = 26;
		blockList.toggleEntries = false;
	}
	
	public void setSelected(ItemStack item) {
		selectedItem = item;
		
		if(item == null)
			return;
		
		//Update List to show this as selected
		GuiListEntry entry = blockList.getEntry(item.getDisplayName() + "(" + item.getItemDamage() + ")");
		blockList.setSelectedEntry(entry);
		blockList.scrollToSelected();
	}
	
	@Override
	public void updateScreen() {
	}
	
	@Override
	public GuiScreen getGui() {
		return this;
	}
	
	@Override
	public void setWorldAndResolution(Minecraft mc, int width, int height) {
		super.setWorldAndResolution(mc, width, height);
		
		buttonList.clear();
		
		guiLeft = (width-backgroundWidth)/2;
		guiTop = (height-backgroundHeight)/2;
		
		blockList.posX = guiLeft + 6;
		blockList.posY = guiTop + 15;
		
		buttonSelect = new GuiButtonStretched(0, guiLeft + 7, guiTop + 227, 50, 10, "Select");
		
		buttonScrollbar = new GuiButtonStretched(0, blockList.posX + blockList.width, blockList.posY + blockList.height, "");
		blockList.setScrollbarButton(buttonScrollbar);
		
		labelFilter = new GuiLabel("Filter:", guiLeft + 7, guiTop + 5, GuiSchematicBuilder.GUI.colorText);
		textFieldSearch = new GuiTextField(fontRendererObj, guiLeft + 50, guiTop + 4, 100, 10);
		
		buttonList.add(buttonScrollbar);
		buttonList.add(buttonSelect);
		
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float par1) {
		this.mc.getTextureManager().bindTexture(backgroundImage);	
		this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, backgroundWidth, backgroundHeight);
		
		blockList.draw(mc);
		textFieldSearch.drawTextBox();
		labelFilter.draw(fontRendererObj);
		
		//Draw Labels and Buttons
		super.drawScreen(mouseX, mouseY, par1);
	}
	
	@Override
	public void mouseClicked(int x, int y, int event) {
		super.mouseClicked(x, y, event);
		
		textFieldSearch.mouseClicked(x, y, event);
		
		if(x >= blockList.posX && x <= blockList.posX+blockList.width
				&& y >= blockList.posY && y <= blockList.posY+blockList.height)
			blockList.onMouseClick(x, y);
	}
	
	@Override
	protected void keyTyped(char eventChar, int eventKey) {
		if(eventKey == 1) //1 == ESC, close modal
			modalHandler.closeModal();
		
		if(textFieldSearch.textboxKeyTyped(eventChar, eventKey))
		{
			blockList.setSearchString(textFieldSearch.getText(), null, true);
			blockList.update();
			return;
		}
	}
	
	@Override
	protected void mouseMovedOrUp(int x, int y, int event) {
		super.mouseMovedOrUp(x, y, event);

		if(event == 0) //Mouse Up
			currentButton = null;
	}
	
	@Override
	protected void mouseClickMove(int x, int y,
			int event, long time) {
		super.mouseClickMove(x, y, event, time);
		
		//Pass on to list for scrollbar
		blockList.onButtonDrag(currentButton, x, y);
	}
	
	@Override
	public void handleMouseInput() {
		super.handleMouseInput();
		
		//Scroll Wheel
		int scroll = Mouse.getDWheel();
		
		if(scroll != 0)
			blockList.onScroll(scroll);
		
	}
	
	@Override
	public void actionPerformed(GuiButton button) {
		super.actionPerformed(button);

		currentButton = button;
		
		if(button == buttonScrollbar)
		{
			int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
	        int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
	        blockList.onButtonDragStart(button, x, y);
		} 
		else if(button == buttonSelect) 
		{
			GuiListEntry entry = blockList.selectedEntry;
			if(entry != null)
			{
				GuiListEntryBlock blockEntry = (GuiListEntryBlock)entry;
				selectedItem = blockEntry.item;
			}
			modalHandler.closeModal();
			return;
		}
	}
	
	@Override
	public boolean doesGuiPauseGame() {
	    return false;
	}

	@Override
	public void onOpen(IGuiModalHandler handler, Minecraft mc, int width, int height) {
		this.modalHandler = handler;
		this.setWorldAndResolution(mc, width, height);
		
		//Populate Blocks list
		blockList.noUpdate = true;
		for(Block block : GameData.getBlockRegistry().typeSafeIterable()) {
			ItemStack item = new ItemStack(block);
			if(item.getItem() == null)
				continue;
			
			List subList = new ArrayList();
			block.getSubBlocks(item.getItem(), null, subList);
			
			for(Object entry : subList)
			{
				if(entry instanceof ItemStack)
				{
					ItemStack entryItem = (ItemStack)entry;
					blockList.addEntry(new GuiListEntryBlock(entryItem.getDisplayName() + "(" + entryItem.getItemDamage() + ")", null, entryItem));
				}
			}
		}
		blockList.noUpdate = false;
		blockList.update();
	}

	@Override
	public void onClose() {
	}

}
