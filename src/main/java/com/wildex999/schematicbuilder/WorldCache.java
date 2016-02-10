package com.wildex999.schematicbuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import com.wildex999.schematicbuilder.schematic.Schematic;
import com.wildex999.schematicbuilder.schematic.SchematicBlock;
import com.wildex999.utils.ModLog;

import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

/*
 * Custom IBlockAccess storing a World for use when rendering preview
 * TODO: Implement World to allow custom TileEntity renderer
 */

public class WorldCache extends World {

	private Schematic schematic;
	private IBlockState block;
	
	private int width;
	private int height;
	private int length;
	
	//TODO: Allow for 'loading' into cache gradually(One chunk at a time etc.)
	ArrayList<IBlockState> blocks;
	
	
	static Field arrayField = null;
	static ISaveHandler saveHandler = null;
	static WorldSettings wSettings = new WorldSettings(0, WorldSettings.GameType.SURVIVAL, false, false, WorldType.DEFAULT);
	static Profiler p = null;
	
	static WorldProvider wProvider = new WorldProvider() {
		
		@Override
		public String getInternalNameSuffix() {
			return "SB_";
		}
		
		@Override
		public String getDimensionName() {
			return "SchematicBuilder_DummyWorld";
		}
	};
	
	
	@SideOnly(Side.CLIENT)
	public WorldCache(Schematic schematic, HashMap<Short, ResourceItem> resourceMap) {
		super(saveHandler, new WorldInfo(wSettings, "SchematicBuilder_DummyWorld"), wProvider, p, true);
		
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
		
		blocks = new ArrayList<IBlockState>(width*height*length);

		//Copy over the data
		for(int x = 0; x < width; x++)
		{
			for(int y = 0; y < height; y++)
			{
				for(int z = 0; z < length; z++)
				{
					SchematicBlock schematicBlock = schematic.getBlock(x, y, z);
					IBlockState blockState = null;
					if(schematicBlock != null)
					{
						short blockId = schematicBlock.getSchematicBlockId();
						byte meta = schematicBlock.getSchematicMeta();
						ResourceItem resource = ResourceManager.getResource(resourceMap, blockId, meta);
						if(resource != null) {
							try {
								blockState = resource.getBlock().getStateFromMeta(resource.getMeta());
							} catch(Exception e) {
								blockState = resource.getBlock().getDefaultState();
							}
						}
					}
					
					if(blockState != null)
						blocks.add(blockState);
					else
						blocks.add(Blocks.air.getDefaultState());
				}
			}
		}
	}
	
	@SideOnly(Side.CLIENT)
	public WorldCache(IBlockState blockState) {
		super(saveHandler, new WorldInfo(wSettings, "SchematicBuilder_DummyWorld"), wProvider, p, true);
		this.block = blockState;
		
		this.width = 1;
		this.height = 1;
		this.length = 1;
		
		blocks = new ArrayList<IBlockState>(1);
		blocks.add(block);
	}
	
	public Schematic getSchematic() {
		return schematic;
	}
	
	public IBlockState getBlock() {
		return block;
	}
	
	private int getIndex(int x, int y, int z) {
		return (x*height*length) + (y*length) + z;
	}
	
	@Override
	public IBlockState getBlockState(BlockPos pos) {
		if(pos.getX() < 0 || pos.getY() < 0 || pos.getZ() < 0 || pos.getX() >= width || pos.getY() >= height || pos.getZ() >= length)
			return Blocks.air.getDefaultState();
		
		int index = getIndex(pos.getX(),pos.getY(),pos.getZ());
		return blocks.get(index);
	}
	
	@Override
	public boolean setBlockState(BlockPos pos, IBlockState newState, int flags) {
		int index = getIndex(pos.getX(), pos.getY(), pos.getZ());
		blocks.set(index, newState);
		
		return true;
	}
	
	@Override
	public boolean isAirBlock(BlockPos pos) {
		return blocks.get(getIndex(pos.getX(), pos.getY(), pos.getZ())).getBlock() == Blocks.air;
	}

	@Override
    public boolean isBlockLoaded(BlockPos pos, boolean allowEmpty)
    {
		return true;
    }
	
	@Override
	public TileEntity getTileEntity(BlockPos pos) {
		try {
			IBlockState blockState = getBlockState(pos);
			TileEntity tile = blockState.getBlock().createTileEntity(this, blockState);
			if(tile != null)
				tile.setWorldObj(this);
			return tile;
		} catch (Exception e) {
			//Simply ignore errors, as they will happen if TileEntity requires a valid world
		}
		return null;
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
    @SideOnly(Side.CLIENT)
    public int getCombinedLight(BlockPos pos, int lightValue)
    {
        return EnumSkyBlock.SKY.defaultLightValue;
    }
	
	@Override
    @SideOnly(Side.CLIENT)
    public BiomeGenBase getBiomeGenForCoords(BlockPos pos) {
		return BiomeGenBase.plains;
	}

	@Override
	public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        if (pos.getX() < -30000000 || pos.getZ() < -30000000 || pos.getX() >= 30000000 || pos.getZ() >= 30000000)
            return _default;

        return getBlockState(pos).getBlock().isSideSolid(this, pos, side);
	}

	@Override
	protected IChunkProvider createChunkProvider() {
		return null;
	}

	@Override
	protected int getRenderDistanceChunks() {
		return 6;
	}

}
