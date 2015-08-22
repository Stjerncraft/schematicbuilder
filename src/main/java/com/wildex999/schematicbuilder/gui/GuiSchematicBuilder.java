package com.wildex999.schematicbuilder.gui;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.jcraft.jorbis.Block;
import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.gui.elements.GuiButtonCustom;
import com.wildex999.schematicbuilder.gui.elements.GuiLabel;
import com.wildex999.schematicbuilder.gui.elements.IGuiTabEntry;
import com.wildex999.schematicbuilder.inventory.ContainerSchematicBuilder;
import com.wildex999.schematicbuilder.network.MessageActionSchematicBuilder;
import com.wildex999.schematicbuilder.network.MessageActionSchematicBuilder.ActionType;
import com.wildex999.schematicbuilder.network.MessageBase;
import com.wildex999.schematicbuilder.tiles.BuilderState;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.IGuiHandler;

public class GuiSchematicBuilder implements IGuiHandler
{
	public static final int GUI_ID = GuiHandler.getNextGuiID();
	
	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		TileSchematicBuilder inventory = (TileSchematicBuilder)world.getTileEntity(x, y, z);
		return new ContainerSchematicBuilder(player.inventory, inventory);
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world,int x, int y, int z) {
		TileEntity tile = world.getTileEntity(x, y, z);
		return new GUI(player, (TileSchematicBuilder)tile);
	}

	public static class GUI extends GuiContainer {
	
		public static final ResourceLocation textureTabs = new ResourceLocation(ModSchematicBuilder.MODID, "textures/gui/schematic_tabs.png");
		
		private static final short tabsWidth = 30;
		private static final short tabsWidthHidden = tabsWidth-3;
		private static final short tabsHeight = 33;
		
		public TileSchematicBuilder tile;
		public EntityPlayer player;
		public ContainerSchematicBuilder container;
		public boolean ignoreKeyboardEvent;
		
		public int colorText = 4210752;
		public int colorOk = 0x47D147;
		public int colorError = 0xFF6600;
		
		public int tabIdMain;
		public int tabIdLocal;
		public int tabIdResources;
		
		private GuiButtonCustom tabMain;
		private GuiButtonCustom tabLocal;
		private GuiButtonCustom tabResources;
		
		//Tabs
		private ArrayList<IGuiTabEntry> tabs = new ArrayList<IGuiTabEntry>();
		private int currentTab = 0;
	
		
		/*
		 * IDEA:
		 * Show status on top and some options.
		 * Bottom have a sortable and searchable list of all resources in schematic(And how much is missing)
		 * 
		 * When pressing 'load schematic' there will be a GUI with tabs on top for 
		 * 'local', 'Server' and 'schematicbuilder.com'.
		 * 
		 * Content of tabs are a list of schematics with sorting and searching options.
		 * Item Windows on main Tab for inserting local Schematic Item(Containing Schematic)
		 * 
		 */
		
		public GUI(EntityPlayer player, TileSchematicBuilder tile) {
			//Slot positions set in Container
			super(new ContainerSchematicBuilder(player.inventory, tile));
			this.tile = tile;
			this.player = player;
			
			if(tile == null)
			{
				player.closeScreen();
				return;
			}
			if(!(inventorySlots instanceof ContainerSchematicBuilder))
			{
				player.closeScreen();
				return;
			}
			container = (ContainerSchematicBuilder)inventorySlots;
			container.hidePlayerInventory(true);
			
			tabIdMain = addTab(new GuiSchematicBuilderMain(this));
			tabIdLocal = addTab(new GuiSchematicBuilderLoadLocal(this));
			tabIdResources = addTab(new GuiSchematicBuilderResources(this));
		}
		
		public int addTab(IGuiTabEntry tab) {
			int tabId = tabs.size();
			tabs.add(tab);
			tab.setTabId(tabId);
			
			return tabId;
		}
		
		public GuiButtonCustom addTabButton(int posY, int texX, int texY, int tabId) {
			GuiButtonCustom button = new GuiButtonCustom(0, this.guiLeft-(tabsWidthHidden), this.guiTop + posY, 
					texX, texY, textureTabs, tabsWidthHidden, tabsHeight, null);
			button.hoverOffsetX = tabsWidth;
			tabs.get(tabId).setTabButton(button);
			button.data = tabId;
			
			buttonList.add(button);
			
			return button;
		}
		
		public void openTab(int tabId) {
			tabs.get(currentTab).onTabDeactivated();
			currentTab = tabId;
			setWorldAndResolution(mc, width, height);
			tabs.get(currentTab).onTabActivated();
			
		}
		
		@Override
		public void setWorldAndResolution(Minecraft mc, int width, int height) {
			super.setWorldAndResolution(mc, width, height);
			
			IGuiTabEntry tab = tabs.get(currentTab);
			
			tab.getGui().setWorldAndResolution(mc, width, height);
			
			guiLeft = tab.getGuiLeft();
			guiTop = tab.getGuiTop();
			width = tab.getGuiWidth() + tabsWidth;
			height = tab.getGuiHeight();
			
			buttonList.clear();
			
			tabMain = addTabButton(8, 0, 0, tabIdMain);
			tabResources = addTabButton(38, 0, 0, tabIdResources); //Resources
			
			tabLocal = addTabButton(100, 0, 0, tabIdLocal); //Load Local
			//addTabButton(130, 0, 0, tabIdTest); //Load Server
			//addTabButton(160, 0, 0, tabIdTest); //Load Online(schematicbuilder.com)
			
			updateGui();
		}
	
		@Override
		public void updateScreen() {
			super.updateScreen();
			
			tabs.get(currentTab).getGui().updateScreen();
		}
		
		@Override
		public void drawGuiContainerBackgroundLayer(float par1, int mouseX, int mouseY) {
			IGuiTabEntry tab = tabs.get(currentTab);
			tab.drawGuiContainerBackgroundLayer(par1, mouseX, mouseY);
			
			for(IGuiTabEntry tabEntry : tabs)
			{
				GuiButtonCustom tabButton = tabEntry.getTabButton();
				if(tabEntry == tab)
					tabButton.width = tabButton.texWidth = tabsWidth;
				else
					tabButton.width = tabButton.texWidth = tabsWidthHidden;
			}
			
			//drawScreen is between background and foreground
			tabs.get(currentTab).getGui().drawScreen(mouseX, mouseY, par1);
		}
		
		@Override
		public void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
			tabs.get(currentTab).drawGuiContainerForegroundLayer(mouseX, mouseY);
			List text = new ArrayList();
			
			//Draw hover text for Tabs
			for(IGuiTabEntry tabEntry : tabs) 
			{
				if(tabEntry.getTabButton().isOver(mouseX, mouseY))
					text.add(tabEntry.getTabName());
			}
			
			this.drawHoveringText(text, mouseX-guiLeft, mouseY-guiTop, fontRendererObj);
		}
		
		@Override
		public void drawScreen(int p_73863_1_, int p_73863_2_, float p_73863_3_) {
			super.drawScreen(p_73863_1_, p_73863_2_, p_73863_3_);
		}
		
		@Override
	    public void drawDefaultBackground()
	    {
			return;
	    }
	
		@Override
		public boolean doesGuiPauseGame() {
			return false;
		}
		
		@Override
		public void handleMouseInput() {
			super.handleMouseInput();
			tabs.get(currentTab).getGui().handleMouseInput();
		}
		
		@Override 
		public void handleKeyboardInput() {
			tabs.get(currentTab).getGui().handleKeyboardInput();
			if(!ignoreKeyboardEvent) //Allow for tabs to capture keyboard
				super.handleKeyboardInput();
			ignoreKeyboardEvent = false;
		}
		
		@Override
		protected void actionPerformed(GuiButton button) {
			IGuiTabEntry tab = tabs.get(currentTab);
			
			if(tab.getTabButton() == button || !(button instanceof GuiButtonCustom))
				return;
			GuiButtonCustom tabButton = (GuiButtonCustom)button;
			
			if(tabButton.data < 0 || tabButton.data >= tabs.size())
				return;
			
			openTab(tabButton.data);
		}
		
		//Update the GUI based on state
		public void updateGui() {
			tabs.get(currentTab).updateGui();
		}
		
	}
}