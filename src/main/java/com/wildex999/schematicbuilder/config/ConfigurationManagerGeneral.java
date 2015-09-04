package com.wildex999.schematicbuilder.config;

import java.io.File;

public class ConfigurationManagerGeneral extends ConfigurationManager {

	public ConfigurationManagerGeneral(File configurationFile) {
		super(configurationFile);
	}
	
	//--General--
	@ConfigEntry(name="debug", comment="Print debug information. Warning: Will cause a lot of console/log spam! Only enable if there is a problem.")
	public boolean debug = false;
	
	@ConfigEntry(name="schematicLoaderThreadCount", canReload=false, comment="The number of threads that can be loading a Schematic at the same time. If more Schematics than threads try to load at once,"
			+ " they will be queued.\nThis should be left alone unless you have more free CPU cores and there are a lot of Schematics loading at once.\n"
			+ "This can not be below 1.\nRequires restart!")
	public int loaderThreadCount = 2;
	
	@ConfigEntry(name="energyEnabled", canReload=false, comment="Disable this if you do not wish to use energy with the mod.\nRequires restart!"
			+ "Note that this will be automatically disabled if there are no energy mods implementing RedstoneFlux.")
	public boolean energyEnabled = true;
	
	@ConfigEntry(name="acceptUploadSchematic", comment="Whether or not Clients can upload their own Schematics to the server")
	public boolean acceptUploadSchematic = true;
	@ConfigEntry(name="maxUploadSizeBytes", comment="The max size in bytes of a Schematic that a player can upload")
	public int maxUploadSizeBytes = 10485760;
	@ConfigEntry(name="maxSendSizeBytes", sendToClient=false, comment="The max size in bytes of a Schematic that the server will send to player/client will accept from server")
	public int maxSendSizeBytes = 10485760;
	@ConfigEntry(name="acceptSendSchematic", comment="Whether or not the Server will send Schematics to clients, allowing for preview and Visualization.")
	public boolean acceptSendSchematic = true;
	@ConfigEntry(name="transferPacketSize", comment="How many bytes of data that will transfer per tick. This is for transfer between Server and Client, and is per transfer operation.")
	public int networkTransferRate = 1024;

	@ConfigEntry(name="writeCachedSchematicCompressed", comment="When this is enabled, any cached Schematics will be compressed as they are written to file.")
	public boolean writeCacheCompressed = true;
	
	
	//--Builder--
	@ConfigEntry(category=ConfigCategory.BUILDER, name="userResources", canReload=false, comment="Disable this if you do not wish for the SchematicBuilder to use resources(Blocks/Items)\n"
			+ "Requires restart!")
	public boolean useResources = true;
	
	@ConfigEntry(category=ConfigCategory.BUILDER, name="nopRepeat", comment="How many times the Builder can do a No-Operation per tick.\nA No-Op is anything that does not place or remove a block, "
			+ "things like placing air where there is already air, or if the correct block is already there.")
	public int nopRepeat = 5;
	@ConfigEntry(category=ConfigCategory.BUILDER, name="nopRepeatPass2", comment="Pass 2 usually will not place many blocks, so setting this higher can increase the speed a great deal without much load.")
	public int nopRepeatPassTwo = 10;
	
	@ConfigEntry(category=ConfigCategory.BUILDER, name="maxEnergyStored", comment="How much internal RF the Builder can store")
	public int builderMaxEnergy = 1000000;
	@ConfigEntry(category=ConfigCategory.BUILDER, name="energyCostNop", comment="How much energy it costs when the Builder does not place a block")
	public int builderEnergyCostNOP = 10;
	@ConfigEntry(category=ConfigCategory.BUILDER, name="energyCostBuild", comment="How much energy it costs when the Builder places a block")
	public int builderEnergyCostPlace = 50;
	@ConfigEntry(category=ConfigCategory.BUILDER, name="energyModifierPass2", comment="Modifier for the energy cost of both Nop and Build for pass 2")
	public float builderEnergyModifierPass2 = 0.5f;
	
	@ConfigEntry(category=ConfigCategory.BUILDER, name="schematicMaxWidth", comment="The size width of a Schematic that the Builder is allowed to load")
	public int builderMaxWidth = 1000;
	@ConfigEntry(category=ConfigCategory.BUILDER, name="schematicMaxLength")
	public int builderMaxLength = 1000;
	@ConfigEntry(category=ConfigCategory.BUILDER, name="schematicMaxHeight")
	public int builderMaxHeight = 255;
	
	@Override
	protected void setupDefaults() {
	}

	@Override
	protected void onReload() {
	}

}
