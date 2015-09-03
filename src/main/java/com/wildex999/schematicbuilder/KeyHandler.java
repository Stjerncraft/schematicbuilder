package com.wildex999.schematicbuilder;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.input.Keyboard;

import com.wildex999.schematicbuilder.network.MessageActionSchematicBuilder;
import com.wildex999.schematicbuilder.network.MessageBase;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent;

public class KeyHandler {
	public KeyHandler instance;
	
	private KeyBinding key_StopWorldRender = new KeyBinding("Stop Visualize/Frame", Keyboard.KEY_BACK, "SchematicBuilder");
	
	public KeyHandler() {
		instance = this;
		ClientRegistry.registerKeyBinding(key_StopWorldRender);
	}
	
	@SubscribeEvent
	public void keyEvent(KeyInputEvent event)  {

		if(key_StopWorldRender.isPressed())
		{
			WorldSchematicVisualizer visualizer = WorldSchematicVisualizer.instance;
			visualizer.vTile = null;
			visualizer.fTile = null;
			
			if(visualizer.progressRender.render)
			{
				TileEntity tempTile = new TileEntity();
				tempTile.xCoord = visualizer.progressRender.tileX;
				tempTile.yCoord = visualizer.progressRender.tileY;
				tempTile.zCoord = visualizer.progressRender.tileZ;
				MessageBase msg = new MessageActionSchematicBuilder(tempTile, MessageActionSchematicBuilder.ActionType.PROGRESS);
				msg.sendToServer();
				visualizer.progressRender.render = false;
			}
		}
	}
}
