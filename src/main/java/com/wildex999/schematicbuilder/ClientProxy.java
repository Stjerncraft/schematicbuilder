package com.wildex999.schematicbuilder;


import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class ClientProxy extends CommonProxy {
	
	@Override
	public void initialize() {
		MinecraftForge.EVENT_BUS.register(new WorldSchematicVisualizer());
		MinecraftForge.EVENT_BUS.register(new KeyHandler());
	}
}
