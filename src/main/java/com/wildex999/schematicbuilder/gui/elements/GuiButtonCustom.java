package com.wildex999.schematicbuilder.gui.elements;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;

//Button with custom texture

public class GuiButtonCustom extends GuiButton {

	public int texX;
	public int texY;
	public int hoverOffsetX = 0;
	public int hoverOffsetY = 0;
	public int texWidth;
	public int texHeight;
	public ResourceLocation texture;
	
	public int data; //Custom data bound to button
	
	public int enabledColor;
	public int disabledColor;
	
	public GuiButtonCustom(int id, int x, int y, int texX, int texY, ResourceLocation texture, int width, int height, String label) {
		super(id, x, y, width, height, label);
		texWidth = width;
		texHeight = height;
		this.texX = texX;
		this.texY = texY;
		this.texture = texture;
	}
	
	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible)
        {
            FontRenderer fontrenderer = mc.fontRendererObj;
            mc.getTextureManager().bindTexture(texture);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            int k = this.getHoverState(this.hovered);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            if(k == 2) //Hover
            	this.drawTexturedModalRect(this.xPosition, this.yPosition, texX+hoverOffsetX, texY+hoverOffsetY, texWidth, texHeight);
            else
            	this.drawTexturedModalRect(this.xPosition, this.yPosition, texX, texY, texWidth, texHeight);
            
            this.mouseDragged(mc, mouseX, mouseY);
            int l = 14737632;

            if (!this.enabled)
            {
                l = enabledColor;
            }
            else if (this.hovered)
            {
                l = disabledColor;
            }

            this.drawCenteredString(fontrenderer, this.displayString, this.xPosition + this.width / 2, this.yPosition + (this.height - 8) / 2, l);
        }
	}
	
	public boolean isOver(int mouseX, int mouseY) {
		if(mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height)
			return true;
		return false;
	}

}

