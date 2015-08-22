package com.wildex999.schematicbuilder.gui;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.ChunkCache;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.SchematicWorldCache;
import com.wildex999.schematicbuilder.WorldSchematicVisualizer;
import com.wildex999.schematicbuilder.blocks.BlockLibrary;
import com.wildex999.schematicbuilder.gui.elements.GuiButtonCustom;
import com.wildex999.schematicbuilder.gui.elements.GuiButtonStretched;
import com.wildex999.schematicbuilder.gui.elements.GuiLabel;
import com.wildex999.schematicbuilder.gui.elements.GuiScreenExt;
import com.wildex999.schematicbuilder.gui.elements.IGuiTabEntry;
import com.wildex999.schematicbuilder.network.MessageActionSchematicBuilder;
import com.wildex999.schematicbuilder.network.MessageBase;
import com.wildex999.schematicbuilder.network.MessageActionSchematicBuilder.ActionType;
import com.wildex999.schematicbuilder.schematic.Schematic;
import com.wildex999.schematicbuilder.schematic.SchematicBlock;
import com.wildex999.schematicbuilder.tiles.BuilderState;

import cpw.mods.fml.client.config.GuiCheckBox;
import cpw.mods.fml.client.config.GuiSlider;

public class GuiSchematicBuilderMain extends GuiScreenExt implements IGuiTabEntry {
	
	public static final ResourceLocation backgroundImage = new ResourceLocation(ModSchematicBuilder.MODID, "textures/gui/schematic_builder.png");
	
	private static final short backgroundWidth = 232;
	private static final short backgroundHeight = 202;
	
	public static boolean renderSchematic = true;
	public static boolean rotateSchematic = true;
	public static float scaleSchematic = 1.0f;
	
	private int renderPosX, renderPosY;
	private int renderWidth, renderHeight;
	private long prevFrameTime = 0;
	
	private GuiLabel labelContainerName;
	private GuiLabel labelStatus;
	private GuiLabel labelStatusContent;
	private GuiLabel labelSchematicName;
	private GuiLabel labelSchematicAuthor;
	private GuiLabel labelSchematicSize;
	private GuiLabel labelPassCount;
	
	private GuiCheckBox checkBoxFloor;
	private GuiCheckBox checkBoxAir;
	
	private GuiSlider sliderRenderScale;
	
	private GuiButton buttonBuild;
	private GuiButtonStretched buttonVisualize; //Render preview in world
	private GuiButtonStretched buttonFrame; //Render frame of Schematic to world
	private GuiButtonStretched buttonAutoRender; //Render loaded Schematic
	private GuiButtonStretched buttonRotateRender; //Rotate renderered Schematic
	private GuiButtonStretched buttonPass1;
	private GuiButtonStretched buttonPass2;
	private GuiButtonStretched buttonPass3;

	private String stringBuild = "Build";
	private String stringStop = "Stop";
	
	private GuiSchematicBuilder.GUI gui;
	
	private GuiButtonCustom tabButton;
	private int tabId;
	
	public GuiSchematicBuilderMain(GuiSchematicBuilder.GUI gui) {
		this.gui = gui;
	}
	
	
	@Override
	public void onTabActivated() {
	}
	
