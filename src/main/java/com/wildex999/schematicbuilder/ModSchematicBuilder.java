package com.wildex999.schematicbuilder;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.wildex999.schematicbuilder.blocks.BlockLibrary;
import com.wildex999.schematicbuilder.gui.GuiHandler;
import com.wildex999.schematicbuilder.gui.GuiSchematicBuilder;
import com.wildex999.schematicbuilder.network.Networking;
import com.wildex999.schematicbuilder.schematic.SchematicLoaderService;
import com.wildex999.utils.ModLog;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(modid = ModSchematicBuilder.MODID, version = ModSchematicBuilder.VERSION, dependencies = "after:CoFHAPI")
public class ModSchematicBuilder {
	public static final String MODID = "schematicbuilder";
	public static final String VERSION = "0.2.4";
	public static ModSchematicBuilder instance;
	
	public static boolean debug = true;
	
	@SidedProxy(clientSide = "com.wildex999.schematicbuilder.ClientProxy", serverSide = "com.wildex999.schematicbuilder.CommonProxy")
	public static CommonProxy proxy;
	
	public static SchematicLoaderService schematicLoaderService;
	
	public GuiHandler guiHandler;
	public static boolean useEnergy;
	
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
    	proxy.initialize();
    	
    	//TODO: Load thread count from config
    	schematicLoaderService = new SchematicLoaderService(2);
    	
    	ModCheck modCheckCoFHAPI = new ModCheckCoFHAPI();
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
    	//TODO: FOR TESTING
    	useEnergy = false;
    	
    	//Init GUI
    	guiHandler = new GuiHandler();
    	NetworkRegistry.INSTANCE.registerGuiHandler(this, guiHandler);
    	guiHandler.setGuiHandler(GuiSchematicBuilder.GUI_ID, new GuiSchematicBuilder());
    	
    	//Events
    	FMLCommonHandler.instance().bus().register(new TickHandler());
    	
    	//Recipe
    	GameRegistry.addRecipe(new ItemStack(BlockLibrary.schematicBuilder), new Object[] {
    		"RAR",
    		"CBC",
    		"RER",
    		'R', Items.redstone, 'B', Blocks.diamond_block, 'A', Items.repeater, 'C', Blocks.redstone_block, 'E', Items.emerald
    	});
    	
    	ModLog.logger.info("Schematic Builder initialized!");
    }
}
