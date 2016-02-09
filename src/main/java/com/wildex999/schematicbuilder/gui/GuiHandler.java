package com.wildex999.schematicbuilder.gui;

import java.util.HashMap;
import java.util.Map;

import com.wildex999.utils.ModLog;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;


public class GuiHandler implements IGuiHandler {
	
	private static int GUI_ID_BASE = 1000; //Auto id allocation starting at 1000. Static id's < 1000.

	//Each GUI group handler registers itself to all the id's it handles
	private Map<Integer, IGuiHandler> customGuiHandlers;
	
	public GuiHandler() {
		customGuiHandlers = new HashMap<Integer, IGuiHandler>();
	}
	
	public void setGuiHandler(int id, IGuiHandler handler) {
		if(handler == null)
		{
			customGuiHandlers.remove(id);
			return;
		}
		
		customGuiHandlers.put(id, handler);
	}

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world,
			int x, int y, int z) {
		IGuiHandler handler = customGuiHandlers.get(ID);
		
		if(handler != null)
			return handler.getServerGuiElement(ID, player, world, x, y, z);
		
		ModLog.logger.warn("Missing GUI Handler for id: " + ID);
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world,
			int x, int y, int z) {
		IGuiHandler handler = customGuiHandlers.get(ID);
		TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
			
		if(handler != null)
			return handler.getClientGuiElement(ID, player, world, x, y, z);
		
		ModLog.logger.warn("Missing GUI Handler for id: " + ID);
		return null;
	}
	
	public static int getNextGuiID()
	{
		return GUI_ID_BASE++;
	}

}
