package com.wildex999.schematicbuilder;

import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {
	
	@Override
	public void initialize() {
		MinecraftForge.EVENT_BUS.register(new WorldSchematicVisualizer());
	}
}
