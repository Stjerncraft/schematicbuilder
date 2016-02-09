package com.wildex999.schematicbuilder.blocks;

import java.util.ArrayList;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.gui.GuiSchematicBuilder;
import com.wildex999.schematicbuilder.items.ItemSchematicBuilder;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class BlockSchematicCreator extends BlockBase {
public final String name = "Schematic Creator";
	
	public BlockSchematicCreator(boolean register)
	{
		this.setUnlocalizedName(name);
		this.setHardness(1f);
		this.setResistance(3f);
		this.setCreativeTab(CreativeTabs.tabBlock);
		this.setStepSound(Block.soundTypeMetal);
		
		if(register) {
			BlockLibrary.register(this);
			//registerTile(TileSchematicBuilder.class);
		}
	}
	
}
