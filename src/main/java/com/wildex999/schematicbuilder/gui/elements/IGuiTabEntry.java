package com.wildex999.schematicbuilder.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public interface IGuiTabEntry {

	//Called when this tab is selected
	public void onTabActivated();
	
	//Called when another tab is selected
	public void onTabDeactivated();
	
	//Called whenever values in the Gui might have changed
	public void updateGui();
	
	//Called by parent to get the child tab GUI
	public GuiScreen getGui();
	
	public int getGuiLeft();
	public int getGuiTop();
	public int getGuiWidth();
	public int getGuiHeight();
	
	public String getTabName();
	
	public void setTabButton(GuiButtonCustom tab);
	public GuiButtonCustom getTabButton();
	
	public void setTabId(int id);
	public int getTabId();
	
	//From Minecraft Gui
	//public void setWorldAndResolution(Minecraft mc, int width, int height);
	public void drawGuiContainerBackgroundLayer(float par1, int mouseX, int mouseY);
	public void drawGuiContainerForegroundLayer(int mouseX, int mouseY);
	//public void updateScreen();
	//public void drawScreen(int p_73863_1_, int p_73863_2_, float p_73863_3_);
}
