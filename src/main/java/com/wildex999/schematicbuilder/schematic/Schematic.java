package com.wildex999.schematicbuilder.schematic;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;
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
	
	private int chunksX;
	private int chunksY;
	private int chunksZ;
	protected int chunkSize = 16; //Note: Parts of getBlock and setBlock is hardcoded for 16 chunksize
	protected List<ArrayList<SchematicBlock>> chunks;
	
	public Schematic(int width, int height, int length) {
		this.width = width;
		this.height = height;
		this.length = length;
		
		name = "";
		author = "";
		
		chunksX = (int) Math.ceil((double)width/chunkSize);
		chunksY = (int) Math.ceil((double)height/chunkSize);
		chunksZ = (int) Math.ceil((double)length/chunkSize);
		int chunkCount = chunksX * chunksY * chunksZ;
		chunks = new ArrayList<ArrayList<SchematicBlock>>(chunkCount);
		for(int i = 0; i < chunkCount; i++)
			chunks.add(null);
	}
	
	//Get the index of the given chunk
	protected int getChunkIndex(int chunkX, int chunkY, int chunkZ) {
		return (chunkY*chunksZ*chunksX) + (chunkZ*chunksX) + chunkX;
	}
	
	//Get the index of the given block in a chunk
	protected int getBlockIndex(int x, int y, int z) {
		//Always positive positions in Schematic
		return (y * chunkSize * chunkSize) + (z * chunkSize) + x;
	}
	
	public void setBlock(int x, int y, int z, Block block, byte metaData) {
		if(block == null || block == Blocks.air)
			setBlock(x, y, z, null);
		else
			setBlock(x, y, z, new SchematicBlock(block, metaData));
	}
	
	public void setBlock(int x, int y, int z, SchematicBlock block) {
		if(x >= width || y >= height || z >= length)
			return ;
		if(x < 0 || y < 0 || z < 0)
			return ;
		
		int chunkIndex = getChunkIndex(x >> 4, y >> 4, z >> 4); //Note: Hardcoded for 16 chunksize
		ArrayList<SchematicBlock> chunk = chunks.get(chunkIndex);
		
		if(chunk == null)
		{
			if(block == null)
				return;
			
			chunk = new ArrayList<SchematicBlock>();
			for(int i=0; i<chunkSize*chunkSize*chunkSize; i++)
				chunk.add(null);
			chunks.set(chunkIndex, chunk);
		}
		
		chunk.set(getBlockIndex(x & 0xF, y & 0xF, z & 0xF), block);
	}
	
	public SchematicBlock getBlock(int x, int y, int z) {
		if(x >= width || y >= height || z >= length)
			return null;
		if(x < 0 || y < 0 || z < 0)
			return null;
		
		ArrayList<SchematicBlock> chunk = chunks.get(getChunkIndex(x >> 4, y >> 4, z >> 4)); //Note: Hardcoded for 16 chunksize
		if(chunk == null)
			return null;
		
		return chunk.get(getBlockIndex(x & 0xF, y & 0xF, z & 0xF));
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
	
	public List<ArrayList<SchematicBlock>> getChunks() {
		return chunks;
	}
	
	public ArrayList<SchematicBlock> getChunkAt(int x, int y, int z) {
		if(x >= width || y >= height || z >= length)
			return null;
		if(x < 0 || y < 0 || z < 0)
			return null;
		
		return chunks.get(getChunkIndex(x >> 4, y >> 4, z >> 4));
	}
	
	//Serializing
	public void toBytes(ByteBuf buf) {
		buf.writeInt(serializedVersion);
		ByteBufUtils.writeUTF8String(buf, name);
		ByteBufUtils.writeUTF8String(buf, author);
		buf.writeInt(width);
		buf.writeInt(height);
		buf.writeInt(length);
		
		int emptyChunks = 0;
		
		//Write Chunks
		for(int chunkX = 0; chunkX < chunksX; chunkX++)
		{
			for(int chunkY = 0; chunkY < chunksY; chunkY++)
			{
				for(int chunkZ = 0; chunkZ < chunksZ; chunkZ++)
				{
					ArrayList<SchematicBlock> chunk = chunks.get(getChunkIndex(chunkX, chunkY, chunkZ));
					if(chunk == null)
					{
						emptyChunks++;
						buf.writeByte(0);
						continue;
					}
					else
						buf.writeByte(1);
					
					//Write Blocks
					for(int x = chunkX*chunkSize; x < width && x < (chunkX+1) * chunkSize; x++)
					{
						for(int y = chunkY*chunkSize; y < height && y < (chunkY+1) * chunkSize; y++)
						{
							for(int z = chunkZ*chunkSize; z < length && z < (chunkZ+1) * chunkSize; z++)
							{
								SchematicBlock block = chunk.get(getBlockIndex(x & 0xF, y & 0xF, z & 0xF));
								
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
					
				}
			}
		}
		
		if(ModSchematicBuilder.debug)
			ModLog.logger.info("Empty chunks in Schematic: " + emptyChunks);
		
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
		
		int chunksX = newSchematic.chunksX;
		int chunksY = newSchematic.chunksY;
		int chunksZ = newSchematic.chunksZ;
		int chunkCount = chunksX*chunksY*chunksZ;
		int chunkSize = newSchematic.chunkSize;
		
		//TODO: Handle loading old serialized Schematic versions
		
		//Read Chunks
		for(int chunkX = 0; chunkX < chunksX; chunkX++)
		{
			for(int chunkY = 0; chunkY < chunksY; chunkY++)
			{
				for(int chunkZ = 0; chunkZ < chunksZ; chunkZ++)
				{
					byte gotChunk = buf.readByte();
					if(gotChunk == 0)
						continue;
					
					//Write Blocks
					for(int x = chunkX*chunkSize; x < width && x < (chunkX+1) * chunkSize; x++)
					{
						for(int y = chunkY*chunkSize; y < height && y < (chunkY+1) * chunkSize; y++)
						{
							for(int z = chunkZ*chunkSize; z < length && z < (chunkZ+1) * chunkSize; z++)
							{
								int data = buf.readShort();
								
								if(data == -1)
									continue;
								
								int blockId = (data >> 4) & 0xFFF; //Get left 12 bits
								byte meta = (byte) (data & 0xF); //Get right 4 bits

								Block block = BlockRegistry.getRaw(blockId); //Allow it to return null for unknown ID
								newSchematic.setBlock(x, y, z, block, meta);
								
								if(blockCount != null && block != null)
								{
									if(blockId >= SchematicLoader.maxBlockId && ModSchematicBuilder.debug)
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
					
				}
			}
		}
		
		//TODO:
		//Read TileEntities
		//Read Entities
		
		return newSchematic;
	}
	
}
