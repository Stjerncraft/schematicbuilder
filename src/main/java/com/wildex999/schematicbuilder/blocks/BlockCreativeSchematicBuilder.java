package com.wildex999.schematicbuilder.blocks;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.items.ItemCreativeSchematicBuilder;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;
import com.wildex999.utils.ModLog;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class BlockCreativeSchematicBuilder extends BlockSchematicBuilder {
	public final String name = "Creative Schematic Builder";
	
	public BlockCreativeSchematicBuilder() {
		super(false);
		setUnlocalizedName(name);

		BlockLibrary.register(this, ItemCreativeSchematicBuilder.class);
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack item) {
    	TileEntity te = world.getTileEntity(pos);
    	if(te == null || !(te instanceof TileSchematicBuilder))
    	{
    		ModLog.logger.error("Expected TileSchematicBuilder at " + pos);
    		return;
    	}
    	TileSchematicBuilder tileBuilder = (TileSchematicBuilder) te;
    	
    	tileBuilder.isCreative = true;
    	
		
		super.onBlockPlacedBy(world, pos, state, placer, item);
	}
	
}
