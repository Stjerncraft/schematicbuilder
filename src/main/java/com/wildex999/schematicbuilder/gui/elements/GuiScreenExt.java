package com.wildex999.schematicbuilder.gui.elements;

import java.util.Iterator;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;

//Small fixes to GuiScreen

public class GuiScreenExt extends GuiScreen {
	protected int guiLeft, guiTop;
	
	//Copied from GuiScreen, fixing some issues with it going outside the screen
	@Override
	protected void drawHoveringText(List textList, int x, int y, FontRenderer font)
	{
		if (!textList.isEmpty())
		{
			GL11.glDisable(GL12.GL_RESCALE_NORMAL);
			RenderHelper.disableStandardItemLighting();
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			int k = 0;
			Iterator iterator = textList.iterator();

			while (iterator.hasNext())
			{
				String s = (String)iterator.next();
				int l = font.getStringWidth(s);

				if (l > k)
				{
					k = l;
				}
			}

			int j2 = x + 12;
			int k2 = y - 12;
			int i1 = 8;

			if (textList.size() > 1)
			{
				i1 += 2 + (textList.size() - 1) * 10;
			}
			
			if (j2 + k + guiLeft > this.width)
			{
				j2 = this.width - (k+guiLeft+10);
			}
			/*if(j2 < -guiLeft+3)
				j2 = -guiLeft+3;*/

			if (k2 + i1 + 6 > this.height)
			{
				k2 = this.height - i1 - 6;
			}
			if(k2 < -guiTop+3)
				k2 = -guiTop+3;

			this.zLevel = 300.0F;
			itemRender.zLevel = 300.0F;
			int j1 = -267386864;
			this.drawGradientRect(j2 - 3, k2 - 4, j2 + k + 3, k2 - 3, j1, j1);
			this.drawGradientRect(j2 - 3, k2 + i1 + 3, j2 + k + 3, k2 + i1 + 4, j1, j1);
			this.drawGradientRect(j2 - 3, k2 - 3, j2 + k + 3, k2 + i1 + 3, j1, j1);
			this.drawGradientRect(j2 - 4, k2 - 3, j2 - 3, k2 + i1 + 3, j1, j1);
			this.drawGradientRect(j2 + k + 3, k2 - 3, j2 + k + 4, k2 + i1 + 3, j1, j1);
			int k1 = 1347420415;
			int l1 = (k1 & 16711422) >> 1 | k1 & -16777216;
			this.drawGradientRect(j2 - 3, k2 - 3 + 1, j2 - 3 + 1, k2 + i1 + 3 - 1, k1, l1);
			this.drawGradientRect(j2 + k + 2, k2 - 3 + 1, j2 + k + 3, k2 + i1 + 3 - 1, k1, l1);
			this.drawGradientRect(j2 - 3, k2 - 3, j2 + k + 3, k2 - 3 + 1, k1, k1);
			this.drawGradientRect(j2 - 3, k2 + i1 + 2, j2 + k + 3, k2 + i1 + 3, l1, l1);

			for (int i2 = 0; i2 < textList.size(); ++i2)
			{
				String s1 = (String)textList.get(i2);
				font.drawStringWithShadow(s1, j2, k2, -1);

				if (i2 == 0)
				{
					k2 += 2;
				}

				k2 += 10;
			}

			this.zLevel = 0.0F;
			itemRender.zLevel = 0.0F;
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			RenderHelper.enableStandardItemLighting();
			GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		}
	}
}
