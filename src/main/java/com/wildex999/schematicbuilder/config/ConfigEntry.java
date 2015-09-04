package com.wildex999.schematicbuilder.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigEntry {
	String name();
	String category() default ConfigCategory.GENERAL;
	String comment() default "";
	boolean canReload() default true;
	boolean sendToClient() default true;
	ConfigEntryType type() default ConfigEntryType.UNKNOWN;
}
