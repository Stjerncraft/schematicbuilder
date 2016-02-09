package com.wildex999.schematicbuilder;

import com.wildex999.schematicbuilder.network.MessageBase;
import com.wildex999.schematicbuilder.network.MessageConfigUpdate;
import com.wildex999.schematicbuilder.network.MessageConfigUpdate.ConfigType;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

public class PlayerManager {
	public PlayerManager() {
	}
	
	@SubscribeEvent
	public void playerLoggedInEvent(PlayerLoggedInEvent event) {
		
		if(ModSchematicBuilder.debug)
			System.out.println("Sending server config to player: " + event.player);
		
		//Send Server config to player
		MessageBase msg = new MessageConfigUpdate(ModSchematicBuilder.configGeneral, ConfigType.GENERAL);
		msg.sendToPlayer((EntityPlayerMP) event.player);
	}
}
