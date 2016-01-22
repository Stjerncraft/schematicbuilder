package com.wildex999.schematicbuilder.gui;

import java.awt.Desktop;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import com.wildex999.schematicbuilder.ResourceManager;
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

public class GuiSchematicBuilderResources extends GuiScreenExt implements IGuiTabEntry {
	
	public static final ResourceLocation backgroundImage = new ResourceLocation(ModSchematicBuilder.MODID, "textures/gui/schematic_builder_resources.png");
	
	private static final short backgroundWidth = 256;
	private static final short backgroundHeight = 256;
	
	private GuiLabel labelContainerName;
	private GuiLabel labelStatus;
	private GuiLabel labelStatusContent;
	private GuiLabel labelEnergy;
	private GuiLabel labelInput;
	private GuiLabel labelOutput;
	
	private int resourceVersion = 0; //Used to check when to reload Resources from tile
	private GuiList resourceList;
	private GuiButtonStretched buttonScrollbar;
	
	private GuiButton buttonAll; //Show all Resources
	private GuiButton buttonMissing; //Show Resources missing
	private GuiButton buttonUnknown; //Show Resources Unknown
	private GuiButton buttonBanned; //Show Resources Banned
	
	//Floor Item
	private GuiLabel labelFloor;
	private GuiLabel labelFloorMissing;
	private GuiLabel labelFloorStored;
	private GuiLabel labelFloorPlaced;
	
	private GuiButtonItem buttonFloorItem;
	private boolean selectingFloorItem; //True when currently selecting new Floor item
	
	private GuiSchematicBuilder.GUI gui;
	
	private GuiButtonCustom tabButton;
	private int tabId;
	
	private GuiButton currentButton; 

	
	public GuiSchematicBuilderResources(GuiSchematicBuilder.GUI gui) {
		this.gui = gui;
		
		resourceList = new GuiList(this, 168, 5, 76, 244);
		resourceList.entryHeight = 26;
		
		selectingFloorItem = false;
	}
	
	//Add resource to GUI List
	public void addResource(ResourceItem resource) {
		GuiListEntryResource listEntry = new GuiListEntryResource(getEntryName(resource), null, resource);
		resourceList.addEntry(listEntry);
	}
	
	//Remove invalid resources and add new ones
	public void updateResources() {
		if(gui.tile.loadedSchematic == null || gui.tile.resources == null)
		{
			reloadResources(); //Essentially clear the list
			return;
		}
		
		//Assumption: If a resource is removed by being invalid, a new one must be added in it's place.(Schematic block list is unchanging, only mapping changes)
		resourceList.noUpdate = true;
		
		//Get list of entries to replace(Avoid Concurrent modification)
		ArrayList<GuiListEntryResource> changeList = new ArrayList<GuiListEntryResource>();
		for(Map.Entry<String, GuiListEntry> entry : resourceList.getCurrentList().entrySet()) {
			String name = entry.getKey();
			GuiListEntryResource resourceEntry = (GuiListEntryResource)entry.getValue();
			
			if(resourceEntry.resource.valid)
				continue;
			
			changeList.add(resourceEntry);
		}
		
		//Modify List
		for(GuiListEntryResource resourceEntry : changeList) {
			//Remove old entry, and add new one for same Schematic Block ID & Meta, which should now have a new Resource
			resourceList.removeEntry(resourceEntry);
			ResourceItem newResource = ResourceManager.getResource(gui.tile.resources, resourceEntry.resource.getSchematicBlockId(), resourceEntry.resource.getSchematicMeta());
			if(newResource == null)
			{
				ModLog.printTileInfoPrefix(gui.tile);
				ModLog.logger.warn("Failed to get new Resource for Schematic Block: " + resourceEntry.resource.getSchematicBlockId() + ":" + resourceEntry.resource.getSchematicMeta()
									+ " while updating GUI Resource list.");
				continue;
			}
			addResource(newResource);
		}
		
		resourceList.noUpdate = false;
		resourceList.update();
	}
	