	@Override
	public void onTabDeactivated() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void updateGui() {
		BuilderState state = gui.tile.state;
		
		//Revert to defaults
		buttonBuild.enabled = false;
		buttonBuild.displayString = stringBuild;
		
		labelSchematicName.label = "Schematic: " + gui.tile.schematicName;
		labelSchematicAuthor.label = "Author: " + gui.tile.schematicAuthor;
		labelSchematicSize.label = "Size(X | Y | Z): " + gui.tile.schematicWidth + " | " + gui.tile.schematicHeight + " | " + gui.tile.schematicLength;
		
		buttonPass1.toggled = false;
		buttonPass2.toggled = false;
		buttonPass3.toggled = false;
		if(gui.tile.config.passCount == 1)
			buttonPass1.toggled = true;
		else if(gui.tile.config.passCount == 2)
			buttonPass2.toggled = true;
		else
			buttonPass3.toggled = true;
			
		checkBoxFloor.setIsChecked(gui.tile.config.placeFloor);
		checkBoxAir.setIsChecked(gui.tile.config.placeAir);
		
		if(WorldSchematicVisualizer.instance.vTile == gui.tile)
			buttonVisualize.toggled = true;
		else
			buttonVisualize.toggled = false;
		
		if(WorldSchematicVisualizer.instance.fTile == gui.tile)
			buttonFrame.toggled = true;
		else
			buttonFrame.toggled = false;
		
		sliderRenderScale.setValue(scaleSchematic);
		
		if(renderSchematic)
			buttonAutoRender.toggled = true;
		else
			buttonAutoRender.toggled = false;
		
		if(rotateSchematic)
			buttonRotateRender.toggled = true;
		else
			buttonRotateRender.toggled = false;
		
		
		switch(state) {
		case IDLE:
			labelStatusContent.label = "Idle";
			labelStatusContent.color = gui.colorOk;
			break;
		case CHOOSINGLOCAL:
			labelStatusContent.label = "Choosing file...";
			labelStatusContent.color = gui.colorOk;
			break;
		case READING:
			labelStatusContent.label = "Reading file: " + gui.tile.filePath;
			labelStatusContent.color = gui.colorOk;
			break;
		case LOADING:
			labelStatusContent.label = "Loading Schematic...";
			labelStatusContent.color = gui.colorOk;
			break;
		case UPLOADING:
			labelStatusContent.label = "Uploading (" + gui.tile.message + ")";
			labelStatusContent.color = gui.colorOk;
			break;
		case DOWNLOADING:
			labelStatusContent.label = "Downloading (" + gui.tile.message + ")";
			labelStatusContent.color = gui.colorOk;
			break;
		case READY:
			labelStatusContent.label = "Ready";
			labelStatusContent.color = gui.colorOk;
			buttonBuild.enabled = true;
			break;
		case BUILDING:
			labelStatusContent.label = "Building(" + gui.tile.message + ")";
			labelStatusContent.color = gui.colorOk;
			buttonBuild.displayString = stringStop;
			buttonBuild.enabled = true;
			break;
		case STOPPED:
			labelStatusContent.label = "Stopped(" + gui.tile.message + ")";
			labelStatusContent.color = gui.colorOk; //TODO: Color warning?
			buttonBuild.enabled = true;
			break;
		case DONE:
			labelStatusContent.label = "Done";
			labelStatusContent.color = gui.colorOk;
			break;
		case ERROR:
			labelStatusContent.label = "E: " + gui.tile.message;
			labelStatusContent.color = gui.colorError;
			break;
		case NEEDRESOURCES:
			labelStatusContent.label = "Missing Resource: " + gui.tile.message;
			labelStatusContent.color = gui.colorError;
			buttonBuild.displayString = stringStop;
			buttonBuild.enabled = true;
			break;
		default:
			labelStatusContent.label = "Unknown State: " + state + "(" + gui.tile.message + ")";
			labelStatusContent.color = gui.colorError;
		}
	}
	
	public GuiScreen getGui() {
		return this;
	}
	
