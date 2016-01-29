package com.wildex999.schematicbuilder.gui;

import java.awt.Desktop;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.ChunkCache;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.ResourceEntry;
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
import com.wildex999.schematicbuilder.gui.elements.GuiResourceEntry;
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
	private GuiLabel labelFilter;
	
	private int resourceVersion = -1; //Used to check when to reload Resources from tile
	private GuiList resourceList;
	private GuiButtonStretched buttonScrollbar;
	
	private GuiListEntryResource resourceEntrySelected;
	
	private GuiButtonStretched buttonAll; //Show all Resources
	private GuiButtonStretched buttonMissing; //Show Resources missing
	private GuiButtonStretched buttonUnknown; //Show Resources Unknown
	private GuiButtonStretched buttonBanned; //Show Resources Banned
	
	private GuiTextField textFieldSearch;
	
	private String[] currentTags;
	private final String tagUnknown = "Unknown";
	private final String tagMissing = "Missing";
	private final String tagBanned = "Banned";
	private String[] tagsAll = {};
	private String[] tagsUnknown = {tagUnknown};
	private String[] tagsMissing = {tagMissing};
	private String[] tagsBanned = {tagBanned};
	
	//Floor Item
	private GuiLabel labelFloor;
	private GuiLabel labelFloorMissing;
	private GuiLabel labelFloorStored;
	private GuiLabel labelFloorPlaced;
	
	private GuiResourceEntry resourceFloor;
	private GuiResourceEntry resourceSelected;
	private GuiButtonItem buttonFloorResource;
	private GuiButtonItem buttonSelectedResource;
	
	private GuiSchematicBuilder.GUI gui;
	
	private GuiButtonCustom tabButton;
	private int tabId;
	
	private GuiButton currentButton; 

	private int resourceUpdateRate = 20;
	private int resourceUpdateCounter = 0;
	
	
	public GuiSchematicBuilderResources(GuiSchematicBuilder.GUI gui) {
		this.gui = gui;
		
		resourceList = new GuiList(this, 168, 5, 76, 244);
		resourceList.entryHeight = 26;
		resourceList.toggleEntries = false;
		
		currentTags = tagsAll;
	}
	
	//Add resource to GUI List
	public GuiListEntryResource addResource(ResourceItem resource) {
		GuiListEntryResource listEntry = new GuiListEntryResource(getEntryName(resource), findTags(resource), resource);
		
		//Add tags
		for(String tag : listEntry.tags)
			resourceList.addTag(listEntry, tag);
		
		if(resourceList.addEntry(listEntry))
			return listEntry;
		else
			return null;
	}
	
	//Find the tags for the given ResourceItem
	public String[] findTags(ResourceItem resource) {
		int missing = resource.blockCount - resource.placedCount - resource.storedCount;
		List<String> tagList = new ArrayList<String>();
		if(missing > 0)
			tagList.add(tagMissing);
		if(resource.isUnknown())
			tagList.add(tagUnknown);
		if(resource.isBanned())
			tagList.add(tagBanned);
		
		return tagList.toArray(new String[tagList.size()]);
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
		for(Map.Entry<String, GuiListEntry> entry : resourceList.getFullList().entrySet()) {
			String name = entry.getKey();
			GuiListEntryResource resourceEntry = (GuiListEntryResource)entry.getValue();

			if(resourceEntry.resource.valid)
			{
				//Update tags
				String[] newTags = findTags(resourceEntry.resource);
				if(!Arrays.equals(resourceEntry.tags, newTags)) {
					System.out.println("NEW TAGS FOR: " + resourceEntry.resource.getBlock().getLocalizedName());
					for(String tag : resourceEntry.tags)
						resourceList.removeTag(resourceEntry, tag);
					resourceEntry.tags = newTags;
					for(String tag : resourceEntry.tags)
						resourceList.addTag(resourceEntry, tag);
				}
				
				continue;
			}
			
			changeList.add(resourceEntry);
		}
		
		//Modify List
		for(GuiListEntryResource resourceEntry : changeList) {
			//Remove old entry, and add new one for same Schematic Block ID & Meta, which should now have a new Resource
			if(resourceEntrySelected == resourceEntry)
				resourceEntrySelected = null;
			
			boolean isSelected = false;
			if(resourceList.selectedEntry == resourceEntry) 
				isSelected = true;
			
			resourceList.removeEntry(resourceEntry);
			ResourceItem newResource = ResourceManager.getResource(gui.tile.resources, resourceEntry.resource.getSchematicBlockId(), resourceEntry.resource.getSchematicMeta());
			if(newResource == null)
			{
				ModLog.printTileInfoPrefix(gui.tile);
				ModLog.logger.warn("Failed to get new Resource for Schematic Block: " + resourceEntry.resource.getSchematicBlockId() + ":" + resourceEntry.resource.getSchematicMeta()
									+ " while updating GUI Resource list.");
				continue;
			}
			
			GuiListEntryResource newEntry = addResource(newResource);
			
			if(isSelected)
				resourceList.setSelectedEntry(newEntry);
		}
		
		resourceList.noUpdate = false;
		resourceList.update();
	}
	
	public void reloadResources() {
		if(this.resourceVersion == gui.tile.resourceVersion)
			return;

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
		this.resourceVersion = gui.tile.resourceVersion;
	}
	
	public String getEntryName(ResourceItem resource) {
		StringBuilder displayName = new StringBuilder();
		
		if(resource.isUnknown() || resource.isBanned())
		{
			SchematicMap map = gui.tile.loadedSchematic.getSchematicMap(resource.getSchematicBlockId(), resource.getSchematicMeta(), true);
			if(map == null)
				displayName.append("Error: No mapping found!(").append(resource.getSchematicBlockId()).append(":" + resource.getSchematicMeta() + ")");
			else
			{
				displayName.append(map.schematicBlockName);
				if(displayName.length() == 0)
					displayName.append(map.schematicBlockId + ":").append(map.schematicMeta);
			}
		}
		else if(resource.getBlock() == Blocks.air)
			displayName.append("Air");
		else if(resource.getItem() != null)
			displayName.append(resource.getItem().getDisplayName()).append("("+resource.getMeta()+")");
		else
			displayName.append(resource.getBlock().getUnlocalizedName()).append("("+resource.getMeta()+")");
		
		//Let each entry, even those sharing the same Entry, be unique in the list
		displayName.append("(").append(resource.getSchematicBlockId()).append(":").append(resource.getSchematicMeta()).append(")");
		
		return displayName.toString();
	}
	
	@Override
	public void onTabActivated() {	
		//TODO: Only update instead of reload when changing tabs?
		reloadResources();
		
		buttonSelectedResource.enabled = false;
		
		gui.container.hidePlayerInventory(false);
		gui.container.hideInventory(false);
		
	}
	
	@Override
	public void onTabDeactivated() {
		gui.container.hidePlayerInventory(true);
		gui.container.hideInventory(true);
		//TODO: Allow it to remain, but detect if new Schematic is loaded
		resourceList.clear();
		resourceVersion = -1;
	}
	
	@Override
	public void updateGui() {
		if(gui.tile.resourceVersion == this.resourceVersion)
			updateResources();
		else
			reloadResources();
		
		if(resourceFloor.buttonWasPressed())
		{
			//Change item locally and send the change to server
			ItemStack newItem = GuiBlockSelector.selectedItem;
			ResourceEntry entry;
			if(newItem != null)
				entry = ModSchematicBuilder.resourceManager.getOrCreate(Block.getBlockFromItem(newItem.getItem()), (byte)newItem.getItemDamage());
			else
				entry = ModSchematicBuilder.resourceManager.getOrCreate(Blocks.air, (byte)0);
			
			if(gui.tile.config.floorBlock.getEntry() != entry)
			{
				gui.tile.config.floorBlock = new ResourceItem((short)0, (byte)0, entry); //Send to server, which will calculate counts
				if(gui.tile.loadedSchematic != null)
					gui.tile.config.floorBlock.blockCount = gui.tile.loadedSchematic.getWidth() * gui.tile.loadedSchematic.getLength();
				
				gui.tile.sendConfigToServer();
			}
			
			resourceFloor.setResource(gui.tile.config.floorBlock);
		}
		if(resourceSelected.buttonWasPressed() && resourceList.selectedEntry != null) {
			ItemStack newItem = GuiBlockSelector.selectedItem;
			if(resourceEntrySelected != null && resourceEntrySelected.resource != null)
			{
				ResourceEntry entry;
				if(newItem != null)
					entry = ModSchematicBuilder.resourceManager.getOrCreate(Block.getBlockFromItem(newItem.getItem()), (byte)newItem.getItemDamage());
				else
					entry = ModSchematicBuilder.resourceManager.getOrCreate(Blocks.air, (byte)0);
				
				ResourceItem oldResource = resourceEntrySelected.resource;
				if(entry != null) {
					ResourceItem newResource = new ResourceItem(oldResource.getSchematicBlockId(), oldResource.getSchematicMeta(), entry);
					gui.tile.sendResourceSwapToServer(newResource);
					
					//Update client side, assuming it to go through. If not, the server will correct us
					/*newResource.blockCount = oldResource.blockCount;
					newResource.placedCount = oldResource.placedCount;
					newResource.storedCount = oldResource.storedCount; //This will be updated by server
					ResourceManager.removeResource(gui.tile.resources, gui.tile.resourcesBackMap, oldResource);
					ResourceManager.setResource(gui.tile.resources, gui.tile.resourcesBackMap, newResource);*/
				}
			}
		}
		
		//Set Floor
		if(gui.tile.config.floorBlock != null)
			buttonFloorResource.setItem(gui.tile.config.floorBlock.getItem(), false);
		else
			buttonFloorResource.setItem(null, false);
		
		resourceSelected.update();
		resourceFloor.update();
		updateEnergyLabel();
	}
	
	@Override
	public void updateScreen() {
		textFieldSearch.updateCursorCounter();
		
		//Check for new selection
		if(resourceEntrySelected== null || resourceList.selectedEntry != resourceEntrySelected) {
			resourceEntrySelected = (GuiListEntryResource) resourceList.selectedEntry;
			
			if(resourceEntrySelected != null)
			{
				resourceSelected.setResource(resourceEntrySelected.resource);
				buttonSelectedResource.enabled = true;
			}
			else
			{
				resourceSelected.setResource(null);
				buttonSelectedResource.enabled = false;
			}
		}
		
		//Update Resources and other things
		if(resourceUpdateCounter-- <= 0)
		{
			updateGui();
			resourceUpdateCounter = resourceUpdateRate;
		}
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
		
		buttonFloorResource = new GuiButtonItem(0, 0, 0, 26, 25, buttonFloorResource != null ? buttonFloorResource.getItem() : null, false);
		resourceFloor = new GuiResourceEntry(this, guiLeft + 5, guiTop + 80, buttonFloorResource, gui.tile.config.floorBlock);
		
		buttonSelectedResource = new GuiButtonItem(0, 0, 0, 26, 25, buttonSelectedResource != null ? buttonSelectedResource.getItem() : null, false);
		resourceSelected = new GuiResourceEntry(this, guiLeft + 5, guiTop + 30, buttonSelectedResource, resourceEntrySelected != null ? resourceEntrySelected.resource : null);
		
		buttonScrollbar = new GuiButtonStretched(0, resourceList.posX + resourceList.width, resourceList.posY + resourceList.height, "");
		resourceList.setScrollbarButton(buttonScrollbar);
		
		//Filtering
		buttonAll = new GuiButtonStretched(0, guiLeft + 5, guiTop + 143, 45, 12, "All");
		if(currentTags == tagsAll) buttonAll.toggled = true;
		buttonMissing = new GuiButtonStretched(0, guiLeft + 50, guiTop + 143, 45, 12, "Missing");
		if(currentTags == tagsMissing) buttonMissing.toggled = true;
		buttonUnknown = new GuiButtonStretched(0, guiLeft + 5, guiTop + 155, 45, 12, "Unknown");
		if(currentTags == tagsUnknown) buttonUnknown.toggled = true;
		buttonBanned = new GuiButtonStretched(0, guiLeft + 50, guiTop + 155, 45, 12, "Banned");
		if(currentTags == tagsBanned) buttonBanned.toggled = true;
		labelFilter = new GuiLabel("Filter", guiLeft + 5, guiTop + 120, gui.colorText);
		textFieldSearch = new GuiTextField(fontRendererObj, guiLeft + 5, guiTop + 130, 100, 10);
		textFieldSearch.setText(resourceList.prevSearch);
		
		buttonList.add(buttonScrollbar);
		buttonList.add(buttonFloorResource);
		buttonList.add(buttonSelectedResource);
		buttonList.add(buttonAll);
		buttonList.add(buttonMissing);
		buttonList.add(buttonUnknown);
		buttonList.add(buttonBanned);
		
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
		labelFilter.draw(fontRendererObj);
		
		textFieldSearch.drawTextBox();
		
		resourceFloor.draw();
		resourceSelected.draw();
	}
	
	@Override
	public void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		//Draw tooltips
		ArrayList<String> textList = new ArrayList<String>();
		
		if(buttonFloorResource.isOver(mouseX, mouseY))
			textList.add(buttonFloorResource.getItem() != null ? buttonFloorResource.getItem().getDisplayName() : "Air");
		if(buttonSelectedResource.isOver(mouseX, mouseY))
			textList.add(buttonSelectedResource.getItem() != null ? buttonSelectedResource.getItem().getDisplayName() : "Air");
		
		this.drawHoveringText(textList, mouseX-guiLeft, mouseY-guiTop, fontRendererObj);
	}
	
	@Override
	public void mouseClicked(int x, int y, int event) {
		super.mouseClicked(x, y, event);
		
		textFieldSearch.mouseClicked(x, y, event);
		
		if(x >= resourceList.posX && x <= resourceList.posX+resourceList.width
				&& y >= resourceList.posY && y <= resourceList.posY+resourceList.height)
			resourceList.onMouseClick(x, y);
	}
	
	@Override
	protected void keyTyped(char eventChar, int eventKey) {
		gui.ignoreKeyboardEvent = true;
		if(textFieldSearch.textboxKeyTyped(eventChar, eventKey))
		{
			resourceList.setSearchString(textFieldSearch.getText(), Arrays.asList(currentTags), true);
			updateGui();
			return;
		}
		gui.ignoreKeyboardEvent = false;
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
		
		textFieldSearch.mouseClicked(x, y, event);
		
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
		else if(button == buttonFloorResource || button == buttonSelectedResource) {
			GuiBlockSelector blockSelector = new GuiBlockSelector();
			gui.showModal(blockSelector);
			
			if(button == buttonFloorResource) {
				blockSelector.setSelected(buttonFloorResource.getItem());
				resourceFloor.buttonPressed();
			} else {
				blockSelector.setSelected(buttonSelectedResource.getItem());
				resourceSelected.buttonPressed();
			}
		} 
		else if(button == buttonAll || button == buttonMissing || button == buttonBanned || button == buttonUnknown) {
			setTags(button);
			resourceList.setSearchString(resourceList.prevSearch, Arrays.asList(currentTags), true);
		}
		
	}
	
	private void setTags(GuiButton button) {
		buttonAll.toggled = false;
		buttonMissing.toggled = false;
		buttonBanned.toggled = false;
		buttonUnknown.toggled = false;
		
		if(button == buttonAll)
		{
			buttonAll.toggled = true;
			currentTags = tagsAll;
		} else if(button == buttonMissing) {
			buttonMissing.toggled = true;
			currentTags = tagsMissing;
		} else if(button == buttonUnknown) {
			buttonUnknown.toggled = true;
			currentTags = tagsUnknown;
		} else if(button == buttonBanned) {
			buttonBanned.toggled = true;
			currentTags = tagsBanned;
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