	public void reloadResources() {
		resourceList.clear();
		
		//Populate Resource List
		if(gui.tile.loadedSchematic != null && gui.tile.resources != null)
		{
			for(Entry<Short, ResourceItem> entry : gui.tile.resources.entrySet())
			{
				ResourceItem resource = entry.getValue();
				addResource(resource);
			}
		}
		
		resourceList.update();
	}
	
	public String getEntryName(ResourceItem resource) {
		String displayName = null;
		
		if(resource.isUnknown() || resource.isBanned())
		{
			SchematicMap map = gui.tile.loadedSchematic.getSchematicMap(resource.getSchematicBlockId(), resource.getSchematicMeta(), true);
			if(map == null)
				displayName = "Error: No mapping found!(" + resource.getSchematicBlockId() + ":" + resource.getSchematicMeta() + ")";
			else
			{
				displayName = map.schematicBlockName;
				if(displayName == null || displayName.isEmpty())
					displayName = map.schematicBlockId + ":" + map.schematicMeta;
			}
		}
		else if(resource.getBlock() == Blocks.air)
			displayName = "Air";
		else if(resource.getItem() != null)
			displayName = resource.getItem().getDisplayName()+"("+resource.getMeta()+")";
		else
			displayName = resource.getBlock().getUnlocalizedName()+"("+resource.getMeta()+")";
		
		return displayName;
	}
	
	@Override
	public void onTabActivated() {	
		//TODO: Only update instead of reload when changing tabs?
		reloadResources();
		
		gui.container.hidePlayerInventory(false);
		gui.container.hideInventory(false);
	}
	
	@Override
	public void onTabDeactivated() {
		gui.container.hidePlayerInventory(true);
		gui.container.hideInventory(true);
		//TODO: Allow it to remain, but detect if new Schematic is loaded
		resourceList.clear();
	}
	
	@Override
	public void updateGui() {
		if(gui.tile.resourceVersion == this.resourceVersion)
			updateResources();
		else
		{
			reloadResources();
			this.resourceVersion = gui.tile.resourceVersion;
		}
		
		if(selectingFloorItem)
		{
			selectingFloorItem = false;
			
			//Change item locally and send the change to server
			ItemStack newItem = GuiBlockSelector.selectedItem;
			if(newItem == null)
				gui.tile.config.floorBlock = null;
			else
				gui.tile.config.floorBlock = new SchematicBlock(Block.getBlockFromItem(newItem.getItem()), (byte) newItem.getItemDamage());
			gui.tile.sendConfigToServer();
		}
		//Set Floor
		if(gui.tile.config.floorBlock != null)
			buttonFloorItem.setItem(new ItemStack((Block) Block.blockRegistry.getObjectById(gui.tile.config.floorBlock.getSchematicBlockId()), 1, gui.tile.config.floorBlock.getSchematicMeta()), false);
		else
			buttonFloorItem.setItem(null, false);
		
		
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
		labelInput = new GuiLabel("Input:", guiLeft + 119, guiTop + 134, gui.colorText);
		labelOutput = new GuiLabel("Output:", guiLeft + 114, guiTop + 151, gui.colorText);
		
		buttonFloorItem = new GuiButtonItem(0, guiLeft + 5, guiTop + 120, 26, 25, buttonFloorItem != null ? buttonFloorItem.getItem() : null, false);
		
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
		
		labelInput.draw(fontRendererObj);
		labelOutput.draw(fontRendererObj);
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
		else if(button == buttonFloorItem) {
			GuiBlockSelector blockSelector = new GuiBlockSelector();
			gui.showModal(blockSelector);

			blockSelector.setSelected(buttonFloorItem.getItem());
			selectingFloorItem = true;
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
		return backgroundWidth;
	}

	@Override
	public int getGuiHeight() {
		return backgroundHeight;
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
