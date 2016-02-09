package com.wildex999.schematicbuilder.gui.elements;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;

//A button which stretches the texture to fill height

public class GuiButtonStretched extends GuiButton {
	
	public boolean toggled; //Draw button texture upside down so it looks "down"
	public int textOffsetX = 0; //Shift the rendered text
	public int textOffsetY = 0;

	public GuiButtonStretched(int id, int x, int y,
			String str) {
		super(id, x, y, str);
	}
	
	public GuiButtonStretched(int id, int x, int y,
			int width, int height, String str) {
		super(id, x, y, width, height, str);
	}
	
	//Much of this is just a copy-paste from GuiButton
	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible)
        {
            FontRenderer fontrenderer = mc.fontRendererObj;
            mc.getTextureManager().bindTexture(buttonTextures);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            
            //Check if mouse is over button
            this.hovered = isOver(mouseX, mouseY);
            //Get draw state(Disabled, not hovering, hovering)
            int drawState = this.getHoverState(this.hovered);
            
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.blendFunc(770, 771);
            
            int buttonWidth = 200;
            int buttonHeight = 20;
            int drawHeight = height;
            int u1 = 0;
            int u_width = width / 2;
            if(width > buttonWidth)
            	u_width = buttonWidth / 2;
            
            int v1 = 46 + drawState * 20;
            int v_height = 20;
            if(height > buttonHeight) { //Tile height
            	v_height = 5;
            	drawHeight = v_height;
            }
            
            if(!toggled)
            {
            	this.drawScaledCustomSizeModalRect(this.xPosition, this.yPosition, u1, v1, u_width, v_height, width/2, drawHeight, 256, 256); //Left
            	this.drawScaledCustomSizeModalRect(this.xPosition + this.width / 2, this.yPosition, buttonWidth-u_width, v1, u_width, v_height, width/2, drawHeight, 256, 256); //Right
            	
            	if(height > buttonHeight) {
                	this.drawScaledCustomSizeModalRect(this.xPosition, this.yPosition+v_height, u1, v1+v_height, u_width, buttonHeight-(2*v_height), width/2, height-(2*v_height), 256, 256);//Left Middle
                	this.drawScaledCustomSizeModalRect(this.xPosition + this.width / 2, this.yPosition+v_height, buttonWidth-u_width, v1+v_height, u_width, buttonHeight-(2*v_height), width/2, height-(2*v_height), 256, 256); //Right Middle
                	this.drawScaledCustomSizeModalRect(this.xPosition, this.yPosition+height-drawHeight, u1, v1+(buttonHeight-v_height), u_width, v_height, width/2, drawHeight, 256, 256); //Left Bottom
                	this.drawScaledCustomSizeModalRect(this.xPosition + this.width / 2, this.yPosition+height-drawHeight, buttonWidth-u_width, v1+(buttonHeight-v_height), u_width, v_height, width/2, drawHeight, 256, 256); //Right Bottom
            	}
            }
            else
            {
            	this.drawScaledCustomSizeModalRect(this.xPosition, this.yPosition, buttonWidth, v1+v_height, -u_width, -v_height, width/2, drawHeight, 256, 256); //Left
            	this.drawScaledCustomSizeModalRect(this.xPosition + this.width / 2, this.yPosition, u1+u_width, v1+v_height, -u_width, -v_height, width/2, drawHeight, 256, 256); //Right
            }
            this.mouseDragged(mc, mouseX, mouseY);
            int l = 14737632;

            if (packedFGColour != 0)
            {
                l = packedFGColour;
            }
            else if (!this.enabled)
            {
                l = 10526880;
            }
            else if (this.hovered || toggled)
            {
                l = 16777120;
            }

            if(!toggled)
            	this.drawCenteredString(fontrenderer, this.displayString, textOffsetX + this.xPosition + this.width / 2, textOffsetY + this.yPosition + (this.height - 8) / 2, l);
            else
            	this.drawCenteredString(fontrenderer, this.displayString, textOffsetX + this.xPosition + this.width / 2, textOffsetY + this.yPosition + (this.height - 8) / 2, l);
        }
	}
	
	public boolean isOver(int mouseX, int mouseY) {
		if(mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height)
			return true;
		return false;
	}


}
