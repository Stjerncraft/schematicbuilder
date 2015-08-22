package com.wildex999.schematicbuilder;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {
	
	@Override
	public void initialize() {
		MinecraftForge.EVENT_BUS.register(new WorldSchematicVisualizer());
		FMLCommonHandler.instance().bus().register(new KeyHandler());
	}
}
