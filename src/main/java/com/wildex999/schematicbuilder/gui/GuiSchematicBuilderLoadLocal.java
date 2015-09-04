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
import com.wildex999.schematicbuilder.SchematicWorldCache;
import com.wildex999.schematicbuilder.blocks.BlockLibrary;
import com.wildex999.schematicbuilder.config.StorageDirectories;
import com.wildex999.schematicbuilder.exceptions.ExceptionLoad;
import com.wildex999.schematicbuilder.gui.elements.GuiButtonCustom;
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

public class GuiSchematicBuilderLoadLocal extends GuiScreenExt implements IGuiTabEntry {
	
	public static final ResourceLocation backgroundImage = new ResourceLocation(ModSchematicBuilder.MODID, "textures/gui/schematic_builder_load_local.png");
	
	private static final short backgroundWidth = 256;
	private static final short backgroundHeight = 239;
	
	
	private static boolean searchCaseToggled = false;
	
	private GuiLabel labelContainerName;
	private GuiLabel labelCountFiltered;
	private GuiLabel labelSearch;
	private GuiLabel labelFilterTags;
	private GuiLabel labelMinScore;
	private GuiLabel labelSearchAuthor;
	private GuiLabel labelSizeMin;
	private GuiLabel labelSizeMax;
	
	private GuiTextField textFieldSearch;
	private GuiTextField textFieldFilterTags;
	private GuiTextField textFieldSearchAuthor;
	private GuiTextField textFieldSizeMinWidth;
	private GuiTextField textFieldSizeMinHeight;
	private GuiTextField textFieldSizeMinLength;
	private GuiTextField textFieldSizeMaxWidth;
	private GuiTextField textFieldSizeMaxHeight;
	private GuiTextField textFieldSizeMaxLength;
	
	private GuiList schematicList;
	private GuiButtonStretched buttonScrollbar;
	
	private GuiButton buttonLoad;
	private GuiButtonStretched buttonSearchCase;
	private GuiButtonStretched buttonSchematicsFolder;
	private GuiButtonStretched buttonRefresh;
	
	private GuiSchematicBuilder.GUI gui;
	
	private GuiButtonCustom tabButton;
	private int tabId;
	
	private GuiButton currentButton; 
	
	//Schematic files
	HashMap<String, GuiListEntry> schematicFiles;

	
	public GuiSchematicBuilderLoadLocal(GuiSchematicBuilder.GUI gui) {
		this.gui = gui;
		
		schematicList = new GuiList(this, 0, 0, 122, 207);
		schematicList.entryHeight = 48;
		schematicList.toggleEntries = false;
	}
	
	private void loadSchematics() {
		//Load files
		File loadFolder = StorageDirectories.getLoadFolderClient();
		File[] fileList = loadFolder.listFiles(new FilenameFilter() {
			public boolean accept(File directory, String fileName) {
				return fileName.endsWith(".schematic");
			}
		});
		
		//Check for missing files
		Iterator<Entry<String, GuiListEntry>> it = schematicFiles.entrySet().iterator();
		while(it.hasNext())
		{
			Entry<String, GuiListEntry> entry = it.next();
			String fileName = entry.getKey();
			//TODO: Use fileList to check instead to avoid hammering drive
			if(!new File(loadFolder, fileName).exists())
					it.remove();
		}
		
		//Check and load metadata for Schematics
		schematicList.noUpdate = true;
		for(File file : fileList)
		{
			System.out.println("Schematic File: " + file.getName());
			if(schematicFiles.containsKey(file.getName()))
				continue;
		
			Schematic schematic;
			
			try {
				schematic = SchematicLoader.loadSchematicMeta(file);
			} catch (IOException e) {
				System.err.println("IO Error while trying to load Schematics");
				e.printStackTrace();
				continue;
			} catch (ExceptionLoad e) {
				System.err.println("Loader Error while trying to load Schematics");
				e.printStackTrace();
				continue;
			}
			
			if(schematic.name.trim().length() == 0)
				schematic.name = file.getName().substring(0, file.getName().lastIndexOf('.'));
			
			GuiListEntry entry = new GuiListEntrySchematic(schematic.name, schematic.author, schematic.getWidth(), schematic.getHeight(), schematic.getLength(), null, file);
			schematicList.addEntry(entry);
			schematicFiles.put(file.getName(), entry);
		}
		
		schematicList.noUpdate = false;
		schematicList.update();
	}
	
	
	@Override
	public void onTabActivated() {
		if(schematicFiles == null)
		{
			schematicFiles = new HashMap<String, GuiListEntry>();
			loadSchematics();
		}
	}
	
	@Override
	public void onTabDeactivated() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void updateGui() {
		BuilderState state = gui.tile.state;
		
		buttonSearchCase.toggled = searchCaseToggled;
		labelCountFiltered.label = createLabelCountFiltered();
		
		if(gui.tile.canAcceptLoad())
			buttonLoad.enabled = true;
		else
			buttonLoad.enabled = false;

		schematicList.update();
	}
	
