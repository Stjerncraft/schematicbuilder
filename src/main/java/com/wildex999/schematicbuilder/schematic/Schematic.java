package com.wildex999.schematicbuilder.schematic;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;

import com.wildex999.utils.ModLog;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;


public class Schematic {
	
	public int serializedVersion = 1; //Used to handle backwards compatibility for server saved Schematics
	public String name;
	public String author;
	
	private int width;
	private int height;
	private int length;
	
	protected List<SchematicBlock> blocks;
	
	public Schematic(int width, int height, int length) {
		this.width = width;
		this.height = height;
		this.length = length;
		
		name = "";
		author = "";
		
		int count = width*height*length;
		blocks = new ArrayList<SchematicBlock>(count);
		for(int i = 0; i < count; i++)
			blocks.add(null);
	}
	
	protected int getIndex(int x, int y, int z) {
		//Always positive positions in Schematic
		return (y * length * width) + (z * width) + x;
	}
	
	public void setBlock(int x, int y, int z, Block block, byte metaData) {
		if(block == null || block == Blocks.air)
			setBlock(x, y, z, null);
		else
			setBlock(x, y, z, new SchematicBlock(block, metaData));
	}
	
	public void setBlock(int x, int y, int z, SchematicBlock block) {
		blocks.set(getIndex(x,y,z), block);
	}
	
	public SchematicBlock getBlock(int x, int y, int z) {
		if(x >= width || y >= height || z >= length)
			return null;
		if(x < 0 || y < 0 || z < 0)
			return null;
		
		return blocks.get(getIndex(x,y,z));
	}
	
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	public int getLength() {
		return length;
	}
	
	public List<SchematicBlock> getBlocks() {
		return blocks;
	}
	
	//Serializing
	public void toBytes(ByteBuf buf) {
		buf.writeInt(serializedVersion);
		ByteBufUtils.writeUTF8String(buf, name);
		ByteBufUtils.writeUTF8String(buf, author);
		buf.writeInt(width);
		buf.writeInt(height);
		buf.writeInt(length);
		
		//Write Blocks
		int size = width*height*length;
		
		for(int x = 0; x < width; x++)
		{
			for(int y = 0; y < height; y++)
			{
				for(int z = 0; z < length; z++)
				{
					SchematicBlock block = getBlock(x, y, z);
					
					if(block == null)
					{
						buf.writeShort(-1);
						continue;
					}
					
					//As of writing this, the max BlockID is 4096, so it fit into 12 bits.
					//MetaData is 4 bits, so we can pack both into 16 bits.
					short data = (short) (block.getBlockId() << 4);
					data |= (short)(block.metaData & 0xF);
					buf.writeShort(data);
				}
			}
		}
		
		//TODO:
		//Write TileEntities
		//Write Entities
	}
	
	public static Schematic fromBytes(ByteBuf buf, HashMap<Short, MutableInt> blockCount) {
		Schematic newSchematic;
		FMLControlledNamespacedRegistry<Block> BlockRegistry = GameData.getBlockRegistry();
		
		int serializedVersion = buf.readInt();
		String name = ByteBufUtils.readUTF8String(buf);
		String author = ByteBufUtils.readUTF8String(buf);
		int width = buf.readInt();
		int height = buf.readInt();
		int length = buf.readInt();
		
		newSchematic = new Schematic(width, height, length);
		newSchematic.serializedVersion = serializedVersion;
		newSchematic.name = name;
		newSchematic.author = author;
		
		//TODO: Handle loading old serialized Schematic versions
		
		//Read Blocks
		for(int x = 0; x < width; x++)
		{
			for(int y = 0; y < height; y++)
			{
				for(int z = 0; z < length; z++)
				{
					int data = buf.readShort();
					int blockId = (data >> 4) & 0xFFF; //Get left 12 bits
					byte meta = (byte) (data & 0xF); //Get right 4 bits
					
					if(blockId == -1)
						continue;

					Block block = BlockRegistry.getRaw(blockId); //Allow it to return null for unknown ID
					newSchematic.setBlock(x, y, z, block, meta);
					
					if(blockCount != null && block != null)
					{
						if(blockId >= SchematicLoader.maxBlockId)
							System.out.println("Over block id for block: " + block);
						
						short index = (short) ((blockId << 4) | meta);
						MutableInt count = blockCount.get(index);
						if(count != null)
							blockCount.get(index).increment();
						else
							blockCount.put(index, new MutableInt(0));
					}
				}
			}
		}
		
		//TODO:
		//Read TileEntities
		//Read Entities
		
		return newSchematic;
	}
	
}
