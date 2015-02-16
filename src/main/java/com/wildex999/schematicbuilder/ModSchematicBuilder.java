package com.wildex999.schematicbuilder;

import com.wildex999.schematicbuilder.blocks.BlockLibrary;
import com.wildex999.schematicbuilder.gui.GuiHandler;
import com.wildex999.schematicbuilder.gui.SchematicBuilderGui;
import com.wildex999.schematicbuilder.network.Networking;
import com.wildex999.utils.ModLog;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;

@Mod(modid = ModSchematicBuilder.MODID, version = ModSchematicBuilder.VERSION)
public class ModSchematicBuilder {
	public static final String MODID = "schematicbuilder";
	public static final String VERSION = "0.2.0";
	public static ModSchematicBuilder instance;
	
	public GuiHandler guiHandler;
	
    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
    	ModLog.init(event.getModLog());
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    	this.instance = this;
    	
    	Networking.init();
    	BlockLibrary.init();
    	
    	//Init GUI
    	guiHandler = new GuiHandler();
    	NetworkRegistry.INSTANCE.registerGuiHandler(this, guiHandler);
    	guiHandler.setGuiHandler(SchematicBuilderGui.GUI_ID, new SchematicBuilderGui());
    	
    	//Events
    	FMLCommonHandler.instance().bus().register(new TickHandler());
    	
    	ModLog.logger.info("Schematic Builder initialized!");
    }
}
