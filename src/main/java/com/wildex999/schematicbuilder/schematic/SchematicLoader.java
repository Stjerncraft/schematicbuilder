package com.wildex999.schematicbuilder.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableInt;

import com.wildex999.schematicbuilder.exceptions.ExceptionInvalid;
import com.wildex999.schematicbuilder.exceptions.ExceptionLoad;
import com.wildex999.schematicbuilder.exceptions.ExceptionSave;
import com.wildex999.utils.ModLog;

import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
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
	public static final String NBT_AUTHOR = "SchematicAuthor";
	
	public static final String NBT_MATERIALS = "Materials";
	public static final String NBT_SCHEMATIC_CLASSIC = "Classic";
	public static final String NBT_SCHEMATIC_ALPHA = "Alpha";
	
	public static final String NBT_SCHEMATICBUILDER_VERSION = "SchematicBuilderVersion";
	public static final int SchematicBuilderVersion = 1;
	
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
	
	public static final short maxBlockId = 4095;
	
	public static boolean writeCompressed = true; //TODO: Add as config option
	
	private static final FMLControlledNamespacedRegistry<Block> BlockRegistry = GameData.getBlockRegistry();
	
	public static Schematic loadSchematic(File file, HashMap<Short, MutableInt> blockCount) throws IOException, ExceptionLoad {
        NBTTagCompound tagCompound = readTagCompoundFromFile(file);
        return loadSchematic(tagCompound, blockCount);
	}
	
	public static Schematic loadSchematicMeta(File file) throws IOException, ExceptionLoad {
		NBTTagCompound tagCompound = readTagCompoundFromFile(file);
		return loadSchematicMeta(tagCompound);
	}
	
	public static void saveSchematic(File file, Schematic schematic) throws IOException, ExceptionSave {
		NBTTagCompound tag = saveSchematic(schematic);
		writeTagCompoundToFile(file, tag);
	}
	
	public static NBTTagCompound readTagCompoundFromFile(File file) throws IOException {
		try {
			return CompressedStreamTools.readCompressed(new FileInputStream(file));
		} catch (Exception e) {
			ModLog.logger.debug("Failed to read as compressed NBT, attempting to read as non-compressed...");
			return CompressedStreamTools.read(file);
		}
	}
	
	public static void writeTagCompoundToFile(File file, NBTTagCompound tag) throws IOException {
		if(writeCompressed)
			CompressedStreamTools.writeCompressed(tag, new FileOutputStream(file));
		else
			CompressedStreamTools.write(tag, file);
	}
	
	public static Schematic loadSchematicMeta(NBTTagCompound tagCompound) throws ExceptionLoad {
		//Verify that it's a supported format
        final String format = tagCompound.getString(NBT_MATERIALS);
        if(!format.equals(NBT_SCHEMATIC_ALPHA))
        	throw new ExceptionLoad("Unsupported Schematic format: " + format);
        
        String schematicName = tagCompound.getString(NBT_NAME);
        String schematicAuthor = tagCompound.getString(NBT_AUTHOR);
		
        int width = tagCompound.getShort(NBT_WIDTH);
        int height = tagCompound.getShort(NBT_HEIGHT);
        int length = tagCompound.getShort(NBT_LENGTH);
        
        if(width == 0 || height == 0 || length == 0)
        	throw new ExceptionInvalid("Width, height or length is not set!");
        
        //Create Schematic from data
        Schematic schematic = new Schematic(width, height, length);
        schematic.name = schematicName;
        schematic.author = schematicAuthor;
        
        return schematic;
	}
	
	//If blockCount is provided, blocks will be counted and written to this list by block name
	public static Schematic loadSchematic(NBTTagCompound tagCompound, HashMap<Short, MutableInt> blockCount) throws ExceptionLoad {	
        boolean extraData = false;
		
        Schematic schematic = loadSchematicMeta(tagCompound);
        
        int width = schematic.getWidth();
        int height = schematic.getHeight();
        int length = schematic.getLength();
        
        byte blocks[] = tagCompound.getByteArray(NBT_BLOCKS);
        byte metaData[] = tagCompound.getByteArray(NBT_DATA);
        
        if(blocks.length < (width*height*length))
        		throw new ExceptionInvalid("Not enough blocks provided to cover the defined size! Got "
        				+ blocks.length + " blocks, with size: " + (width*height*length) + "."
        				+ " Width: " + width + " Height: " + height + " Length: " + length);
        
        
        byte extraBlocks[] = null;
        byte extraBlocksNibble[] = null;
        if(tagCompound.hasKey(NBT_ADD_BLOCKS)) {
        	extraData = true;
        	extraBlocksNibble = tagCompound.getByteArray(NBT_ADD_BLOCKS);
        	
        	if(extraBlocksNibble.length*2 < (width*height*length))
        		throw new ExceptionInvalid("Not enough extra Block data provided to cover the defined size! Got "
        				+ extraBlocksNibble.length*2 + " blocks, with size: " + (width*height*length) + "."
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
        			int originalId = blockID;
        			if(nameMap != null)
        			{
        				Integer id = nameMap.get(blockID);
        				if(id != null)
        					blockID = id;
        			}
        			
        			Block block = BlockRegistry.getRaw(blockID); //Allow it to return null for unknown ID
        			//TODO: Replace with Unobtainium for the specific unknown id
        			schematic.setBlock(x, y, z, block, meta);
        			
					if(blockCount != null && block != Blocks.air)
					{
						if(blockID >= SchematicLoader.maxBlockId)
							System.out.println("Over block id for block: " + block);
						
						short blockIndex = (short) ((blockID << 4) | meta);
						MutableInt count = blockCount.get(blockIndex);
						if(count != null)
							count.increment();
						else
							blockCount.put(blockIndex, new MutableInt(1));
					}
        		}
        	}
        }
		
		return schematic;
	}
	
	//Save Schematic to NBT. Returns null on failure.
	public static NBTTagCompound saveSchematic(Schematic schematic) throws ExceptionSave {
		if(schematic == null)
			throw new ExceptionSave("Null schematic provided!");
		
		NBTTagCompound nbt = new NBTTagCompound();
		
		nbt.setString(NBT_MATERIALS, NBT_SCHEMATIC_ALPHA);
		nbt.setInteger(NBT_SCHEMATICBUILDER_VERSION, SchematicBuilderVersion);
		
		nbt.setString(NBT_NAME, schematic.name);
		
		if(schematic.getWidth() > Short.MAX_VALUE || schematic.getHeight() > Short.MAX_VALUE || schematic.getLength() > Short.MAX_VALUE)
			throw new ExceptionSave("SchematicBuilder failed to save Schematic due to size limits exceeded(" + Short.MAX_VALUE + "): "
					+ "Width: " + schematic.getWidth() + " Height: " + schematic.getHeight() + " Length: " + schematic.getLength());
		
		nbt.setShort(NBT_WIDTH, (short)schematic.getWidth());
		nbt.setShort(NBT_HEIGHT, (short)schematic.getHeight());
		nbt.setShort(NBT_LENGTH, (short)schematic.getLength());
		
		//Packed means that there are two blocks per byte
		byte blocks[] = new byte[schematic.getWidth()*schematic.getHeight()*schematic.getLength()]; //8 bit
		byte meta[] = new byte[(schematic.getWidth()*schematic.getHeight()*schematic.getLength())]; //8 bit(Not packed yet, TODO)
		byte extra[] = new byte[(schematic.getWidth()*schematic.getHeight()*schematic.getLength())/2]; //4 bit packed
		String mapping[] = new String[maxBlockId+1]; //One name per id
		boolean gotExtraData = false;
		SchematicBlock airBlock = new SchematicBlock(Blocks.air, (byte) 0);
		
		for(int x = 0; x < schematic.getWidth(); x++)
		{
			for(int y = 0; y < schematic.getHeight(); y++)
			{
				for(int z = 0; z < schematic.getLength(); z++)
				{
					int index = x + (y * schematic.getLength() + z) * schematic.getWidth();
					int part = 1 - (index & 1); //We want the first part to be shifted left
					SchematicBlock sBlock = schematic.getBlock(x, y, z);
					if(sBlock == null)
						sBlock = airBlock;
					
					int blockId = sBlock.getBlockId();
					
					blocks[index] = (byte) (blockId & 0xFF);
					if(blockId > 255)
					{
						gotExtraData = true;
						extra[index/2] |= (byte) ((blockId >> 8) << (part*4));
					}
					//meta[index/2] |= sBlock.metaData << (part*4);
					meta[index] = sBlock.metaData; //Currently uses a full byte instead of compressing down to 4 bits
					
					if(mapping[blockId] == null)
						mapping[blockId] = Block.blockRegistry.getNameForObject(sBlock.getBlock());
				}
			}
		}
		
		nbt.setByteArray(NBT_BLOCKS, blocks);
		nbt.setByteArray(NBT_DATA, meta);
		if(gotExtraData)
			nbt.setByteArray(NBT_ADD_BLOCKS, extra);
		
		//Write Block name/id mapping
		NBTTagCompound mappingTag = new NBTTagCompound();
		for(short blockId = 0; blockId <= maxBlockId; blockId++)
		{
			if(mapping[blockId] == null)
				continue;
			mappingTag.setShort(mapping[blockId], blockId);
		}
		
		nbt.setTag(NBT_MAPPING_SCHEMATICA, mappingTag);
		
		//TODO: Write TileEntities
		//TODO: Write Entities
		
		return nbt;
	}
}
