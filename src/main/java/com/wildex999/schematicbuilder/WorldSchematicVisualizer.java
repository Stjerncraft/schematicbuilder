package com.wildex999.schematicbuilder;

import org.lwjgl.opengl.GL11;

import com.wildex999.schematicbuilder.schematic.Schematic;
import com.wildex999.schematicbuilder.schematic.SchematicBlock;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class WorldSchematicVisualizer {
	
	public static WorldSchematicVisualizer instance;
	
	public TileSchematicBuilder vTile;
	public TileSchematicBuilder fTile;
	
	public WorldSchematicVisualizer() {
		instance = this;
	}
	
	@SubscribeEvent(priority=EventPriority.HIGHEST)
	public void renderWorldLastEvent(RenderWorldLastEvent event) {
		if((vTile == null || vTile.loadedSchematic == null) && (fTile == null || fTile.loadedSchematic == null))
			return;
		
		//Try to reset to a reasonable state
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        
		renderVisualize();
		renderFrame();
		
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glPopMatrix();
	}
	
	public void renderVisualize() {
		if(vTile == null || vTile.loadedSchematic == null)
			return;
		
		Schematic schematic = vTile.loadedSchematic;
		
		if(vTile.schematicCache == null)
			vTile.schematicCache = new SchematicWorldCache(schematic);
		RenderBlocks renderBlocksRi = new RenderBlocks(vTile.schematicCache);
		
		Minecraft mc = Minecraft.getMinecraft();
		EntityClientPlayerMP player = mc.thePlayer;
		int ambientOcclusion = Minecraft.getMinecraft().gameSettings.ambientOcclusion;
		mc.gameSettings.ambientOcclusion = 0; //Don't do light level checks
		
		
		vTile.updateDirection();

		Tessellator.instance.startDrawingQuads();
		Tessellator.instance.disableColor();
		boolean gotFloor;
		if(vTile.config.placeFloor && vTile.config.floorBlock != null)
		{
			Tessellator.instance.setTranslation(vTile.buildX - player.posX, (vTile.yCoord+1) - player.posY, (vTile.buildZ+1) - player.posZ);
			gotFloor = true;
		}
		else
		{
			Tessellator.instance.setTranslation(vTile.buildX - player.posX, vTile.yCoord - player.posY, (vTile.buildZ+1) - player.posZ);
			gotFloor = false;
		}
		renderBlocksRi.enableAO = false;

		for(int x = 0; x < schematic.getWidth(); x++)
		{
			int y;
			if(gotFloor)
				y = -1;
			else
				y = 0;
			for(; y < schematic.getHeight(); y++)
			{
				for(int z = 0; z < schematic.getLength(); z++)
				{
					if(y >= 0)
					{
						SchematicBlock block = schematic.getBlock(x, y, z);
						if(block == null)
							continue;

						Block realBlock = block.getBlock();
						if(realBlock.getMaterial() == Material.air)
							continue;

						int worldY = y;
						if(gotFloor)
							worldY++;

						Block worldBlock = vTile.getWorldObj().getBlock(vTile.buildX+x, vTile.yCoord+worldY, (vTile.buildZ+1)+z);
						if(worldBlock != Blocks.air)
							continue;

						renderBlocksRi.renderBlockByRenderType(realBlock, x, y, z);
					}
					else
					{
						Block worldBlock = vTile.getWorldObj().getBlock(vTile.buildX+x, vTile.yCoord+(y+1), (vTile.buildZ+1)+z);
						if(worldBlock != Blocks.air)
							continue;
						renderBlocksRi.renderBlockByRenderType(vTile.config.floorBlock.getBlock(), x, y, z);
					}
				}
			}
		}

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.5F);
		Tessellator.instance.draw();
		GL11.glDisable(GL11.GL_BLEND);
		Tessellator.instance.setTranslation(0, 0, 0);

		Minecraft.getMinecraft().gameSettings.ambientOcclusion = ambientOcclusion;
	}
	
	public void renderFrame() {
		if(fTile == null || fTile.loadedSchematic == null)
			return;
		
		Schematic schematic = fTile.loadedSchematic;
		
		if(fTile.schematicCache == null)
			fTile.schematicCache = new SchematicWorldCache(schematic);
		RenderBlocks renderBlocksRi = new RenderBlocks(fTile.schematicCache);
		
		Minecraft mc = Minecraft.getMinecraft();
		EntityClientPlayerMP player = mc.thePlayer;
		
		fTile.updateDirection();
		
		Tessellator.instance.startDrawingQuads();
		if(fTile.config.placeFloor && fTile.config.floorBlock != null)
			Tessellator.instance.setTranslation(fTile.buildX - player.posX, (fTile.yCoord+1) - player.posY, (fTile.buildZ+1) - player.posZ);
		else
			Tessellator.instance.setTranslation(fTile.buildX - player.posX, fTile.yCoord - player.posY, (fTile.buildZ+1) - player.posZ);
		Tessellator t = Tessellator.instance;

		t.setColorRGBA(255, 255, 255, 128);
		//Draw Cube
		//Front
		t.addVertex(schematic.getWidth(), 0, schematic.getLength());
		t.addVertex(schematic.getWidth(), schematic.getHeight(), schematic.getLength());
		t.addVertex(0, schematic.getHeight(), schematic.getLength());
		t.addVertex(0, 0, schematic.getLength());
		
		//Back
		t.addVertex(schematic.getWidth(), 0, 0);
		t.addVertex(schematic.getWidth(), schematic.getHeight(), 0);
		t.addVertex(0, schematic.getHeight(), 0);
		t.addVertex(0, 0, 0);
		
		//Left
		t.addVertex(0, 0, schematic.getLength());
		t.addVertex(0, schematic.getHeight(), schematic.getLength());
		t.addVertex(0, schematic.getHeight(), 0);
		t.addVertex(0, 0, 0);
		
		//Right
		t.addVertex(schematic.getWidth(), 0, schematic.getLength());
		t.addVertex(schematic.getWidth(), schematic.getHeight(), schematic.getLength());
		t.addVertex(schematic.getWidth(), schematic.getHeight(), 0);
		t.addVertex(schematic.getWidth(), 0, 0);
		
		//Top
		t.addVertex(schematic.getWidth(), schematic.getHeight(), schematic.getLength());
		t.addVertex(schematic.getWidth(), schematic.getHeight(), 0);
		t.addVertex(0, schematic.getHeight(), 0);
		t.addVertex(0, schematic.getHeight(), schematic.getLength());
		
		//Bottom
		t.addVertex(schematic.getWidth(), 0, schematic.getLength());
		t.addVertex(schematic.getWidth(), 0, 0);
		t.addVertex(0, 0, 0);
		t.addVertex(0, 0, schematic.getLength());
		
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4f(1F, 0F, 0F, 0.5F);
		GL11.glDisable(GL11.GL_CULL_FACE);
		Tessellator.instance.draw();
		Tessellator.instance.setTranslation(0, 0, 0);
		GL11.glDisable(GL11.GL_BLEND);

	}
}
