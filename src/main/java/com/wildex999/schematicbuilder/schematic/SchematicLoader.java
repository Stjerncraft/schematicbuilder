package com.wildex999.schematicbuilder.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.wildex999.schematicbuilder.exceptions.ExceptionInvalid;
import com.wildex999.schematicbuilder.exceptions.ExceptionLoad;
import com.wildex999.schematicbuilder.exceptions.ExceptionRead;
import com.wildex999.utils.ModLog;

import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

//Schematic Loading/Saving heavily based on 
//https://github.com/Lunatrius/Schematica/blob/master/src/main/java/com/github/lunatrius/schematica/world/schematic/SchematicAlpha.java
//Which uses The MIT License (MIT)
//Copyright (c) 2014 Jadran "Lunatrius" Kotnik
//https://github.com/Lunatrius/Schematica/blob/master/LICENSE

//Modification and addition done by Kai Roar Stjern ( wildex999 ) 

public class SchematicLoader {
	
	public static final String NBT_NAME = "SchematicName";
	
	public static final String NBT_MATERIALS = "Materials";
	public static final String NBT_SCHEMATIC_CLASSIC = "Classic";
	public static final String NBT_SCHEMATIC_ALPHA = "Alpha";
	
	public static final String NBT_BLOCKS = "Blocks";
	public static final String NBT_DATA = "Data";
	public static final String NBT_EXTENDED_METADATA = "ExtendedMetadata";
	
	public static final String NBT_WIDTH = "Width";
	public static final String NBT_HEIGHT = "Height";
	public static final String NBT_LENGTH = "Length";
	
	public static final String NBT_ADD_BLOCKS = "AddBlocks";
	public static final String NBT_ADD_BLOCKS_SCHEMATICA = "Add";
	public static final String NBT_MAPPING_SCHEMATICA = "SchematicaMapping";
	
	public static final String NBT_TILE_ENTITIES = "TileEntities";
	public static final String NBT_ENTITIES = "Entities";
	
	private static final FMLControlledNamespacedRegistry<Block> BlockRegistry = GameData.getBlockRegistry();
	
	public static Schematic loadSchematic(File file) throws IOException, ExceptionLoad, ExceptionRead {
        NBTTagCompound tagCompound = readTagCompoundFromFile(file);
        return loadSchematic(tagCompound);
	}
	
	public static NBTTagCompound readTagCompoundFromFile(File file) throws IOException, ExceptionRead {
		try {
			return CompressedStreamTools.readCompressed(new FileInputStream(file));
		} catch (Exception e) {
			ModLog.logger.info("Failed to read as compressed NBT, attempting to read as non-compressed...");
			try {
				return CompressedStreamTools.read(file);
			} catch (Exception e2) {
				throw new ExceptionRead("Failed to read file, corrupt? Error: " + e2.getMessage());
			}
		}
	}
	
	public static Schematic loadSchematic(NBTTagCompound tagCompound) throws ExceptionLoad {	
        boolean extraData = false;
		
		//Verify that it's a supported format
        final String format = tagCompound.getString(NBT_MATERIALS);
        if(!format.equals(NBT_SCHEMATIC_ALPHA))
        	throw new ExceptionLoad("Unsupported Schematic format: " + format);
        
        String schematicName = tagCompound.getString(NBT_NAME);
        
        byte blocks[] = tagCompound.getByteArray(NBT_BLOCKS);
        byte metaData[] = tagCompound.getByteArray(NBT_DATA);
		
        int width = tagCompound.getShort(NBT_WIDTH);
        int height = tagCompound.getShort(NBT_HEIGHT);
        int length = tagCompound.getShort(NBT_LENGTH);
        
        if(width == 0 || height == 0 || length == 0)
        	throw new ExceptionInvalid("Width, height or length is not set!");
        
        if(blocks.length < (width*height*length))
        		throw new ExceptionInvalid("Not enough blocks provided to cover the defined size! Got "
        				+ blocks.length + " blocks, with size: " + (width*height*length) + "."
        				+ " Width: " + width + " Height: " + height + " Length: " + length);
        
        
        byte extraBlocks[] = null;
        byte extraBlocksNibble[] = null;
        if(tagCompound.hasKey(NBT_ADD_BLOCKS)) {
        	extraData = true;
        	extraBlocksNibble = tagCompound.getByteArray(NBT_ADD_BLOCKS);
        	
        	if(extraBlocksNibble.length < (width*height*length))
        		throw new ExceptionInvalid("Not enough extra Block data provided to cover the defined size! Got "
        				+ blocks.length + " blocks, with size: " + (width*height*length) + "."
        				+ " Width: " + width + " Height: " + height + " Length: " + length);
        	
        	//This one has been packed, so we have to spread it to 2 bytes, with 4 bits per byte
        	extraBlocks = new byte[extraBlocksNibble.length * 2];
        	for(int i = 0; i < extraBlocksNibble.length; i++) {
        		extraBlocks[i * 2] = (byte) ((extraBlocksNibble[i] >> 4) & 0xF);
        		extraBlocks[i * 2 + 1] = (byte) (extraBlocksNibble[i] & 0xF);
        	}
        } else if(tagCompound.hasKey(NBT_ADD_BLOCKS_SCHEMATICA)) {
        	extraData = true;
        	//This one is already in the format of 4 bits per byte(But uses more space when stored)
        	extraBlocks = tagCompound.getByteArray(NBT_ADD_BLOCKS_SCHEMATICA);
        }
        
        //Read Mapping for name to id if included
        Map<Integer, Integer> nameMap = null;
        if(tagCompound.hasKey(NBT_MAPPING_SCHEMATICA)) {
        	nameMap = new HashMap<Integer, Integer>();
        	NBTTagCompound mapping = tagCompound.getCompoundTag(NBT_MAPPING_SCHEMATICA);
        	Set<String> names = mapping.func_150296_c();
        	for(String name : names) {
        		nameMap.put(mapping.getInteger(name), BlockRegistry.getId(name));
        	}
        }
        
        //TODO: Read TileEntities
        //TODO: Read Entities
        
        
        //Create Schematic from data
        Schematic schematic = new Schematic(width, height, length);
        schematic.name = schematicName;

        //Store blocks
        for(int x = 0; x < width; x++)
        {
        	for(int y = 0; y < height; y++)
        	{
        		for(int z = 0; z < length; z++)
        		{
        			int index = x + (y * length + z) * width;
        			int blockID = blocks[index] & 0xFF;
        			if(extraData)
        				blockID = blockID | ((extraBlocks[index] & 0xFF) << 8);
        			byte meta = (byte)(metaData[index] & 0xFF);
        			
        			//Map to the new correct block ID if a name map was included
        			if(nameMap != null)
        			{
        				Integer id = nameMap.get(blockID);
        				if(id != null)
        					blockID = id;
        			}
        			
        			Block block = BlockRegistry.getRaw(blockID); //Allow it to return null for unknown ID
        			schematic.setBlock(x, y, z, block, meta);
        		}
        	}
        }
		
		return schematic;
	}
}
