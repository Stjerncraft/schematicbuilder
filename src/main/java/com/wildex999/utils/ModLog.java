package com.wildex999.utils;

import net.minecraft.tileentity.TileEntity;

import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLLog;

public class ModLog {
	public static Logger logger;
	
	public static void init(Logger logger)
	{
		ModLog.logger = logger;
	}
	
	//Print TileEntity Name, World and Coordinates as a prefix for error message to follow.
	public static void printTileErrorPrefix(TileEntity tile) {
		printTileMessagePrefix(tile, "Error");
	}
	
	public static void printTileInfoPrefix(TileEntity tile) {
		printTileMessagePrefix(tile, "Info");
	}
	
	private static void printTileMessagePrefix(TileEntity tile, String messageType) {
		if(tile == null)
			return;
		String worldName = "Unknown";
		if(tile.getWorldObj() != null)
			worldName = tile.getWorldObj().provider.getDimensionName();
		System.err.println(messageType + " for TileEntity: " + tile + ", in World: " + worldName + "[(xyz) " + tile.xCoord + ", " + tile.yCoord + ", " + tile.zCoord + "]:");
	}
}
