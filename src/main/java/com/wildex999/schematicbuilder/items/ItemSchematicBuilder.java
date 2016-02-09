package com.wildex999.schematicbuilder.items;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemSchematicBuilder extends ItemBlock {

	public ItemSchematicBuilder(Block block) {
		super(block);
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer player, List textLines, boolean extraInfo)
	{
		String schematicName = "";
		int energy = 0;
		
		if(itemStack.hasTagCompound())
		{
			NBTTagCompound nbt = itemStack.getTagCompound();
			schematicName = nbt.getString("schematicName");
			energy = nbt.getInteger("energy");
		}
		
		textLines.add("Schematic: " + schematicName);
		textLines.add("Energy: " + energy + " RF");
	}

}
