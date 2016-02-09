package com.wildex999.schematicbuilder.gui.elements;

import java.util.Arrays;
import java.util.Comparator;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.WorldCache;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumWorldBlockLayer;

//Element for rendering a single block in the GUI

public class GuiBlockRenderer extends Gui {

	private Minecraft mc;
	
	private int renderPosX, renderPosY;
	private int renderWidth, renderHeight;
	
	private static WorldCache worldCache;
	private static Tessellator tessellator;
	private IBlockState block;
	
	public boolean rotate = true;
	
	private TextureManager texMan;
	
	private float rot;
	
	public GuiBlockRenderer(Minecraft mc, int posX, int posY, int width, int height, Block block, byte meta) {
		this.mc = mc;
		renderPosX = posX;
		renderPosY = posY;
		renderWidth = width;
		renderHeight = height;
		
		this.block = block.getStateFromMeta(meta);
		
		//Set up rendering
		worldCache = new WorldCache(block, meta);
		texMan = mc.getTextureManager();
		
		if(tessellator == null)
			tessellator = new Tessellator(2097152);
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
		if(worldCache == null || block == null)
			return;
		
        ScaledResolution sr = new ScaledResolution(mc);
        worldCache.setBlockState(new BlockPos(0,0,0), block);
        
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
		
		texMan.bindTexture(TextureMap.locationBlocksTexture);

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glClearDepth(1.0f);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        
		int ambientOcclusion = Minecraft.getMinecraft().gameSettings.ambientOcclusion;
		Minecraft.getMinecraft().gameSettings.ambientOcclusion = 0; //Don't do light level checks
		
		//Prepare render
		GL11.glViewport(renderPosX*sr.getScaleFactor(), mc.displayHeight-((renderPosY*sr.getScaleFactor()) + renderHeight*sr.getScaleFactor()), renderWidth*sr.getScaleFactor(), renderHeight*sr.getScaleFactor());
		//Tessellator.instance.setTranslation(-0.5f, -0.5f, -0.5f);
        
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
		/*try {
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
		}*/
        
        WorldRenderer worldRendererIn = null; //TODO: Create own WorldRenderer, not used by the rest of the game
        try {
        
        BlockRendererDispatcher render = mc.getBlockRendererDispatcher();
        worldRendererIn = tessellator.getWorldRenderer();
        
        //Pre render
        worldRendererIn.begin(7, DefaultVertexFormats.BLOCK);
        worldRendererIn.setTranslation(-0.5,-0.5,-0.5);
        
        //Render
        render.renderBlock(block, new BlockPos(0,0,0), worldCache, worldRendererIn);
        tessellator.draw();
        
        //Post render
        worldRendererIn.setTranslation(0, 0, 0);
		
        } catch(Exception e) {
        	worldCache = null; //Stop rendering
        	try {
        		worldRendererIn.finishDrawing();
        	} catch(Exception ee) {} //A failed render can leave the renderer in a state to crash the game
        	
        	System.out.println("GUI Block rendering failed!");
        	e.printStackTrace();
        }

		//Cleanup state
        Minecraft.getMinecraft().gameSettings.ambientOcclusion = ambientOcclusion;
        GL11.glViewport(0,  0, mc.displayWidth, mc.displayHeight);
        
        GL11.glScalef(1f/scale, 1f/scale, 1f/scale);
        
        //Restore previous values
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
	}
}
