package com.wildex999.schematicbuilder.tiles;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum BuilderState {
	IDLE(0), //No state set yet
	CHOOSINGLOCAL(1), //Client: Currently choosing file to load
	READING(2), //Reading from stream/file
	LOADING(3), //Loading/Parsing Schematic from NBT
	UPLOADING(4), //Client: Uploading to server
	DOWNLOADING(5), //Downloading from Server/Client
	PREPARING(6), //Creating list of materials etc.(Spread over multiple ticks)
	READY(7), //Schematic loaded, and ready to build
	CLEARING(8), //Clearing space for building
	CHECKING(9), //Checking for already placed blocks(Run after server restart/TileEntity reload, before continuing to build)
	BUILDING(10), //Building from Schematic
	NEEDRESOURCES(11), //Waiting for more Resources(Materials/Energy)
	DONE(12), //Done building from Schematic
	DONEMISSING(13), //Done, but unable to place some blocks
	ERROR(14), //Same as IDLE, but show an error message
	STOPPED(15); //Same as BUILDING, but not actively running
	
	private final int value;
	
	//Map of values for rapid lookup from serialized
	private static Map<Integer, BuilderState> enumMap;
	static {
		enumMap = new HashMap<Integer, BuilderState>();
		for(BuilderState state : EnumSet.allOf(BuilderState.class))
			enumMap.put(state.value, state);
	}
	
	BuilderState(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	public static BuilderState fromValue(int value) {
		return enumMap.get(value);
	}
}
