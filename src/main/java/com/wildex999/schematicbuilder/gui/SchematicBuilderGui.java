package com.wildex999.schematicbuilder.gui;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.gui.elements.GuiLabel;
import com.wildex999.schematicbuilder.inventory.ContainerSchematicBuilder;
import com.wildex999.schematicbuilder.network.MessageActionSchematicBuilder;
import com.wildex999.schematicbuilder.network.MessageActionSchematicBuilder.ActionType;
import com.wildex999.schematicbuilder.network.MessageBase;
import com.wildex999.schematicbuilder.tiles.BuilderState;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.IGuiHandler;

public class SchematicBuilderGui implements IGuiHandler
{
	public static final int GUI_ID = GuiHandler.getNextGuiID();
	
	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		TileSchematicBuilder inventory = (TileSchematicBuilder)world.getTileEntity(x, y, z);
		return new ContainerSchematicBuilder(player.inventory, inventory);
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world,int x, int y, int z) {
		TileEntity tile = world.getTileEntity(x, y, z);
		return new GUI(player, (TileSchematicBuilder)tile);
	}

	public static class GUI extends GuiContainer {
	
		public static final ResourceLocation backgroundImage = new ResourceLocation(ModSchematicBuilder.MODID, "textures/gui/schematic_builder.png");
		
		private static final short backgroundWidth = 232;
		private static final short backgroundHeight = 239;
		
		private TileSchematicBuilder tile;
		private EntityPlayer player;
		
		private int colorText = 4210752;
		private int colorOk = 0x47D147;
		private int colorError = 0xFF6600;
		
		private GuiLabel labelContainerName;
		private GuiLabel labelStatus;
		private GuiLabel labelStatusContent;
		private GuiLabel labelSchematicName;
		private GuiLabel labelLoad;
		
		private GuiButton buttonLoadGlobal;
		private GuiButton buttonLoadLocal;
		private GuiButton buttonBuild;

		private String stringBuild = "Build";
		private String stringStop = "Stop";
	
		public GUI(EntityPlayer player, TileSchematicBuilder tile) {
			//Slot positions set in Container
			super(new ContainerSchematicBuilder(player.inventory, tile));
			this.tile = tile;
			this.player = player;
			
			if(tile == null)
				player.closeScreen();
		}
		
		
		@Override
		public void setWorldAndResolution(Minecraft mc, int width, int height) {
			super.setWorldAndResolution(mc, width, height);
			
			this.guiLeft = (width-backgroundWidth)/2;
			this.guiTop = (height-backgroundHeight)/2;
			this.xSize = backgroundWidth;
			this.ySize = backgroundHeight;

			labelContainerName = new GuiLabel(tile.getInventory().getInventoryName(), this.guiLeft + 50, this.guiTop + 5, colorText);
			labelStatus = new GuiLabel("Status:", guiLeft + 5, guiTop + 15, colorText);
			labelStatusContent = new GuiLabel("Idle", guiLeft + 43, guiTop + 15, colorOk);
			labelSchematicName = new GuiLabel("Schematic: None", guiLeft + 5, guiTop + 25, colorText);
			labelLoad = new GuiLabel("Load Schematic:", guiLeft + 5, guiTop + 40, colorText);
			
			buttonLoadGlobal = new GuiButton(11, guiLeft + 90, guiTop + 35, 40, 20, "Server");
			buttonLoadGlobal.enabled = false;
			buttonLoadLocal = new GuiButton(12, guiLeft + 132, guiTop + 35, 40, 20, "Local");
			buttonBuild = new GuiButton(13, guiLeft + 145, guiTop + 55, 28, 20, stringBuild);
			buttonList.add(buttonLoadGlobal);
			buttonList.add(buttonLoadLocal);
			buttonList.add(buttonBuild);
			
			update();
		}
	
		@Override
		public void drawGuiContainerBackgroundLayer(float par1, int mouseX, int mouseY) {
			
			this.mc.getTextureManager().bindTexture(backgroundImage);
	
			
			this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, backgroundWidth, backgroundHeight);

			labelContainerName.draw(fontRendererObj);
			labelStatus.draw(fontRendererObj);
			labelStatusContent.draw(fontRendererObj);
			labelSchematicName.draw(fontRendererObj);
			labelLoad.draw(fontRendererObj);
		}
	
		@Override
		public boolean doesGuiPauseGame() {
			return false;
		}
		
		@Override
		protected void actionPerformed(GuiButton button) {
			super.actionPerformed(button);
			
			if(button == buttonLoadLocal)
				tile.chooseSchematicFile();
			else if(button == buttonBuild)
			{
				if(tile.state != BuilderState.BUILDING)
				{
					MessageBase msg = new MessageActionSchematicBuilder(tile, ActionType.BUILD);
					msg.sendToServer();
				} 
				else 
				{
					MessageBase msg = new MessageActionSchematicBuilder(tile, ActionType.STOP);
					msg.sendToServer();
				}
			}
		}
		
		//Update the GUI based on state
		public void update() {
			BuilderState state = tile.state;
			
			//Revert to defaults
			buttonLoadLocal.enabled = false;
			buttonBuild.enabled = false;
			buttonBuild.displayString = stringBuild;
			
			switch(state) {
			case IDLE:
				labelStatusContent.label = "Idle";
				labelStatusContent.color = colorOk;
				buttonLoadLocal.enabled = true;
				break;
			case CHOOSINGLOCAL:
				labelStatusContent.label = "Choosing file...";
				labelStatusContent.color = colorOk;
				break;
			case READING:
				labelStatusContent.label = "Reading file: " + tile.filePath;
				labelStatusContent.color = colorOk;
				break;
			case LOADING:
				labelStatusContent.label = "Loading Schematic...";
				labelStatusContent.color = colorOk;
				break;
			case UPLOADING:
				labelStatusContent.label = "Uploading (" + tile.message + ")";
				labelStatusContent.color = colorOk;
				break;
			case DOWNLOADING:
				labelStatusContent.label = "Downloading (" + tile.message + ")";
				labelStatusContent.color = colorOk;
				break;
			case READY:
				labelStatusContent.label = "Ready";
				labelStatusContent.color = colorOk;
				buttonLoadLocal.enabled = true;
				buttonBuild.enabled = true;
				break;
			case BUILDING:
				labelStatusContent.label = "Building(" + tile.message + ")";
				labelStatusContent.color = colorOk;
				buttonBuild.displayString = stringStop;
				buttonBuild.enabled = true;
				break;
			case STOPPED:
				labelStatusContent.label = "Stopped(" + tile.message + ")";
				labelStatusContent.color = colorOk; //TODO: Color warning?
				buttonBuild.enabled = true;
				buttonLoadLocal.enabled = true;
				break;
			case DONE:
				labelStatusContent.label = "Done";
				labelStatusContent.color = colorOk;
				buttonLoadLocal.enabled = true;
				break;
			case ERROR:
				labelStatusContent.label = "E: " + tile.message;
				labelStatusContent.color = colorError;
				buttonLoadLocal.enabled = true;
				break;
			default:
				labelStatusContent.label = "Unknown State: " + state;
				labelStatusContent.color = colorError;
				buttonLoadLocal.enabled = true;
			}
		}
		
	}
}