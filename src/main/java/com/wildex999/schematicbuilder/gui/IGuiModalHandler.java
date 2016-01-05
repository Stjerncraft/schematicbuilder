package com.wildex999.schematicbuilder.gui;

import net.minecraft.client.gui.GuiScreen;

//Allows opening a modal GUI on top, which will receive all input until closed

public interface IGuiModalHandler {
	public void showModal(IGuiModal gui);
	public void closeModal(); //The Modal GUI must call this when closing
}
