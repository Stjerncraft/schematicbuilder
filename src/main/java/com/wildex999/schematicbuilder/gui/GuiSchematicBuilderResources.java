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
import com.wildex999.schematicbuilder.tiles.BuilderState;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;

import cpw.mods.fml.client.config.GuiButtonExt;

public class GuiSchematicBuilderResources extends GuiScreenExt implements IGuiTabEntry {
	
	public static final ResourceLocation backgroundImage = new ResourceLocation(ModSchematicBuilder.MODID, "textures/gui/schematic_builder_resources.png");
	
	private static final short backgroundWidth = 256;
	private static final short backgroundHeight = 256;
	
	private GuiLabel labelContainerName;
	private GuiLabel labelStatus;
	private GuiLabel labelStatusContent;
	private GuiLabel labelEnergy;
	
	private GuiList resourceList;
	private GuiButtonStretched buttonScrollbar;
	
	private GuiButtonItem buttonFloorItem;
	
	private GuiSchematicBuilder.GUI gui;
	
	private GuiButtonCustom tabButton;
	private int tabId;
	
	private GuiButton currentButton; 

	
	public GuiSchematicBuilderResources(GuiSchematicBuilder.GUI gui) {
		this.gui = gui;
		
		resourceList = new GuiList(this, 168, 5, 76, 244);
		resourceList.entryHeight = 26;
	}
	
	
	@Override
	public void onTabActivated() {
		//Set Floor
		if(gui.tile.config.floorBlock != null)
			buttonFloorItem.setItem(new ItemStack(gui.tile.config.floorBlock.getBlock(), 1, gui.tile.config.floorBlock.metaData));
		else
			buttonFloorItem.setItem(null);
		
		//Populate Resource List
		if(gui.tile.loadedSchematic != null && gui.tile.resources != null)
		{
			for(Entry<Short, ResourceItem> entry : gui.tile.resources.entrySet())
			{
				ResourceItem resource = entry.getValue();
				GuiListEntryResource listEntry;
				if(resource.getItem() != null)
					listEntry = new GuiListEntryResource(resource.getItem().getDisplayName()+"("+resource.getMeta()+")", null, resource);
				else
					listEntry = new GuiListEntryResource(resource.getBlock().getUnlocalizedName()+"("+resource.getMeta()+")", null, resource);
				resourceList.addEntry(listEntry);
			}
		}
	}
	
	@Override
	public void onTabDeactivated() {
		//TODO: Allow it to remain, but detect if new Schematic is loaded
		resourceList.clear();
	}
	
	@Override
	public void updateGui() {
		resourceList.update();
		
		updateEnergyLabel();
	}
	
	@Override
	public void updateScreen() {
	}
	
	public GuiScreen getGui() {
		return this;
	}
	
	@Override
	public void setWorldAndResolution(Minecraft mc, int width, int height) {
		super.setWorldAndResolution(mc, width, height);
		
		buttonList.clear();
		
		guiLeft = (width-backgroundWidth)/2;
		guiTop = (height-backgroundHeight)/2;
		
		resourceList.posX = guiLeft + 168;
		resourceList.posY = guiTop + 5;
		
		labelContainerName = new GuiLabel(gui.tile.getInventory().getInventoryName(), guiLeft + 5, guiTop + 5, gui.colorText);
		labelStatus = new GuiLabel("Status:", guiLeft + 5, guiTop + 15, gui.colorText);
		labelStatusContent = new GuiLabel("Idle", guiLeft + 43, guiTop + 15, gui.colorOk);
		labelEnergy = new GuiLabel("", guiLeft +5, guiTop + 25, gui.colorText);
		
		buttonFloorItem = new GuiButtonItem(0, guiLeft + 5, guiTop + 140, 140, 25, null);
		
		buttonScrollbar = new GuiButtonStretched(0, resourceList.posX + resourceList.width, resourceList.posY + resourceList.height, "");
		resourceList.setScrollbarButton(buttonScrollbar);
		
		buttonList.add(buttonScrollbar);
		buttonList.add(buttonFloorItem);
		
		updateGui();
		
	}
	

	@Override
    public void drawDefaultBackground()
    {
		return;
    }

	@Override
	public void drawGuiContainerBackgroundLayer(float par1, int mouseX, int mouseY) {
		this.mc.getTextureManager().bindTexture(backgroundImage);
		
		this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, backgroundWidth, backgroundHeight);
		resourceList.draw(mc);
		
		labelContainerName.draw(fontRendererObj);
		labelStatus.draw(fontRendererObj);
		labelStatusContent.draw(fontRendererObj);
		if(ModSchematicBuilder.useEnergy)
			labelEnergy.draw(fontRendererObj);
	}
	
	@Override
	public void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		

	}
	
	@Override
	public void mouseClicked(int x, int y, int event) {
		super.mouseClicked(x, y, event);
		
		if(x >= resourceList.posX && x <= resourceList.posX+resourceList.width
				&& y >= resourceList.posY && y <= resourceList.posY+resourceList.height)
			resourceList.onMouseClick(x, y);
	}
	
	@Override
	protected void keyTyped(char eventChar, int eventKey) {

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
		resourceList.onButtonDrag(currentButton, x, y);
	}
	
	@Override
	public void handleMouseInput() {
		super.handleMouseInput();
		
		//Scroll Wheel
		int scroll = Mouse.getDWheel();
		
		if(scroll != 0)
			resourceList.onScroll(scroll);
	}
	
	@Override
	public void actionPerformed(GuiButton button) {
		super.actionPerformed(button);

		currentButton = button;
		
		if(button == buttonScrollbar)
		{
			int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
	        int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
			resourceList.onButtonDragStart(button, x, y);
		}
	}
	
	

	@Override
	public int getGuiLeft() {
		return guiLeft;
	}

	@Override
	public int getGuiTop() {
		return guiTop;
	}

	@Override
	public int getGuiWidth() {
		return width;
	}

	@Override
	public int getGuiHeight() {
		return height;
	}
	
	@Override
	public String getTabName() {
		return "Resources";
	}


	@Override
	public void setTabButton(GuiButtonCustom tab) {
		tabButton = tab;
	}


	@Override
	public GuiButtonCustom getTabButton() {
		return tabButton;
	}


	@Override
	public void setTabId(int id) {
		tabId = id;
	}


	@Override
	public int getTabId() {
		return tabId;
	}
	
	public void updateEnergyLabel() {
		if(ModSchematicBuilder.useEnergy)
			labelEnergy.label = "Energy: " + gui.tile.energyStorage.getEnergyStored() + " / " + gui.tile.energyStorage.getMaxEnergyStored() + " RF";
	}

}
