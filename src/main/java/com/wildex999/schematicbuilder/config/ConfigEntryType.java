package com.wildex999.schematicbuilder.config;

import io.netty.buffer.ByteBuf;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import net.minecraftforge.common.config.Property;

public enum ConfigEntryType {
	UNKNOWN(null, Property.Type.STRING),
	BYTE(byte.class, Property.Type.INTEGER),
	SHORT(short.class, Property.Type.INTEGER),
	INT(int.class, Property.Type.INTEGER),
	FLOAT(float.class, Property.Type.DOUBLE),
	DOUBLE(double.class, Property.Type.DOUBLE),
	BOOLEAN(boolean.class, Property.Type.BOOLEAN),
	STRING(String.class, Property.Type.STRING);
	
	private static int valueCounter = 0;
	
	private int value;
	private final Class<?> typeClass;
	private final Property.Type configType;
	
	
	//Map of values for rapid lookup from serialized
	private static Map<Integer, ConfigEntryType> enumMap;
	static {
		enumMap = new HashMap<Integer, ConfigEntryType>();
		for(ConfigEntryType type : EnumSet.allOf(ConfigEntryType.class))
		{
			type.value = valueCounter++;
			enumMap.put(type.value, type);
		}
	}
	
	ConfigEntryType(Class<?> typeClass, Property.Type configType) {
		this.typeClass = typeClass;
		this.configType = configType;
	}
	
	public Class<?> getTypeClass() {
		return typeClass;
	}
	
	public Property.Type getConfigType() {
		return configType;
	}
	
	public int getValue() {
		return value;
	}
	
	public static ConfigEntryType fromValue(int value) {
		return enumMap.get(value);
	}
	
	public static ConfigEntryType typeOf(Field field) {
		Class<?> typeClass = field.getType();
		if(typeClass == ConfigEntryType.BYTE.getTypeClass())
			return ConfigEntryType.BYTE;
		else if(typeClass == ConfigEntryType.SHORT.getTypeClass())
			return ConfigEntryType.SHORT;
		else if(typeClass == ConfigEntryType.INT.getTypeClass())
			return ConfigEntryType.INT;
		else if(typeClass == ConfigEntryType.FLOAT.getTypeClass())
			return ConfigEntryType.FLOAT;
		else if(typeClass == ConfigEntryType.DOUBLE.getTypeClass())
			return ConfigEntryType.DOUBLE;
		else if(typeClass == ConfigEntryType.BOOLEAN.getTypeClass())
			return ConfigEntryType.BOOLEAN;
		else if(typeClass == ConfigEntryType.STRING.getTypeClass())
			return ConfigEntryType.STRING;
		else
			return ConfigEntryType.UNKNOWN;
	}
	
}
