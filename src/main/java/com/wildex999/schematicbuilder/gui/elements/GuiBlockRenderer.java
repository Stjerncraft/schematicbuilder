package com.wildex999.schematicbuilder.gui.elements;

import java.util.Arrays;
import java.util.Comparator;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.WorldCache;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;

//Element for rendering a single block in the GUI

public class GuiBlockRenderer extends Gui {

	private Minecraft mc;
	
	private int renderPosX, renderPosY;
	private int renderWidth, renderHeight;
	
	private WorldCache worldCache;
	private Block block;
	private byte meta;
	
	public boolean rotate = true;
	
	private TextureManager texMan;
	private RenderBlocks renderBlocksRi;
	
	private float rot;
	
	public GuiBlockRenderer(Minecraft mc, int posX, int posY, int width, int height, Block block, byte meta) {
		this.mc = mc;
		renderPosX = posX;
		renderPosY = posY;
		renderWidth = width;
		renderHeight = height;
		
		this.block = block;
		this.meta = meta;
		
		//Set up rendering
		worldCache = new WorldCache(block, meta);
		texMan = mc.getTextureManager();
		renderBlocksRi = new RenderBlocks(worldCache);
	}
	
	public void setPosition(int x, int y) {
		renderPosX = x;
		renderPosY = y;
	}
	
	public void setSize(int width, int height) {
		renderWidth = width;
		renderHeight = height;
	}
	
	public void renderBlock() {
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        
		//Camera(Project)
        float farPlaneDistance = (float)((this.mc.gameSettings.renderDistanceChunks + 10) * 16 );
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        float fovy = 85;
        Project.gluPerspective(fovy, (float)renderWidth / (float)renderHeight, 0.05F, farPlaneDistance * 2.0F);
        
        //World(Model)
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        //GL11.glRotatef(0f, 0.0F, 0.0F, 1.0F); //Correct z direction so we don't see inside the blocks
		
		texMan.bindTexture(TextureMap.locationBlocksTexture);

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glClearDepth(1.0f);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        renderBlocksRi.useInventoryTint = true; //renderWithColor
        
		int ambientOcclusion = Minecraft.getMinecraft().gameSettings.ambientOcclusion;
		Minecraft.getMinecraft().gameSettings.ambientOcclusion = 0; //Don't do light level checks
		
		//Prepare render
		GL11.glViewport(renderPosX*sr.getScaleFactor(), mc.displayHeight-((renderPosY*sr.getScaleFactor()) + renderHeight*sr.getScaleFactor()), renderWidth*sr.getScaleFactor(), renderHeight*sr.getScaleFactor());
		Tessellator.instance.setTranslation(-0.5f, -0.5f, -0.5f);
        
        GL11.glTranslatef(0, 0, -1.5f);
        
    	rot += 1f;
    	GL11.glRotatef(30f, 1, 0, 0);
    	if(rotate)
    		GL11.glRotatef(rot, 0, 1, 0);
    	else
    		GL11.glRotatef(45f, 0, 1, 0);
        
        float scale = 1;
        GL11.glScalef(scale, scale, scale);
        
		//Render TileEntity if available
		try {
			if(block.hasTileEntity(meta)) {
				TileEntity tile = worldCache.getTileEntity(0, 0, 0);
				if(tile != null) {
					TileEntityRendererDispatcher.instance.renderTileEntityAt(tile, 0, -1, -1, 0);
				}
			}
		} catch(Exception e) {
			if(ModSchematicBuilder.debug) {
				System.out.println("Failed to render: ");
				e.printStackTrace();
			}
		}

		
		//Render
		Tessellator.instance.startDrawingQuads();	
		renderBlocksRi.renderBlockByRenderType(block, 0, 0, 0);	
		Tessellator.instance.draw(); //Render and reset state

		//Cleanup state
		Minecraft.getMinecraft().gameSettings.ambientOcclusion = ambientOcclusion;
        Tessellator.instance.setTranslation(0, 0, 0);
        GL11.glViewport(0,  0, mc.displayWidth, mc.displayHeight);
        
        GL11.glScalef(1f/scale, 1f/scale, 1f/scale);
        
        //Restore previous values
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
	}
}
