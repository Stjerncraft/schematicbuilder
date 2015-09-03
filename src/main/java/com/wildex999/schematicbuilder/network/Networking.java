package com.wildex999.schematicbuilder.network;

import org.ietf.jgss.MessageProp;

import com.wildex999.utils.ModLog;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class Networking {
	private static int nextId;
	private static SimpleNetworkWrapper channel;
	
	public static void init() {
		nextId = 0;
		channel = NetworkRegistry.INSTANCE.newSimpleChannel("SchematicBuilder1");
		
		//Messages
		//TODO: Load config first, and then print out which ID is registered per message if debugging enabled.
		
		//Release 1(Keep id ordering of previous message by always adding new ones at the end)
		//When removing an old message, replace it with a call to getFreeId().
		channel.registerMessage(MessageUploadSchematic.Handler.class, MessageUploadSchematic.class, getFreeId(), Side.SERVER);
		channel.registerMessage(MessageActionSchematicBuilder.Handler.class, MessageActionSchematicBuilder.class, getFreeId(), Side.SERVER);
		
		channel.registerMessage(MessageUpdateSchematicBuilder.Handler.class, MessageUpdateSchematicBuilder.class, getFreeId(), Side.CLIENT);
		channel.registerMessage(MessageUploadSchematic.Handler.class, MessageUploadSchematic.class, getFreeId(), Side.CLIENT);
		channel.registerMessage(MessageUpdateProgress.Handler.class, MessageUpdateProgress.class, getFreeId(), Side.CLIENT);
		
		//Release 2
		//Release 3
		//etc.
	}
	
	public static int getFreeId() {
		if(nextId >= 255)
			ModLog.logger.warn("Registered more than 255 messages. This might cause problems!");
		return nextId++;
	}
	
	public static SimpleNetworkWrapper getChannel() {
		return channel;
	}
}
