package com.wildex999.schematicbuilder.schematic;

import java.io.File;

/*
 * Schematic is stored on file and read as needed
 * 
 */

public class StreamedSchematic extends Schematic {

	public StreamedSchematic(File file) {
		super(0, 0, 0);
		//TODO: Read metadata(Size, name etc.) from file
	}

}
