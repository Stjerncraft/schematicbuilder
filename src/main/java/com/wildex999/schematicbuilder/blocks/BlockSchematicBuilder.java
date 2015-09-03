package com.wildex999.schematicbuilder.blocks;

import java.util.ArrayList;

import javax.swing.JFileChooser;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.gui.GuiSchematicBuilder;
import com.wildex999.schematicbuilder.items.ItemSchematicBuilder;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;
import com.wildex999.utils.FileChooser;
import com.wildex999.utils.ModLog;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
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
		
		BlockLibrary.register(this, ItemSchematicBuilder.class);
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
		player.openGui(ModSchematicBuilder.instance, GuiSchematicBuilder.GUI_ID, world, blockX, blockY, blockZ);
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
        
        //Set the previous Schematic
        if(item.hasTagCompound())
        {
        	TileEntity te = world.getTileEntity(x, y, z);
        	if(te == null || !(te instanceof TileSchematicBuilder))
        	{
        		System.err.println("Expected TileSchematicBuilder at(xyz) " + x + " " + y + " " + z);
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
	}
	
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int metadata)
	{
		TileEntity tile = world.getTileEntity(x, y, z);
		if(tile != null)
		{
			if(tile instanceof IInventory)
				dropInventory((IInventory)tile, world, x, y, z);
			if(tile instanceof TileSchematicBuilder)
				((TileSchematicBuilder)tile).onBreak();
		}
		
		super.breakBlock(world, x, y, z, block, metadata);
	}
	
	/*
	 * Drop BlockSchematicBuilder with the loaded Schematic on it
	 */
	@Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune)
    {
		TileEntity te = world.getTileEntity(x, y, z);
		ArrayList<ItemStack> dropItems = super.getDrops(world, x, y, z, metadata, fortune);
		if(te == null || !(te instanceof TileSchematicBuilder))
		{
			System.err.println("Failed to drop SchematicBuilder TileEntity due to it no longer existing where expected(xyz): " + x + " " + y + " " + z);
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
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest)
    {
        if (willHarvest) return true; //If it will harvest, delay deletion of the block until after getDrops
        return super.removedByPlayer(world, player, x, y, z, willHarvest);
    }
    /**
     * Called when the player destroys a block with an item that can harvest it. (i, j, k) are the coordinates of the
     * block and l is the block's subtype/damage.
     */
    @Override
    public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int meta)
    {
        super.harvestBlock(world, player, x, y, z, meta);
        world.setBlockToAir(x, y, z);
    }
	
	
}
