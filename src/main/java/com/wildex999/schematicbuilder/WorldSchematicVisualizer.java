package com.wildex999.schematicbuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.ComparisonChain;
import com.wildex999.schematicbuilder.schematic.Schematic;
import com.wildex999.schematicbuilder.schematic.SchematicBlock;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;
import com.wildex999.utils.ModLog;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderList;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.shader.TesselatorVertexState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class WorldSchematicVisualizer {
	
	public static WorldSchematicVisualizer instance;
	
	private class RenderCache {
		public int chunkX;
		public int chunkY;
		public int chunkZ;
		//public TesselatorVertexState state;
		public int renderListIndex;
		
		public RenderCache(int chunkX, int chunkY, int chunkZ, int renderListIndex) {
			this.chunkX = chunkX;
			this.chunkY = chunkY;
			this.chunkZ = chunkZ;
			//this.state = state;
			this.renderListIndex = renderListIndex;
		}
	}
	private ArrayList<RenderCache> renderChunks = new ArrayList<RenderCache>();
	
	public Schematic cachedSchematic;
	
	private RenderList renderList;
	private int renderListStart;
	private int renderListCurrent;
	private int renderListSize;
	
	
	private int chunkSize = 16;
	private double pPosX, pPosY, pPosZ; //Corrected player position
	private int updateOffset; //Which Chunk to update next
	
	private Field bufferSizeField;
	
	public TileSchematicBuilder vTile;
	public TileSchematicBuilder fTile;
	public int resourceVersion;
	
	public class ProgressRender {
		public boolean render;
		public int tileX, tileY, tileZ;
		public int targetX, targetY, targetZ;
		
		public boolean isTile(TileEntity tile) {
			return tile.xCoord == tileX && tile.yCoord == tileY && tile.zCoord == tileZ;
		}
	};
	public ProgressRender progressRender = new ProgressRender();
	
	public int sortFreq = 120;
	public int currentSortFrame = 0;
	
	
	public WorldSchematicVisualizer() {
		instance = this;
		
		//Set private field public for checking if a chunk has nothing rendered
		try {
			bufferSizeField = ReflectionHelper.findField(Tessellator.class, "rawBufferIndex", "field_147569_p");
		}
		catch (Exception e) {
			ModLog.logger.error("Unable to get 'rawBufferIndex' field from Tessellator. Unable to render preview!");
			bufferSizeField = null;
		}
	}
	
	@SubscribeEvent(priority=EventPriority.HIGHEST)
	public void renderWorldLastEvent(RenderWorldLastEvent event) {
		if((vTile == null || vTile.loadedSchematic == null) && (fTile == null || fTile.loadedSchematic == null))
		{
			clearRenderCache();
			return;
		}
		
		//Corrected player position, taking into account movement since last tick(partial ticks)
		EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        pPosX = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks;
        pPosY = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks;
        pPosZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks;
		
		//Try to reset to a reasonable state
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPushMatrix();
		GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        
		renderVisualize();
		renderFrame();
		renderProgress();
		
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glPopMatrix();
	}
	
	private void renderProgress() {
		if(!progressRender.render)
			return;
		
		Tessellator t = Tessellator.instance;
		t.startDrawingQuads();
		t.setTranslation(progressRender.targetX - pPosX, progressRender.targetY - pPosY, (progressRender.targetZ+1) - pPosZ);
		
		//Draw Cube
		//Front
		t.setColorRGBA(255, 0, 0, 255);
		t.addVertex(1, 0, 1);
		t.addVertex(1, 1, 1);
		t.addVertex(0, 1, 1);
		t.addVertex(0, 0, 1);
		
		//Back
		t.setColorRGBA(255, 0, 0, 255);
		t.addVertex(1, 0, 0);
		t.addVertex(1, 1, 0);
		t.addVertex(0, 1, 0);
		t.addVertex(0, 0, 0);
		
		//Left
		t.setColorRGBA(255, 0, 0, 255);
		t.addVertex(0, 0, 1);
		t.addVertex(0, 1, 1);
		t.addVertex(0, 1, 0);
		t.addVertex(0, 0, 0);
		
		//Right
		t.setColorRGBA(255, 0, 0, 255);
		t.addVertex(1, 0, 1);
		t.addVertex(1, 1, 1);
		t.addVertex(1, 1, 0);
		t.addVertex(1, 0, 0);
		
		//Top
		t.setColorRGBA(255, 0, 0, 255);
		t.addVertex(1, 1, 1);
		t.addVertex(1, 1, 0);
		t.addVertex(0, 1, 0);
		t.addVertex(0, 1, 1);
		
		//Bottom
		t.setColorRGBA(255, 0, 0, 255);
		t.addVertex(1, 0, 1);
		t.addVertex(1, 0, 0);
		t.addVertex(0, 0, 0);
		t.addVertex(0, 0, 1);
		
		//GL11.glEnable(GL11.GL_BLEND);
		//GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		//GL11.glColor4f(1F, 0F, 0F, 0.5F);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDepthMask(false);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		Tessellator.instance.draw();
		Tessellator.instance.setTranslation(0, 0, 0);
		
		GL11.glDepthMask(true);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_CULL_FACE);
		//GL11.glDisable(GL11.GL_BLEND);
	}

	public void renderVisualize() {
		if(vTile == null || vTile.loadedSchematic == null || bufferSizeField == null)
			return;
		
		Schematic schematic = vTile.loadedSchematic;
		Minecraft mc = Minecraft.getMinecraft();
		int ambientOcclusion = mc.gameSettings.ambientOcclusion;
		mc.gameSettings.ambientOcclusion = 0; //Don't do light level checks
		
		int chunkCountX = (int) Math.ceil((double)schematic.getWidth()/chunkSize);
		int chunkCountY = (int) Math.ceil((double)schematic.getHeight()/chunkSize);
		int chunkCountZ = (int) Math.ceil((double)schematic.getLength()/chunkSize);
		
		if(vTile.loadedSchematic != cachedSchematic || resourceVersion != vTile.resourceVersion)
		{
			newRenderCache(chunkCountX*chunkCountY*chunkCountZ);
			cachedSchematic = vTile.loadedSchematic;
			resourceVersion = vTile.resourceVersion;
		}
		
		boolean gotFloor;
		float renderY;
		if(vTile.config.placeFloor && vTile.config.floorBlock != null)
		{
			renderY = (float) ((vTile.yCoord+1) - pPosY);
			gotFloor = true;
		}
		else
		{
			renderY = (float) (vTile.yCoord - pPosY);
			gotFloor = false;
		}
		
		//Prepare rendering
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.5F);
		
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
		
		vTile.updateDirection();
		
		if(vTile.schematicCache == null)
			vTile.schematicCache = new WorldCache(schematic);
		RenderBlocks renderBlocksRi = new RenderBlocks(vTile.schematicCache);
		renderBlocksRi.enableAO = false;
		
		//Render in 16x16x16 chunks to cache
		if(renderChunks.size() < chunkCountX*chunkCountY*chunkCountZ) //First time render
		{
			int updateChunksPerFrame = 5; //How many chunks to render into cache this frame
			int chunksUpdated = 0; 
			boolean stopRender = false;
			
			int skipCount = renderChunks.size();
			
			for(int chunkX = 0; chunkX < chunkCountX && !stopRender; chunkX++)
			{
				for(int chunkY = 0; chunkY < chunkCountY && !stopRender; chunkY++)
				{
					for(int chunkZ = 0; chunkZ < chunkCountZ && !stopRender; chunkZ++)
					{
						if(skipCount-- > 0)
							continue;
						
						int listIndex = renderListCurrent++;
						if(listIndex >= renderListStart+renderListSize)
						{
							System.err.println("Error creating new OpenGL Render list. Not enough indexes!");
							vTile = null;
							clearRenderCache();
							return;
						}
						
						//Store into GL Call List
						GL11.glNewList(listIndex, GL11.GL_COMPILE);
						
						//Render Chunk
						Tessellator.instance.startDrawingQuads();
						Tessellator.instance.disableColor();
						renderChunk(chunkX, chunkY, chunkZ, chunkSize, renderBlocksRi, gotFloor);
						
						try {
							if(bufferSizeField.getInt(Tessellator.instance) > 0)
							{
								chunksUpdated++;
								renderChunks.add(new RenderCache(chunkX, chunkY, chunkZ, listIndex));
							}
							else
								renderChunks.add(null);
						} catch(Exception e) {}
						Tessellator.instance.draw(); //Render and reset state
						
						GL11.glEndList();
						
						if(chunksUpdated >= updateChunksPerFrame)
							stopRender = true;
					}
				}
			}
		}
		
		
		//Sort cached renderChunks
		RenderCache[] sortedCache = new RenderCache[renderChunks.size()];
		for(int i=0; i<renderChunks.size(); i++)
			sortedCache[i] = renderChunks.get(i);
		
		//Sort by distance to player
		Arrays.sort(sortedCache, new Comparator<RenderCache>() {
			@Override
			public int compare(RenderCache c1, RenderCache c2) {
				if(c1 == null && c2 == null)
					return 0;
				else if(c1 == null)
					return -1;
				else if(c2 == null)
					return 1;
				
				int halfChunk = chunkSize/2;
				
				int xDistance = vTile.buildX + (c1.chunkX * chunkSize + halfChunk) - (int)pPosX;
				int yDistance = vTile.buildY + (c1.chunkY * chunkSize + halfChunk) - (int)pPosY;
				int zDistance = vTile.buildZ + (c1.chunkZ * chunkSize + halfChunk) - (int)pPosZ;
				
				int distance1 = xDistance*xDistance + yDistance*yDistance + zDistance*zDistance;
				
				xDistance = vTile.buildX + (c2.chunkX * chunkSize + halfChunk) - (int)pPosX;
				yDistance = vTile.buildY + (c2.chunkY * chunkSize + halfChunk) - (int)pPosY;
				zDistance = vTile.buildZ + (c2.chunkZ * chunkSize + halfChunk) - (int)pPosZ;
				
				int distance2 = xDistance*xDistance + yDistance*yDistance + zDistance*zDistance;
				
				return Integer.compare(distance1, distance2);
			}
		});
		
		updateOffset++;
		if(updateOffset < 0 || updateOffset >= sortedCache.length)
			updateOffset = 0;
		int updateCount = 0;
		
		//Render cached chunks
		renderList.resetList();
		renderList.setupRenderList(0, 0, 0, 0, 0, 0);
		int renderChunkRadius = Minecraft.getMinecraft().gameSettings.renderDistanceChunks;
		for(RenderCache cache : sortedCache)
		{
			if(cache == null)
				continue;
			
			//Check if chunks should render
			float chunkPosX = vTile.buildX + (cache.chunkX * chunkSize + (chunkSize/2));
			float chunkPosY = vTile.buildY + (cache.chunkY * chunkSize + (chunkSize/2));
			float chunkPosZ = vTile.buildZ + (cache.chunkZ * chunkSize + (chunkSize/2));
			
			float dx = (float) (pPosX - chunkPosX);
			float dy = (float) (pPosY - chunkPosY);
			float dz = (float) (pPosZ - chunkPosZ);
			
			float distance = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
			
			if(distance <= renderChunkRadius*chunkSize)
			{
				if(updateCount++ == updateOffset)
				{
					
					GL11.glNewList(cache.renderListIndex, GL11.GL_COMPILE);
					
					//Render Chunk
					Tessellator.instance.startDrawingQuads();
					Tessellator.instance.disableColor();
					renderChunk(cache.chunkX, cache.chunkY, cache.chunkZ, chunkSize, renderBlocksRi, gotFloor);
					Tessellator.instance.draw(); //Render and reset state
					
					GL11.glEndList();
				}
				
				//Add to list of lists to render
				renderList.addGLRenderList(cache.renderListIndex);
				
			}
		}
		
		GL11.glPushMatrix();
		GL11.glTranslatef(vTile.buildX - (float)pPosX, renderY, (vTile.buildZ+1) - (float)pPosZ);
		
		renderList.callLists();
		
		mc.gameSettings.ambientOcclusion = ambientOcclusion;
		
		GL11.glPopMatrix();
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	protected void renderChunk(int chunkX, int chunkY, int chunkZ, int chunkSize, RenderBlocks renderBlocksRi, boolean gotFloor) {
		Schematic schematic = vTile.loadedSchematic;
		
		for(int x = chunkX*chunkSize; x < schematic.getWidth() && x < (chunkX+1) * chunkSize; x++)
		{
			for(int y = chunkY*chunkSize; y < schematic.getHeight() && y < (chunkY+1) * chunkSize; y++)
			{
				for(int z = chunkZ*chunkSize; z < schematic.getLength() && z < (chunkZ+1) * chunkSize; z++)
				{
					if(y >= 0)
					{
						SchematicBlock block = schematic.getBlock(x, y, z);
						if(block == null)
							continue;
						
						ResourceItem resource = ResourceManager.getResource(vTile.resources, block.getSchematicBlockId(), block.getSchematicMeta());
						if(resource == null)
							continue;

						Block realBlock = resource.getBlock();
						if(realBlock == null || realBlock.getMaterial() == Material.air)
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
						renderBlocksRi.renderBlockByRenderType((Block)Block.blockRegistry.getObjectById(vTile.config.floorBlock.getBlockId()), x, y, z);
					}
				}
			}
		}
	}
	
	private void newRenderCache(int size) {
		if(renderChunks.size() > 0)
			clearRenderCache();
		renderList = new RenderList();
		renderListStart = GL11.glGenLists(size);
		renderListCurrent = renderListStart;
		
		if(renderListStart == 0)
		{
			System.err.println("Unable to create OpenGL render list.");
			renderListSize = 0;
		}
		else
			renderListSize = size;
	}
	
	private void clearRenderCache() {
		cachedSchematic = null;
		renderChunks.clear();
		renderList = null;
		if(renderListSize > 0)
		{
			GL11.glDeleteLists(renderListStart, renderListSize);
			renderListStart = -1;
			renderListCurrent = -1;
			renderListSize = 0;
		}
	}
	
	public void renderFrame() {
		if(fTile == null || fTile.loadedSchematic == null)
			return;
		
		Schematic schematic = fTile.loadedSchematic;
		
		Minecraft mc = Minecraft.getMinecraft();
		EntityClientPlayerMP player = mc.thePlayer;
		
		fTile.updateDirection();
		
		Tessellator.instance.startDrawingQuads();
		if(fTile.config.placeFloor && fTile.config.floorBlock != null)
			Tessellator.instance.setTranslation(fTile.buildX - pPosX, (fTile.yCoord+1) - pPosY, (fTile.buildZ+1) - pPosZ);
		else
			Tessellator.instance.setTranslation(fTile.buildX - pPosX, fTile.yCoord - pPosY, (fTile.buildZ+1) - pPosZ);
		Tessellator t = Tessellator.instance;

		//Draw Cube
		//Front
		t.setColorRGBA(255, 255, 255, 128);
		t.addVertex(schematic.getWidth(), 0, schematic.getLength());
		t.addVertex(schematic.getWidth(), schematic.getHeight(), schematic.getLength());
		t.addVertex(0, schematic.getHeight(), schematic.getLength());
		t.addVertex(0, 0, schematic.getLength());
		
		//Back
		t.setColorRGBA(255, 255, 0, 128);
		t.addVertex(schematic.getWidth(), 0, 0);
		t.addVertex(schematic.getWidth(), schematic.getHeight(), 0);
		t.addVertex(0, schematic.getHeight(), 0);
		t.addVertex(0, 0, 0);
		
		//Left
		t.setColorRGBA(255, 0, 255, 128);
		t.addVertex(0, 0, schematic.getLength());
		t.addVertex(0, schematic.getHeight(), schematic.getLength());
		t.addVertex(0, schematic.getHeight(), 0);
		t.addVertex(0, 0, 0);
		
		//Right
		t.setColorRGBA(0, 255, 255, 128);
		t.addVertex(schematic.getWidth(), 0, schematic.getLength());
		t.addVertex(schematic.getWidth(), schematic.getHeight(), schematic.getLength());
		t.addVertex(schematic.getWidth(), schematic.getHeight(), 0);
		t.addVertex(schematic.getWidth(), 0, 0);
		
		//Top
		t.setColorRGBA(128, 255, 255, 128);
		t.addVertex(schematic.getWidth(), schematic.getHeight(), schematic.getLength());
		t.addVertex(schematic.getWidth(), schematic.getHeight(), 0);
		t.addVertex(0, schematic.getHeight(), 0);
		t.addVertex(0, schematic.getHeight(), schematic.getLength());
		
		//Bottom
		t.setColorRGBA(255, 128, 128, 128);
		t.addVertex(schematic.getWidth(), 0, schematic.getLength());
		t.addVertex(schematic.getWidth(), 0, 0);
		t.addVertex(0, 0, 0);
		t.addVertex(0, 0, schematic.getLength());
		
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		//GL11.glColor4f(1F, 0F, 0F, 0.5F);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDepthMask(false);
		
		Tessellator.instance.draw();
		Tessellator.instance.setTranslation(0, 0, 0);
		
		GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_BLEND);

	}
}
