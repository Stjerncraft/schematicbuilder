package com.wildex999.schematicbuilder.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public interface IGuiModal {
	public void onOpen(IGuiModalHandler handler, Minecraft mc, int width, int height); //Called when opened by a handler
	public void onClose(); //Called when closed
	public GuiScreen getGui();
}
