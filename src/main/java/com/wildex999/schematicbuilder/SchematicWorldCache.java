package com.wildex999.schematicbuilder;

import java.util.ArrayList;

import com.wildex999.schematicbuilder.schematic.Schematic;
import com.wildex999.schematicbuilder.schematic.SchematicBlock;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

/*
 * Custom IBlockAccess storing a Schematic for use when rendering preview
 * TODO: Implement World to allow custom TileEntity renderer
 */

public class SchematicWorldCache implements IBlockAccess {

	private Schematic schematic;
	//TODO: Allow for 'loading' into cache gradually(One chunk at a time etc.)
	ArrayList<Block> blocks;
	ArrayList<Byte> metadata;
	
	public SchematicWorldCache(Schematic schematic) {
		this.schematic = schematic;
		
		blocks = new ArrayList<Block>(schematic.getWidth()*schematic.getHeight()*schematic.getLength());
		metadata = new ArrayList<Byte>(schematic.getWidth()*schematic.getHeight()*schematic.getLength());
		
		//Copy over the data
		for(int x = 0; x < schematic.getWidth(); x++)
		{
			for(int y = 0; y < schematic.getHeight(); y++)
			{
				for(int z = 0; z < schematic.getLength(); z++)
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
	
	public Schematic getSchematic() {
		return schematic;
	}
	
	private int getIndex(int x, int y, int z) {
		return (x*schematic.getHeight()*schematic.getLength()) + (y*schematic.getLength()) + z;
	}
	
	@Override
	public Block getBlock(int x, int y, int z) {
		if(x < 0 || y < 0 || z < 0 || x >= schematic.getWidth() || y >= schematic.getHeight() || z >= schematic.getLength())
			return Blocks.air;
		return blocks.get(getIndex(x,y,z));
	}

	@Override
	public TileEntity getTileEntity(int x, int y,int z) {
		return null;
	}

	@Override
	public int getLightBrightnessForSkyBlocks(int x, int y,int z, int p_72802_4_) {
        return EnumSkyBlock.Block.defaultLightValue;
	}

	@Override
	public int getBlockMetadata(int x, int y, int z) {
		if(x < 0 || y < 0 || z < 0 || x >= schematic.getWidth() || y >= schematic.getHeight() || z >= schematic.getLength())
			return 0;
		return metadata.get(getIndex(x,y,z));
	}

	@Override
	public int isBlockProvidingPowerTo(int x, int y,int z, int p_72879_4_) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isAirBlock(int x, int y, int z) {
		//TODO: Actually send a World
		return getBlock(x, y, z).isAir(this, x, y, z);
	}

	@Override
	public BiomeGenBase getBiomeGenForCoords(int x, int z) {
		return BiomeGenBase.sky;
	}

	@Override
	public int getHeight() {
		return schematic.getHeight();
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

}
