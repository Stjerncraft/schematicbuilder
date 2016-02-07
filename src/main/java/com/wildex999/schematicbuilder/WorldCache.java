package com.wildex999.schematicbuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;

import com.wildex999.schematicbuilder.schematic.Schematic;
import com.wildex999.schematicbuilder.schematic.SchematicBlock;
import com.wildex999.utils.ModLog;

import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraftforge.common.util.ForgeDirection;

/*
 * Custom IBlockAccess storing a World for use when rendering preview
 * TODO: Implement World to allow custom TileEntity renderer
 */

public class WorldCache extends World {

	private Schematic schematic;
	
	private Block block;
	private byte meta;
	
	private int width;
	private int height;
	private int length;
	
	//TODO: Allow for 'loading' into cache gradually(One chunk at a time etc.)
	ArrayList<Block> blocks;
	ArrayList<Byte> metadata;
	
	
	static Field arrayField = null;
	static ISaveHandler saveHandler = null;
	static WorldProvider wProvider = null;
	static WorldSettings wSettings = new WorldSettings(0, WorldSettings.GameType.SURVIVAL, false, false, WorldType.DEFAULT);
	static Profiler p = null;
	
	@SideOnly(Side.CLIENT)
	public WorldCache(Schematic schematic) {
		super(saveHandler, "", wProvider, wSettings, p);
		
		//World Constructor allocates 128kb array, we remove it.
		//This is an ugly solution which causes a lot of gc churn.
		try {
			if(arrayField == null)
				arrayField = ReflectionHelper.findField(World.class, "field_72994_J", "lightUpdateBlockList");
			arrayField.set(this, null);
		} catch (Exception e) {
			if(ModSchematicBuilder.debug) {
				ModLog.logger.info(e.getMessage());
				ModLog.logger.info("Failed to get field: 'lightUpdateBlockList' from World. There will potentially be a higher memory usage for client due to this.");
			}
			
		}
		
		this.schematic = schematic;
		
		this.width = schematic.getWidth();
		this.height = schematic.getHeight();
		this.length = schematic.getLength();
		
		blocks = new ArrayList<Block>(width*height*length);
		metadata = new ArrayList<Byte>(width*height*length);
		
		//Copy over the data
		for(int x = 0; x < width; x++)
		{
			for(int y = 0; y < height; y++)
			{
				for(int z = 0; z < length; z++)
				{
					SchematicBlock schematicBlock = schematic.getBlock(x, y, z);
					Block block;
					byte meta = 0;
					if(schematicBlock != null)
					{
						block = schematicBlock.getServerBlock(schematic);
						meta = schematicBlock.getMeta(schematic);
					}
					else
						block = null;

					
					if(block != null)
					{
						blocks.add(block);
						metadata.add(meta);
					}
					else
					{
						blocks.add(Blocks.air);
						metadata.add((byte) 0);
					}
				}
			}
		}
	}
	
	@SideOnly(Side.CLIENT)
	public WorldCache(Block block, byte meta) {
		super(saveHandler, "", wProvider, wSettings, p);
		this.block = block;
		this.meta = meta;
		
		this.width = 1;
		this.height = 1;
		this.length = 1;
		
		blocks = new ArrayList<Block>(1);
		metadata = new ArrayList<Byte>(1);
		
		blocks.add(block);
		metadata.add(meta);
	}
	
	public Schematic getSchematic() {
		return schematic;
	}
	
	public Block getBlock() {
		return block;
	}
	
	private int getIndex(int x, int y, int z) {
		return (x*height*length) + (y*length) + z;
	}
	
	@Override
	public Block getBlock(int x, int y, int z) {
		if(x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= length)
			return Blocks.air;
		return blocks.get(getIndex(x,y,z));
	}
	
	@Override
	public boolean blockExists(int x, int y, int z) {
		return blocks.get(getIndex(x,y,z)) != null;
	}

	@Override
	public TileEntity getTileEntity(int x, int y,int z) {
		try {
			Block block = getBlock(x, y, z);
			int meta = getBlockMetadata(x, y, z);
			TileEntity tile = block.createTileEntity(this, meta);
			if(tile != null)
				tile.setWorldObj(this);
			return tile;
		} catch (Exception e) {
			//Simply ignore errors, as they will happen if TileEntity requires a valid world
		}
		return null;
	}

	@Override
	public int getLightBrightnessForSkyBlocks(int x, int y,int z, int p_72802_4_) {
        return EnumSkyBlock.Block.defaultLightValue;
	}

	@Override
	public int getBlockMetadata(int x, int y, int z) {
		if(x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= length)
			return 0;
		return metadata.get(getIndex(x,y,z));
	}
	
	@Override
	public boolean setBlockMetadataWithNotify(int x, int y, int z, int meta, int flag) {
		if(x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= length)
			return false;
		
		metadata.set(getIndex(x,y,z), (byte)meta);
		return true;
	}

	@Override
	public int isBlockProvidingPowerTo(int x, int y,int z, int p_72879_4_) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isAirBlock(int x, int y, int z) {
		return getBlock(x, y, z).isAir(this, x, y, z);
	}

	@Override
	public BiomeGenBase getBiomeGenForCoords(int x, int z) {
		return BiomeGenBase.sky;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public boolean extendedLevelsInChunkCache() {
		return true;
	}

	@Override
	public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
        if (x < -30000000 || z < -30000000 || x >= 30000000 || z >= 30000000)
        {
            return _default;
        }

        return getBlock(x, y, z).isSideSolid(this, x, y, z, side);
	}

	@Override
	protected IChunkProvider createChunkProvider() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int func_152379_p() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Entity getEntityByID(int p_73045_1_) {
		// TODO Auto-generated method stub
		return null;
	}

}
