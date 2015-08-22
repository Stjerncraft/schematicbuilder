package com.wildex999.schematicbuilder;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

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
			WorldSchematicVisualizer.instance.vTile = null;
			WorldSchematicVisualizer.instance.fTile = null;
		}
	}
}
