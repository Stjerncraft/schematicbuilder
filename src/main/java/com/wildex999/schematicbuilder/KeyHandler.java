package com.wildex999.schematicbuilder;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;

import org.lwjgl.input.Keyboard;

import com.wildex999.schematicbuilder.network.MessageActionSchematicBuilder;
import com.wildex999.schematicbuilder.network.MessageBase;
import com.wildex999.schematicbuilder.tiles.TileDummy;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;

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
				TileEntity tempTile = new TileDummy();
				BlockPos newPos = new BlockPos(visualizer.progressRender.tileX, visualizer.progressRender.tileY, visualizer.progressRender.tileZ);
				tempTile.setPos(newPos);
				MessageBase msg = new MessageActionSchematicBuilder(tempTile, MessageActionSchematicBuilder.ActionType.PROGRESS);
				msg.sendToServer();
				visualizer.progressRender.render = false;
			}
		}
	}
}
