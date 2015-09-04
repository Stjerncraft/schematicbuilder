package com.wildex999.schematicbuilder.config;

//Implemented by anyone interested in Configuration reloads

public interface IConfigListener {
	void onConfigReload(ConfigurationManager configManager);
}
