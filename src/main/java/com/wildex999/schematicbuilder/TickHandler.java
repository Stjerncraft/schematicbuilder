package com.wildex999.schematicbuilder;

import com.wildex999.schematicbuilder.network.MessageBase;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.relauncher.Side;

public class TickHandler {
	
	@SubscribeEvent
	public void onServerTick(ServerTickEvent event) {
		if(event.side == Side.CLIENT)
            return;

		if(event.phase == Phase.END)
		{
			//Networking queued messages
			//MessageBase.sendQueuedMessages();
		}
	}
}
