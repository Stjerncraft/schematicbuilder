package com.wildex999.schematicbuilder.blocks;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.gui.GuiSchematicBuilder;
import com.wildex999.schematicbuilder.items.ItemSchematicBuilder;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;
import com.wildex999.utils.FileChooser;
import com.wildex999.utils.ModLog;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockSchematicBuilder extends BlockBase {
	
	public final String name = "Schematic Builder";
	public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
	
	public BlockSchematicBuilder(boolean register)
	{
		this.setUnlocalizedName(name);
		this.setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
		this.setHardness(1f);
		this.setResistance(3f);
		this.setCreativeTab(CreativeTabs.tabBlock);
		this.setStepSound(Block.soundTypeMetal);
		
		if(register) {
			BlockLibrary.register(this, ItemSchematicBuilder.class);
			registerTile(TileSchematicBuilder.class);
		}
	}
	
	@Override
	protected BlockState createBlockState()
    {
        return new BlockState(this, new IProperty[]{FACING});
    }
	
	/**
     * Convert the given metadata into a BlockState for this Block
     */
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta & 3));
    }

    /**
     * Convert the BlockState into the correct metadata value
     */
    public int getMetaFromState(IBlockState state)
    {
        int i = 0;
        i = i | ((EnumFacing)state.getValue(FACING)).getHorizontalIndex();
        return i;
    }
	
	@Override
	public boolean hasTileEntity(IBlockState state)
	{
		return true;
	}
	
	@Override
	public TileEntity createTileEntity(World world, IBlockState state)
	{
		return new TileSchematicBuilder();
	}
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
		player.openGui(ModSchematicBuilder.instance, GuiSchematicBuilder.GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
        return true;
	};

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack item) {
		EnumFacing placerFacing = EnumFacing.getHorizontal(MathHelper.floor_double((double)(placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3);
		state = state.withProperty(FACING, placerFacing);
        world.setBlockState(pos, state);
		
        //Set the previous Schematic
        if(item.hasTagCompound())
        {
        	TileEntity te = world.getTileEntity(pos);
        	if(te == null || !(te instanceof TileSchematicBuilder))
        	{
        		System.err.println("Expected TileSchematicBuilder at " + pos);
        		return;
        	}
        	TileSchematicBuilder tileBuilder = (TileSchematicBuilder) te;
        	
        	NBTTagCompound nbt = item.getTagCompound();
        	String cachedFile = nbt.getString("cachedSchematicFile");
        	String schematicName = nbt.getString("schematicName");
        	
        	tileBuilder.config.readFromNBT(nbt, tileBuilder);
        	
        	if(ModSchematicBuilder.useEnergy)
        	{
        		tileBuilder.energyStorage.setEnergyStored(nbt.getInteger("energy"));
        	}
        	
        	if(!cachedFile.trim().isEmpty())
        		tileBuilder.onPlaceCached(cachedFile, schematicName);
        }
	};
	
	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state)
	{
		TileEntity tile = world.getTileEntity(pos);
		if(tile != null)
		{
			if(tile instanceof IInventory)
				dropInventory((IInventory)tile, world, pos.getX(), pos.getY(), pos.getZ());
			if(tile instanceof TileSchematicBuilder)
				((TileSchematicBuilder)tile).onBreak();
		}

		super.breakBlock(world, pos, state);
	}
	
	/*
	 * Drop BlockSchematicBuilder with the loaded Schematic on it
	 */
	@Override
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune)
    {
		TileEntity te = world.getTileEntity(pos);
		List<ItemStack> dropItems = super.getDrops(world, pos, state, fortune);
		if(te == null || !(te instanceof TileSchematicBuilder))
		{
			System.err.println("Failed to drop SchematicBuilder TileEntity due to it no longer existing where expected: " + pos);
			return dropItems;
		}
		TileSchematicBuilder tileBuilder = (TileSchematicBuilder) te;
		
		//Add NBT data to item
		if(dropItems.isEmpty()) //Assert: Should not happen
			return dropItems;
		
		ItemStack dropItem = dropItems.get(0);
		if(!dropItem.hasTagCompound())
			dropItem.setTagCompound(new NBTTagCompound());
		NBTTagCompound nbt = dropItem.getTagCompound();
		
		//Write data
		nbt.setString("cachedSchematicFile", tileBuilder.cachedSchematicFile);
		if(tileBuilder.loadedSchematic == null)
			nbt.setString("schematicName", "None");
		else
			nbt.setString("schematicName", tileBuilder.loadedSchematic.name);
		
		tileBuilder.config.writeToNBT(nbt);
		
		if(ModSchematicBuilder.useEnergy)
		{
			nbt.setInteger("energy", tileBuilder.energyStorage.getEnergyStored());
		}
		
		//TODO: Drop Resources as SuperCompressedItem, which can be placed in crafting table to retrieve stacks of the compressed item.
		return dropItems;
    }
	
    @Override
    public boolean removedByPlayer(World world, BlockPos pos, EntityPlayer player, boolean willHarvest)
    {
        if (willHarvest) return true; //If it will harvest, delay deletion of the block until after getDrops
        return super.removedByPlayer(world, pos, player, willHarvest);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity tile)
    {
        super.harvestBlock(world, player, pos, state, tile);
        world.setBlockToAir(pos);
    }
	
	
}
