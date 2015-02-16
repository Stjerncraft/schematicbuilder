package com.wildex999.schematicbuilder.blocks;

import javax.swing.JFileChooser;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.gui.SchematicBuilderGui;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;
import com.wildex999.utils.FileChooser;
import com.wildex999.utils.ModLog;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class BlockSchematicBuilder extends BlockBase {
	public final String name = "Schematic Builder";
	
	public BlockSchematicBuilder()
	{
		this.setBlockName(name);
		this.setHardness(1f);
		this.setResistance(3f);
		this.setCreativeTab(CreativeTabs.tabBlock);
		this.setStepSound(Block.soundTypeMetal);
		
		BlockLibrary.register(this);
		registerTile(TileSchematicBuilder.class);
	}
	
	@Override
	public boolean hasTileEntity(int meta)
	{
		return true;
	}
	
	@Override
	public TileEntity createTileEntity(World world, int meta)
	{
		return new TileSchematicBuilder();
	}
	
	@Override
    public boolean onBlockActivated(World world, int blockX, int blockY, int blockZ, EntityPlayer player, int side, float offX, float offY, float offZ)
    {
		player.openGui(ModSchematicBuilder.instance, SchematicBuilderGui.GUI_ID, world, blockX, blockY, blockZ);
        return true;
    }
	
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack item) {
		
        int placerFacing = MathHelper.floor_double((double)(placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        //Facing:
        //0 = South
        //1 = West
        //2 = North
        //3 = East
        world.setBlockMetadataWithNotify(x, y, z, placerFacing, 2);
	}
	
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int metadata)
	{
		TileEntity tile = world.getTileEntity(x, y, z);
		if(tile != null)
		{
			if(tile instanceof IInventory)
				dropInventory((IInventory)tile, world, x, y, z);
		}
		
		super.breakBlock(world, x, y, z, block, metadata);
	}
}