	@Override
	public void setWorldAndResolution(Minecraft mc, int width, int height) {
		super.setWorldAndResolution(mc, width, height);
		
		guiLeft = (width-backgroundWidth)/2;
		guiTop = (height-backgroundHeight)/2;

		labelContainerName = new GuiLabel(gui.tile.getInventory().getInventoryName(), this.guiLeft + 50, this.guiTop + 5, gui.colorText);
		labelStatus = new GuiLabel("Status:", guiLeft + 5, guiTop + 15, gui.colorText);
		labelStatusContent = new GuiLabel("Idle", guiLeft + 43, guiTop + 15, gui.colorOk);
		labelSchematicName = new GuiLabel("Schematic: None", guiLeft + 5, guiTop + 25, gui.colorText);
		labelSchematicAuthor = new GuiLabel("Author: None", guiLeft + 5, guiTop + 35, gui.colorText);
		labelSchematicSize = new GuiLabel("Size(X | Y | Z): 0 | 0 | 0", guiLeft + 5, guiTop + 45, gui.colorText);
		labelPassCount = new GuiLabel("Passes to run:", guiLeft + 110, guiTop + 64, gui.colorText);
		
		buttonBuild = new GuiButton(13, guiLeft + 145, guiTop + 170, 80, 20, stringBuild);
		buttonAutoRender = new GuiButtonStretched(0, guiLeft + 55, guiTop + 64, 45, 10, "Render");
		buttonRotateRender = new GuiButtonStretched(0, guiLeft + 55, guiTop + 74, 45, 10, "Rotate");
		buttonVisualize = new GuiButtonStretched(0, guiLeft + 5, guiTop + 54, 50, 10, "Visualize");
		buttonFrame = new GuiButtonStretched(0, guiLeft + 55, guiTop + 54, 50, 10, "Frame");
		buttonPass1 = new GuiButtonStretched(0, guiLeft + 110, guiTop + 72, 40, 13, "1 Pass");
		buttonPass2 = new GuiButtonStretched(0, guiLeft + 150, guiTop + 72, 40, 13, "2 Pass");
		buttonPass3 = new GuiButtonStretched(0, guiLeft + 190, guiTop + 72, 40, 13, "3 Pass");
		buttonPass3.enabled = false;
		
		checkBoxFloor = new GuiCheckBox(0, guiLeft + 146, guiTop + 86, "Place Floor", true);
		checkBoxAir = new GuiCheckBox(0, guiLeft + 146, guiTop + 96, "Place Air", true);
		
		sliderRenderScale = new GuiSlider(0, guiLeft + 5, guiTop + 64, 50, 20, "Zoom: ", "", 1f, 5f, scaleSchematic, true, true);
		
		buttonList.clear();
		buttonList.add(buttonBuild);
		buttonList.add(buttonAutoRender);
		buttonList.add(buttonRotateRender);
		buttonList.add(buttonVisualize);
		buttonList.add(buttonFrame);
		buttonList.add(checkBoxFloor);
		buttonList.add(checkBoxAir);
		buttonList.add(sliderRenderScale);
		buttonList.add(buttonPass1);
		buttonList.add(buttonPass2);
		buttonList.add(buttonPass3);
		
        renderPosX = guiLeft + 8;
        renderPosY = guiTop + 86;
        renderWidth = 133;
        renderHeight = 107;
        
        prevFrameTime = System.currentTimeMillis();
        
        updateGui();
	}
	
	@Override
    public void drawDefaultBackground()
    {
		return;
    }
	
	@Override
	public void drawGuiContainerBackgroundLayer(float par1, int mouseX, int mouseY) {
		this.mc.getTextureManager().bindTexture(backgroundImage);
		
		this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, backgroundWidth, backgroundHeight);

		labelContainerName.draw(fontRendererObj);
		labelStatus.draw(fontRendererObj);
		labelStatusContent.draw(fontRendererObj);
		labelSchematicName.draw(fontRendererObj);
		labelSchematicAuthor.draw(fontRendererObj);
		labelSchematicSize.draw(fontRendererObj);
		labelPassCount.draw(fontRendererObj);
		
