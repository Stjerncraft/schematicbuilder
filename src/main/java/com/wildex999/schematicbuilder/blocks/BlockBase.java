package com.wildex999.schematicbuilder.blocks;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class BlockBase extends Block {
	protected static final Random rand = new Random(); //TODO: Use something that would stay in sync on both client and server
	
	
	public BlockBase(Material material)
	{
		super(material);
	}
	
	public BlockBase()
	{
		super(Material.rock);
	}
	
	//Get block name without the "tile." tacked on by forge.
	public String getProperName()
	{
		return this.getUnlocalizedName().substring(5);
	}
	
	//Register tile entity, with the ID derived from the block name
	public void registerTile(Class<? extends TileEntity> tile) {
		GameRegistry.registerTileEntity(tile, getTileId());
	}
	
	//Return the tile ID, derived from the block name
	public String getTileId()
	{
		return "TE_" + getProperName();
	}
	
	//Drop everything in the inventory on the ground
	public void dropInventory(IInventory tile, World world, int x, int y, int z) {
		
		for (int item = 0; item < tile.getSizeInventory(); ++item)
		{
			ItemStack itemstack = tile.getStackInSlot(item);

			if (itemstack != null)
			{
				float f = this.rand.nextFloat() * 0.8F + 0.1F;
				float f1 = this.rand.nextFloat() * 0.8F + 0.1F;
				float f2 = this.rand.nextFloat() * 0.8F + 0.1F;
				float f3 = 0.05F;
				
				EntityItem entityitem = new EntityItem(world, (double)((float)x + f), (double)((float)y + f1), (double)((float)z + f2), new ItemStack(itemstack.getItem(), itemstack.stackSize, itemstack.getItemDamage()));

				entityitem.motionX = (double)((float)this.rand.nextGaussian() * f3);
				entityitem.motionY = (double)((float)this.rand.nextGaussian() * f3 + 0.2F);
				entityitem.motionZ = (double)((float)this.rand.nextGaussian() * f3);

				if (itemstack.hasTagCompound())
					entityitem.getEntityItem().setTagCompound((NBTTagCompound)itemstack.getTagCompound().copy());

				world.spawnEntityInWorld(entityitem);
			}
		}
	}
}
