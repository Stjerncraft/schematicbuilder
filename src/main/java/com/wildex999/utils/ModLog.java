package com.wildex999.utils;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;

import org.apache.logging.log4j.Logger;

public class ModLog {
	public static Logger logger;
	
	public static void init(Logger logger)
	{
		ModLog.logger = logger;
	}
	
	//Print TileEntity Name, World and Coordinates as a prefix for error message to follow.
	public static void printTileErrorPrefix(TileEntity tile) {
		logger.error(getTileMessagePrefix(tile, "Error"));
	}
	
	public static void printTileInfoPrefix(TileEntity tile) {
		logger.info(getTileMessagePrefix(tile, "Info"));
	}
	
	private static String getTileMessagePrefix(TileEntity tile, String messageType) {
		if(tile == null)
			return "Unknown Tile(null)";
		String worldName = "Unknown";
		if(tile.getWorld() != null)
			worldName = tile.getWorld().provider.getDimensionName();
		BlockPos pos = tile.getPos();
		return (messageType + " for TileEntity: " + tile + ", in World: " + worldName + "[(xyz) " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]:");
	}
}
