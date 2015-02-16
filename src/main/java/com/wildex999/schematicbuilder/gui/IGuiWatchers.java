package com.wildex999.schematicbuilder.gui;

import net.minecraft.entity.player.EntityPlayer;

//Implement if players can watch for GUI updates

public interface IGuiWatchers {
	public void addWatcher(EntityPlayer player);
	public void removeWatcher(EntityPlayer player);
}

