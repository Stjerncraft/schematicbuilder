package com.wildex999.schematicbuilder.network;

import io.netty.buffer.ByteBuf;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.tileentity.TileEntity;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.WorldSchematicVisualizer;
import com.wildex999.schematicbuilder.config.ConfigurationManager;
import com.wildex999.schematicbuilder.network.MessageBase.TileEntityInfo;
import com.wildex999.utils.ModLog;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

/*
 * Send by server for client to update it's config to match the server.
 * 
 * Is also sent by the Client when asking for config changes(OP player)
 */

public class MessageConfigUpdate extends MessageBase implements IMessageHandler<MessageConfigUpdate, IMessage> {
	public enum ConfigType {
		GENERAL(0),
		RESOURCE(1);
		
		private final int value;
		
		//Map of values for rapid lookup from serialized
		private static Map<Integer, ConfigType> enumMap;
		static {
			enumMap = new HashMap<Integer, ConfigType>();
			for(ConfigType state : EnumSet.allOf(ConfigType.class))
				enumMap.put(state.value, state);
		}
		
		ConfigType(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
		
		public static ConfigType fromValue(int value) {
			return enumMap.get(value);
		}
	};
	
	private ConfigType type;
	private ConfigurationManager config;
	private boolean error;
	
	//Receive Constructor
	public MessageConfigUpdate() {
		error = false;
	}
	
	//Send Constructor
	public MessageConfigUpdate(ConfigurationManager config, ConfigType type) {
		this.type = type;
		this.config = config;
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(type.getValue());
		try {
			config.toBytes(buf);
		} catch (Exception e) {
			//We can't return error, or mark it for not sending, so we have to throw a RuntimeException instead(Bad)
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		type = ConfigType.fromValue(buf.readInt());
		switch(type) {
		case GENERAL:
			try {
				ModSchematicBuilder.configGeneral.fromBytes(buf);
			} catch (Exception e) {
				ModLog.logger.error("Error while receiving serialized config: " + e.getMessage());
				e.printStackTrace();
				error = true;
			}
			break;
		case RESOURCE:
			//TODO: Load serialized Resource config
			break;
		}
	}

    @Override
    public IMessage onMessage(MessageConfigUpdate message, MessageContext ctx) {
    	//TODO: If receiving on server side, check if player is allowed to make these changes
    	if(ctx.side == Side.SERVER)
    	{
    		System.out.println("Received Config update on Server, this is not yet implemented!");
    		return null;
    	}
    	
    	
    	if(ModSchematicBuilder.debug)
    		System.out.println("Client received Config update from server for type " + message.type);
    	
    	switch(message.type)
    	{
    	case GENERAL:
    		if(!error)
    		{
    			ModSchematicBuilder.configGeneral.saveConfig(false); //Save changes to local config
    			ModSchematicBuilder.configGeneral.reload(false); //Notify of changes
    		}
    		else
    			ModSchematicBuilder.configGeneral.reload(true); //Config might be in an unknown/invalid state, reload from local config
    		break;
    	case RESOURCE:
    		break;
    	}
    	
    	return null;
    }
}
