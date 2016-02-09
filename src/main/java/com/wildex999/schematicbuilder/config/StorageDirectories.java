package com.wildex999.schematicbuilder.config;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class StorageDirectories {
	public static File getSaveFolderServer() {
		File saveFolder = new File(DimensionManager.getCurrentSaveRootDirectory(), "schematics");
		if(!saveFolder.exists())
			saveFolder.mkdirs();
		return saveFolder;
	}
	
	@SideOnly(Side.CLIENT)
	public static File getSaveFolderClient() {
		File mcHome = (File)FMLInjectionData.data()[6]; //TODO: Find a cleaner way
		File saveFolder = new File(mcHome, "saves/SchematicBuilder/local"); //TODO: Per server local cache
		if(!saveFolder.exists())
			saveFolder.mkdirs();
		return saveFolder;
	}
	
	@SideOnly(Side.CLIENT)
	public static File getLoadFolderClient() {
		File mcHome = (File)FMLInjectionData.data()[6]; //TODO: Find a cleaner way
		File loadFolder = new File(mcHome, "schematics");
		if(!loadFolder.exists())
			loadFolder.mkdirs();
		return loadFolder;
	}
	
	@SideOnly(Side.CLIENT)
	public static File getCacheFolderClient() {
		File mcHome = (File)FMLInjectionData.data()[6]; //TODO: Find a cleaner way
		
		ServerData serverData = Minecraft.getMinecraft().getCurrentServerData();
		String server;
		if(serverData == null)
			server = "local";
		else
			server = serverData.serverIP;
		
		File cacheFolder = new File(mcHome, "schematicsCache/" + server);
		if(!cacheFolder.exists())
			cacheFolder.mkdirs();
		return cacheFolder;
	}
}
