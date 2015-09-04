package com.wildex999.schematicbuilder.config;

import io.netty.buffer.ByteBuf;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.utils.ModLog;

import cpw.mods.fml.common.network.ByteBufUtils;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public abstract class ConfigurationManager {
	protected HashSet<IConfigListener> configListeners;
	
	protected class ConfigEntryStored {
		public String category;
		public String name;
		public String comment;
		public boolean canReload;
		public boolean sendToClient;
		public ConfigEntryType type;
	}
	protected HashMap<Field, ConfigEntryStored> configEntries;
	
	protected File configFile;
	protected Configuration config;
	protected boolean configLoaded;
	
	public boolean markedForSave;
	
	public ConfigurationManager(File configurationFile) {
		configListeners = new HashSet<IConfigListener>();
		configEntries = new HashMap<Field, ConfigEntryStored>();
		config = null;
		configFile = configurationFile;
		configLoaded = false;
	}
	
	public void loadConfig() {
		if(configLoaded)
			return;
		
		config = new Configuration(configFile);
		
		//Process ConfigEntry annotations
		Field[] fields = this.getClass().getFields();
		for(Field field : fields)
		{
			Annotation annotation = field.getAnnotation(ConfigEntry.class);
			if(annotation != null)
			{
				ConfigEntry entry = (ConfigEntry) annotation;
				ConfigEntryStored storedEntry = new ConfigEntryStored();
				storedEntry.name = entry.name();
				storedEntry.category = entry.category();
				storedEntry.comment = entry.comment();
				storedEntry.canReload = entry.canReload();
				storedEntry.sendToClient = entry.sendToClient();
				
				if(entry.type() == ConfigEntryType.UNKNOWN)
					storedEntry.type = ConfigEntryType.typeOf(field);
				else
					storedEntry.type = entry.type();
				
				configEntries.put(field, storedEntry);
			}
		}
		readConfigEntries(false);
		
		//Setup any remaining defaults
		setupDefaults();
		configLoaded = true;
		saveConfig(true);
	}
	
	//Read the configuration values for the Config Entries
	protected void readConfigEntries(boolean reloading) {
		for(Entry<Field, ConfigEntryStored> entry : configEntries.entrySet())
		{
			Field field = entry.getKey();
			ConfigEntryStored configEntry = entry.getValue();
			if(reloading && !configEntry.canReload)
				continue;
			
			try {
				Property prop = config.get(configEntry.category, configEntry.name, String.valueOf(field.get(this)), configEntry.comment, configEntry.type.getConfigType());
				switch(configEntry.type)
				{
				case UNKNOWN:
					ModLog.logger.error("Unable to load value of unknown type from config: " + configEntry.category + "." + configEntry.name);
					break;
				case BYTE:
					field.setByte(this, (byte)prop.getInt());
					break;
				case SHORT:
					field.setShort(this, (short)prop.getInt());
					break;
				case INT:
					field.setInt(this, prop.getInt());
					break;
				case FLOAT:
					field.setFloat(this, (float)prop.getDouble());
					break;
				case DOUBLE:
					field.setDouble(this, prop.getDouble());
					break;
				case BOOLEAN:
					field.setBoolean(this, prop.getBoolean());
					break;
				case STRING:
					field.set(this, prop.getString());
					break;
				}
			} catch (Exception e) {
				ModLog.logger.error("Failed to read config entry: " + configEntry.category + "." + configEntry.name);
				e.printStackTrace();
			}
		}
	}
	
	
	//Write the changed values into the config
	protected void writeConfigEntries() {
		for(Entry<Field, ConfigEntryStored> entry : configEntries.entrySet())
		{
			Field field = entry.getKey();
			ConfigEntryStored configEntry = entry.getValue();
			
			try {
				Property prop = config.get(configEntry.category, configEntry.name, String.valueOf(field.get(this)), configEntry.comment, configEntry.type.getConfigType());
				prop.setValue(String.valueOf(field.get(this)));
			} catch (Exception e) {
				ModLog.logger.error("Failed to write config entry: " + configEntry.category + "." + configEntry.name);
				e.printStackTrace();
			}
		}
	}
	
	//Reload values from config file
	//fromFile: Whether to read changes from file
	public void reload(boolean fromFile) {
		if(fromFile)
			config = new Configuration(configFile);
		onReload();
		notifyListenersReload();
	}
	
	//Called after first loading the config to set defaults
	protected abstract void setupDefaults(); 
	//Called after the Configuration file has been reloaded to read the new values
	protected abstract void onReload();
	
	public void saveConfig(boolean now) {
		if(now)
		{
			writeConfigEntries();
			config.save();
			markedForSave = false;
		}
		else
			markedForSave = true;
	}
	
	//Serialization
	//Write the annotated fields
	public void toBytes(ByteBuf buf) throws Exception {
		buf.writeInt(configEntries.size());
		for(Entry<Field, ConfigEntryStored> entry : configEntries.entrySet())
		{
			Field field = entry.getKey();
			ConfigEntryStored configEntry = entry.getValue();
			
			if(!configEntry.sendToClient)
				continue;
			
			ByteBufUtils.writeUTF8String(buf, field.getName());
			buf.writeInt(configEntry.type.getValue());
			
			switch(configEntry.type)
			{
			case UNKNOWN:
				if(ModSchematicBuilder.debug)
					ModLog.logger.error("Unable to serialize config option: " + field.getName() + " for sending over network! Unknown data type!");
				break;
			case BYTE:
				buf.writeByte(field.getByte(this));
				break;
			case SHORT:
				buf.writeShort(field.getShort(this));
				break;
			case INT:
				buf.writeInt(field.getInt(this));
				break;
			case FLOAT:
				buf.writeFloat(field.getFloat(this));
				break;
			case DOUBLE:
				buf.writeDouble(field.getDouble(this));
				break;
			case BOOLEAN:
				buf.writeBoolean(field.getBoolean(this));
				break;
			case STRING:
				ByteBufUtils.writeUTF8String(buf, (String)field.get(this));
				break;
			}
		}
	}
	
	//Read the annotated fields
	public void fromBytes(ByteBuf buf) throws Exception {
		int count = buf.readInt();
		for(int i=0; i<count; i++)
		{
			String fieldName = ByteBufUtils.readUTF8String(buf);
			ConfigEntryType type = ConfigEntryType.fromValue(buf.readInt());
			
			Field field = this.getClass().getField(fieldName);
			switch(type)
			{
			case UNKNOWN:
				if(ModSchematicBuilder.debug)
					ModLog.logger.error("Unable to deserialize config option: " + field.getName() + " from network! Unknown data type!");
				break;
			case BYTE:
				field.setByte(this, buf.readByte());
				break;
			case SHORT:
				field.setShort(this, buf.readShort());
				break;
			case INT:
				field.setInt(this, buf.readInt());
				break;
			case FLOAT:
				field.setFloat(this, buf.readFloat());
				break;
			case DOUBLE:
				field.setDouble(this, buf.readDouble());
				break;
			case BOOLEAN:
				field.setBoolean(this, buf.readBoolean());
				break;
			case STRING:
				field.set(this, ByteBufUtils.readUTF8String(buf));
				break;
			}
		}
	}
	
	//Notify all listeners of a reload
	protected void notifyListenersReload() {
		for(IConfigListener listener : configListeners)
			listener.onConfigReload(this);
	}
	
	public boolean addConfigListener(IConfigListener listener) {
		return configListeners.add(listener);
	}
	
	public boolean removeConfigListener(IConfigListener listener) {
		return configListeners.remove(listener);
	}
}
