package com.wildex999.schematicbuilder.blocks;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.items.ItemCreativeSchematicBuilder;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class BlockCreativeSchematicBuilder extends BlockSchematicBuilder {
	public final String name = "Creative Schematic Builder";
	
	public BlockCreativeSchematicBuilder() {
		super(false);
		setBlockName(name);
		
		BlockLibrary.register(this, ItemCreativeSchematicBuilder.class);
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack item) {
    	TileEntity te = world.getTileEntity(x, y, z);
    	if(te == null || !(te instanceof TileSchematicBuilder))
    	{
    		System.err.println("Expected TileSchematicBuilder at(xyz) " + x + " " + y + " " + z);
    		return;
    	}
    	TileSchematicBuilder tileBuilder = (TileSchematicBuilder) te;
    	
    	tileBuilder.isCreative = true;
		
		super.onBlockPlacedBy(world, x, y, z, placer, item);
	}
	
}