		//Draw Rendering background
		this.drawRect(renderPosX, renderPosY, renderPosX+renderWidth, renderPosY+renderHeight, 0xFF000000);
	}
	
	public float rot = 0.0f;
	public float zl = -15.0f;
	
	@Override
	public void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		/*ItemStack itemStack = new ItemStack(Blocks.redstone_wire);
		if(itemStack == null)
		{
			System.out.println("No itemstack to render");
			
		}*/
		//RenderItem.getInstance().renderItemIntoGUI(fontRendererObj, mc.getTextureManager(), itemStack, 10, 10);
		
		//IDEA: Drop block we want to render + all edges(6 blocks) and then ask it to render at a custom location(Let's say we start from 0,64,0)
		//And try to ask it to ignore lighting.
		//But first, try only rendering the one block without calling update and enforcing metadata.
		

		
		TextureManager texMan = mc.getTextureManager();
		boolean renderWithColor = true;
		
		Schematic schematic = gui.tile.loadedSchematic;
		if(schematic != null && renderSchematic)
		{
			if(gui.tile.schematicCache == null)
				gui.tile.schematicCache = new SchematicWorldCache(schematic);
			RenderBlocks renderBlocksRi = new RenderBlocks(gui.tile.schematicCache);
			renderBlocksRi.enableAO = false;
			renderBlocks(texMan, renderBlocksRi, schematic);
			//gui.tile.getWorldObj().setBlock(gui.tile.xCoord, gui.tile.yCoord, gui.tile.zCoord, BlockLibrary.schematicBuilder);
			//gui.tile.getWorldObj().setTileEntity(gui.tile.xCoord, gui.tile.yCoord, gui.tile.zCoord, gui.tile);
		}
		
		//Draw tooltips
		ArrayList<String> textList = new ArrayList<String>();
		
		if(buttonPass1.isOver(mouseX, mouseY))
		{
			textList.add("Will likely have blocks missing or incorrect");
			textList.add("state due to neighbour blocks while placing.");
			textList.add("Affects things like torches and redstone!");
		}
		else if(buttonPass2.isOver(mouseX, mouseY))
		{
			textList.add("Will do additional second pass to fix");
			textList.add("missing or incorrect blocks.");
			textList.add("Second pass will run faster than first pass.");
		}
		else if(buttonPass3.isOver(mouseX, mouseY))
		{
			textList.add("Will not place (known) fluids during the first 2 passes,");
			textList.add("saving them for the third pass.");
			textList.add("Stops fluids running out and damaging surroundings");
			textList.add("during the first two passes from missing blocks.");
		}
		else if(mouseX >= checkBoxFloor.xPosition && mouseY >= checkBoxFloor.yPosition && mouseX < checkBoxFloor.xPosition + checkBoxFloor.width && mouseY < checkBoxFloor.yPosition + checkBoxFloor.height)
		{
			textList.add("If checked, will build a floor below the Schematic construction.");
			textList.add("Note, this will move the construction up by one!");
		}
		else if(mouseX >= checkBoxAir.xPosition && mouseY >= checkBoxAir.yPosition && mouseX < checkBoxAir.xPosition + checkBoxAir.width && mouseY < checkBoxAir.yPosition + checkBoxAir.height)
		{
			textList.add("If checked, will place air blocks from Schematic.");
		}
		
		this.drawHoveringText(textList, mouseX-guiLeft, mouseY-guiTop, fontRendererObj);
	}
	
	private void renderBlocks(TextureManager texMan, RenderBlocks renderBlocksRi, Schematic schematic) {
		int l;
        float f;
        float f3;
        float f4;
		
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        
		//Camera(Project)
        float farPlaneDistance = (float)(this.mc.gameSettings.renderDistanceChunks * 16);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        float fovy = 85;
        Project.gluPerspective(fovy, (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, farPlaneDistance * 2.0F);
        
        //World(Model)
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glRotatef(0f, 0.0F, 0.0F, 1.0F); //Correct z direction so we don't see inside the blocks
        
        GL11.glTranslatef(0, 0, -4f);
		
		texMan.bindTexture(TextureMap.locationBlocksTexture);
        //GL11.glEnable(GL11.GL_ALPHA_TEST);

        zl -= 2f;
        //GL11.glTranslatef((float)(posX - 2), (float)(posY + 3), -3.0F + this.zLevel + zl);
        /*GL11.glScalef(10.0F, 10.0F, 10.0F);
        GL11.glTranslatef(1.0F, 0.5F, 1.0F);
        GL11.glScalef(1.0F, 1.0F, -1.0F);
        GL11.glRotatef(210.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);*/
        //l = itemStack.getItem().getColorFromItemStack(itemStack, 0);
        l = 16777215;
        f3 = (float)(l >> 16 & 255) / 255.0F;
        f4 = (float)(l >> 8 & 255) / 255.0F;
        f = (float)(l & 255) / 255.0F;

        if (true) //renderWithColor
        {
            GL11.glColor4f(f3, f4, f, 1.0F);
        }

        //rot+= 0.5f;
        //GL11.glRotatef(rot, 0.0F, 1.0F, 0.0F);
        //GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glClearDepth(1.0f);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        //GL11.glFrontFace(GL11.GL_CW);
        renderBlocksRi.useInventoryTint = true; //renderWithColor
        Tessellator.instance.startDrawingQuads();
        
        //System.out.println("Rendersize: " + schematic.getWidth() + " " + schematic.getHeight() + " " + schematic.getLength());
        //System.out.println("RenderAllSides: " + renderBlocksRi.renderAllFaces);
		int ambientOcclusion = Minecraft.getMinecraft().gameSettings.ambientOcclusion;
		Minecraft.getMinecraft().gameSettings.ambientOcclusion = 0; //Don't do light level checks
        
        //TODO: Split into chunks and do per chunk frustrum and distance check
		//TODO: Render to texture and only re-render when rotating
		//TODO: Render one chunk per draw call(Or less?)
		

		Tessellator.instance.setTranslation(-schematic.getWidth()/2, -schematic.getHeight()/2, -schematic.getLength()/2);
		int maxSize = 0;
		if(schematic.getWidth() > maxSize)
			maxSize = schematic.getWidth();
		if(schematic.getHeight() > maxSize)
			maxSize = schematic.getHeight();
		if(schematic.getLength() > maxSize)
			maxSize = schematic.getLength();
		
		for(int x = 0; x < schematic.getWidth(); x++)
		{
			for(int y = 0; y < schematic.getHeight(); y++)
			{
				for(int z = 0; z < schematic.getLength(); z++)
				{
					SchematicBlock block = schematic.getBlock(x, y, z);
					if(block == null)
						continue;
					Block realBlock = block.getBlock();
					if(realBlock.getMaterial() == Material.air)
						continue;
					
					//gui.tile.getWorldObj().setBlock(worldX, worldY, worldZ, realBlock, block.metaData, 4); //Set new block without notify and without re-render
					renderBlocksRi.renderBlockByRenderType(realBlock, x, y, z);
				}
			}
		}
        
        Minecraft.getMinecraft().gameSettings.ambientOcclusion = ambientOcclusion;
        
        //GL11.glTranslatef(-(gui.tile.xCoord+schematic.getWidth()/2), -(gui.tile.yCoord+schematic.getHeight()/2), -(gui.tile.zCoord+(schematic.getLength()*2)));
        renderBlocksRi.useInventoryTint = true;
        
        GL11.glTranslatef(0, 0, -schematic.getLength()/2f);
        
        long renderTime = System.currentTimeMillis() - prevFrameTime;
        if(rotateSchematic)
        	rot += 10f*(renderTime/1000f);
        GL11.glRotatef(rot, 0, 1, 0);
        
        float zDistance = schematic.getLength()/2f;
        
        float renderScaleMax = (float) (zDistance * Math.sin(fovy*Math.PI/180f / 2f));
        float scale = renderScaleMax / (float)(maxSize/2f);
        scale *= scaleSchematic;
        GL11.glScalef(scale, scale, scale);
        
		GL11.glViewport(renderPosX*sr.getScaleFactor(), mc.displayHeight-((renderPosY*sr.getScaleFactor()) + renderHeight*sr.getScaleFactor()), renderWidth*sr.getScaleFactor(), renderHeight*sr.getScaleFactor());
        Tessellator.instance.draw();
        Tessellator.instance.setTranslation(0, 0, 0);
        GL11.glViewport(0,  0, mc.displayWidth, mc.displayHeight);
        
        GL11.glScalef(1f/scale, 1f/scale, 1f/scale);
        //Restore previous values
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        
        prevFrameTime = System.currentTimeMillis();
	}
	
	@Override
	protected void mouseClickMove(int x, int y,
			int event, long time) {
		super.mouseClickMove(x, y, event, time);
		
		//Get update from Slider
		float newScale = (float)sliderRenderScale.getValue();
		if(newScale != scaleSchematic)
		{
			scaleSchematic = newScale;
			updateGui();
		}
	}
	
	@Override
	public void actionPerformed(GuiButton button) {
		super.actionPerformed(button);

		if(button == buttonBuild)
		{
			if(gui.tile.state != BuilderState.BUILDING && gui.tile.state != BuilderState.NEEDRESOURCES)
			{
				MessageBase msg = new MessageActionSchematicBuilder(gui.tile, ActionType.BUILD);
				msg.sendToServer();
			} 
			else 
			{
				MessageBase msg = new MessageActionSchematicBuilder(gui.tile, ActionType.STOP);
				msg.sendToServer();
			}
		}
		else if(button == buttonPass1)
		{
			gui.tile.config.passCount = 1;
			MessageBase msg = new MessageActionSchematicBuilder(gui.tile, gui.tile.config);
			msg.sendToServer();
		}
		else if(button == buttonPass2)
		{
			gui.tile.config.passCount = 2;
			MessageBase msg = new MessageActionSchematicBuilder(gui.tile, gui.tile.config);
			msg.sendToServer();
		}
		else if(button == buttonPass3)
		{
			//TODO: Implement
			/*gui.tile.config.passCount = 3;
			MessageBase msg = new MessageActionSchematicBuilder(gui.tile, gui.tile.config);
			msg.sendToServer();*/
		}
		else if(button == checkBoxFloor)
		{
			gui.tile.config.placeFloor = checkBoxFloor.isChecked();
			gui.tile.sendConfigToServer();
		}
		else if(button == checkBoxAir)
		{
			gui.tile.config.placeAir = checkBoxAir.isChecked();
			gui.tile.sendConfigToServer();
		}
		else if(button == buttonAutoRender)
		{
			renderSchematic = !renderSchematic;
		}
		else if(button == buttonRotateRender)
		{
			rotateSchematic = !rotateSchematic;
		}
		else if(button == buttonVisualize)
		{
			if(WorldSchematicVisualizer.instance.vTile == gui.tile)
				WorldSchematicVisualizer.instance.vTile = null;
			else
				WorldSchematicVisualizer.instance.vTile = gui.tile;
		}
		else if(button == buttonFrame)
		{
			if(WorldSchematicVisualizer.instance.fTile == gui.tile)
				WorldSchematicVisualizer.instance.fTile = null;
			else
				WorldSchematicVisualizer.instance.fTile = gui.tile;
		}
		
		updateGui();
	}

	@Override
	public int getGuiLeft() {
		return guiLeft;
	}

	@Override
	public int getGuiTop() {
		return guiTop;
	}

	@Override
	public int getGuiWidth() {
		return width;
	}

	@Override
	public int getGuiHeight() {
		return height;
	}
	
	@Override
	public String getTabName() {
		return "Controller";
	}


	@Override
	public void setTabButton(GuiButtonCustom tab) {
		tabButton = tab;
	}


	@Override
	public GuiButtonCustom getTabButton() {
		return tabButton;
	}


	@Override
	public void setTabId(int id) {
		tabId = id;
	}


	@Override
	public int getTabId() {
		return tabId;
	}

}
