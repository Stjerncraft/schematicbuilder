package com.wildex999.schematicbuilder;

import java.io.File;

import com.wildex999.schematicbuilder.blocks.BlockLibrary;
import com.wildex999.schematicbuilder.config.ConfigurationManager;
import com.wildex999.schematicbuilder.config.ConfigurationManagerGeneral;
import com.wildex999.schematicbuilder.config.IConfigListener;
import com.wildex999.schematicbuilder.gui.GuiHandler;
import com.wildex999.schematicbuilder.gui.GuiSchematicBuilder;
import com.wildex999.schematicbuilder.items.ItemLibrary;
import com.wildex999.schematicbuilder.network.MessageUploadSchematic;
import com.wildex999.schematicbuilder.network.Networking;
import com.wildex999.schematicbuilder.schematic.SchematicLoader;
import com.wildex999.schematicbuilder.schematic.SchematicLoaderService;
import com.wildex999.utils.ModLog;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(modid = ModSchematicBuilder.MODID, version = ModSchematicBuilder.VERSION, dependencies = "after:CoFHAPI")
public class ModSchematicBuilder implements IConfigListener{
	public static final String MODID = "schematicbuilder";
	public static final String VERSION = "0.2.5";
	public static ModSchematicBuilder instance;
	
	public static ConfigurationManagerGeneral configGeneral;
	public static boolean debug = true;
	
	public static ResourceManager resourceManager;
	
	@SidedProxy(clientSide = "com.wildex999.schematicbuilder.ClientProxy", serverSide = "com.wildex999.schematicbuilder.CommonProxy")
	public static CommonProxy proxy;
	
	public static SchematicLoaderService schematicLoaderService;
	public static PlayerManager playerManager;
	
	public GuiHandler guiHandler;
	public static boolean useEnergy;
	
    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
    	ModLog.init(event.getModLog());
    	
    	//Load Config
    	configGeneral = new ConfigurationManagerGeneral(new File(event.getModConfigurationDirectory(), MODID+".cfg"));
    	configGeneral.loadConfig();
    	configGeneral.addConfigListener(this);
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    	this.instance = this;
    	
    	Networking.init();
    	BlockLibrary.init();
    	ItemLibrary.init();
    	proxy.initialize();
    	
    	onConfigReload(configGeneral);
    	
    	if(configGeneral.loaderThreadCount < 1)
    		configGeneral.loaderThreadCount = 1;
    	schematicLoaderService = new SchematicLoaderService(configGeneral.loaderThreadCount);
    	playerManager = new PlayerManager();
    	
    	ModCheck modCheckCoFHAPI = new ModCheckCoFHAPI();
    	if(configGeneral.energyEnabled)
    	{
	    	if(modCheckCoFHAPI.hasMod())
	    	{
	    		System.out.println("CoFHAPI found, Blocks will require energy.");
	    		useEnergy = true;
	    	}
	    	else
	    	{
	    		System.out.println("CoFHAPI not found, Blocks will not require energy.");
	    		useEnergy = false;
	    	}
    	}
    	else
    		useEnergy = false;
    	
    	//Init Resources
    	resourceManager = new ResourceManager();
    	
    	//Init GUI
    	guiHandler = new GuiHandler();
    	NetworkRegistry.INSTANCE.registerGuiHandler(this, guiHandler);
    	guiHandler.setGuiHandler(GuiSchematicBuilder.GUI_ID, new GuiSchematicBuilder());
    	
    	//Events
    	FMLCommonHandler.instance().bus().register(new TickHandler());
    	FMLCommonHandler.instance().bus().register(playerManager);
    	
    	//Recipe
    	GameRegistry.addRecipe(new ItemStack(BlockLibrary.schematicBuilder), new Object[] {
    		"RAR",
    		"CBC",
    		"RER",
    		'R', Items.redstone, 'B', Blocks.diamond_block, 'A', Items.repeater, 'C', Blocks.redstone_block, 'E', Items.emerald
    	});
    	
    	ModLog.logger.info("Schematic Builder initialized!");
    }
    
    @SubscribeEvent
    public void onServerShutdown(FMLServerStoppingEvent event)  {
    	ModLog.logger.info("SchematicBuilder shutting down...");
    	configGeneral.saveConfig(true);
    	ModLog.logger.info("Done!");
    }

	@Override
	public void onConfigReload(ConfigurationManager configManager) {
    	this.debug = configGeneral.debug;
    	SchematicLoader.writeCompressed = configGeneral.writeCacheCompressed;
    	
    	MessageUploadSchematic.packetSize = configGeneral.networkTransferRate; //TODO: Send this to client too
    	
	}
}