	@Override
	public void updateScreen() {
		textFieldSearch.updateCursorCounter();
		textFieldFilterTags.updateCursorCounter();
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
		
		schematicList.posX = guiLeft + 113;
		schematicList.posY = guiTop + 19;
		
		labelContainerName = new GuiLabel(gui.tile.getInventory().getInventoryName(), this.guiLeft + 10, this.guiTop + 5, gui.colorText);
		labelCountFiltered = new GuiLabel(createLabelCountFiltered(), guiLeft + 125, guiTop + 5, gui.colorText);
		labelSearch = new GuiLabel("Search", guiLeft + 5, guiTop + 20, gui.colorText);
		labelFilterTags = new GuiLabel("Filter Tags(t1,t2,...)", guiLeft + 5, guiTop + 45, gui.colorText);
		labelMinScore = new GuiLabel("Min Score", guiLeft + 5, guiTop + 70, gui.colorText);
		labelSearchAuthor = new GuiLabel("Search Author", guiLeft + 5, guiTop + 95, gui.colorText);
		labelSizeMin = new GuiLabel("Min(Width,Height,Len.)", guiLeft + 5, guiTop + 120, gui.colorText);
		labelSizeMax = new GuiLabel("Max(Width,Height,Len.)", guiLeft + 5, guiTop + 145, gui.colorText);
		
		textFieldSearch = new GuiTextField(fontRendererObj, guiLeft + 5, guiTop + 30, 100, 10);
		textFieldFilterTags = new GuiTextField(fontRendererObj, guiLeft + 5, guiTop + 55, 100, 10);
		
		buttonScrollbar = new GuiButtonStretched(0, schematicList.posX + schematicList.width, schematicList.posY + schematicList.height, "");
		schematicList.setScrollbarButton(buttonScrollbar);
		
		buttonLoad = new GuiButtonStretched(0, guiLeft+140, guiTop+226, 50, 10, "Load");
		buttonSearchCase = new GuiButtonStretched(0, guiLeft+87, guiTop+18, 18, 12, "Aa");
		buttonSchematicsFolder = new GuiButtonStretched(0, guiLeft + 5, guiTop + 205, 50, 20, "Folder");
		buttonRefresh = new GuiButtonStretched(0, guiLeft + 60, guiTop + 205, 50, 20, "Refresh");
		
		buttonList.add(buttonScrollbar);
		buttonList.add(buttonLoad);
		buttonList.add(buttonSearchCase);
		buttonList.add(buttonSchematicsFolder);
		buttonList.add(buttonRefresh);
		
		updateGui();
		
	}
	
	private String createLabelCountFiltered() {
		return "Schematics: " + schematicList.getListSizeFiltered() + " / " + schematicList.getListSize();
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
		schematicList.draw(mc);
		
		textFieldSearch.drawTextBox();
		textFieldFilterTags.drawTextBox();

		labelContainerName.draw(fontRendererObj);
		labelCountFiltered.draw(fontRendererObj);
		labelSearch.draw(fontRendererObj);
		labelFilterTags.draw(fontRendererObj);
		labelMinScore.draw(fontRendererObj);
		labelSearchAuthor.draw(fontRendererObj);
		labelSizeMin.draw(fontRendererObj);
		labelSizeMax.draw(fontRendererObj);
	}
	
	@Override
	public void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		
		ArrayList<String> textList = new ArrayList<String>();
		
		//Draw tooltips
		if(buttonSearchCase.isOver(mouseX, mouseY))
		{
			if(buttonSearchCase.toggled)
				textList.add("Search ignores Case");
			else
				textList.add("Search respects Case");
		}
		else if(buttonSchematicsFolder.isOver(mouseX, mouseY))
		{
			textList.add("Open folder that Schematics are loaded from.");
			textList.add(StorageDirectories.getLoadFolderClient().getAbsolutePath());
		}
		else if(buttonRefresh.isOver(mouseX, mouseY))
		{
			textList.add("Look for new Schematics in folder, and remove non-existent.");
		}
		
		drawHoveringText(textList, mouseX-guiLeft, mouseY-guiTop, fontRendererObj);
	}
	
	@Override
	public void mouseClicked(int x, int y, int event) {
		super.mouseClicked(x, y, event);
		
		textFieldSearch.mouseClicked(x, y, event);
		textFieldFilterTags.mouseClicked(x, y, event);
		
		if(x >= schematicList.posX && x <= schematicList.posX+schematicList.width
				&& y >= schematicList.posY && y <= schematicList.posY+schematicList.height)
			schematicList.onMouseClick(x, y);
	}
	
	@Override
	protected void keyTyped(char eventChar, int eventKey) {
		gui.ignoreKeyboardEvent = true;
		if(textFieldSearch.textboxKeyTyped(eventChar, eventKey))
		{
			schematicList.setSearchString(textFieldSearch.getText(), searchCaseToggled);
			updateGui();
			return;
		}
		else if(textFieldFilterTags.textboxKeyTyped(eventChar, eventKey))
		{
			//schematicList.setTagFilters();
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
		
		//Pass on to list for scrollbar
		schematicList.onButtonDrag(currentButton, x, y);
	}
	
	@Override
	public void handleMouseInput() {
		super.handleMouseInput();
		
		//Scroll Wheel
		int scroll = Mouse.getDWheel();
		
		if(scroll != 0)
			schematicList.onScroll(scroll);
	}
	
	@Override
	public void actionPerformed(GuiButton button) {
		super.actionPerformed(button);

		currentButton = button;
		
		if(button == buttonScrollbar)
		{
			int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
	        int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
			schematicList.onButtonDragStart(button, x, y);
		}
		else if(button == buttonSearchCase)
		{
			searchCaseToggled = !searchCaseToggled;
			buttonSearchCase.toggled = searchCaseToggled;
			if(textFieldSearch.getText().length() > 0)
				schematicList.setSearchString(textFieldSearch.getText(), searchCaseToggled);
		}
		else if(button == buttonSchematicsFolder)
		{
			try {
				Desktop.getDesktop().open(StorageDirectories.getLoadFolderClient());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if(button == buttonRefresh)
		{
			loadSchematics();
		}
		else if(button == buttonLoad)
		{
			if(schematicList.selectedEntry == null)
				return;
			gui.openTab(gui.tabIdMain);
			GuiListEntrySchematic entry = (GuiListEntrySchematic)schematicList.selectedEntry;
			gui.tile.loadSchematic(entry.file);
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
		return "Local Schematics";
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

}
