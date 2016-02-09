package com.wildex999.schematicbuilder.network;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.wildex999.schematicbuilder.WorldSchematicVisualizer;

import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/*
 * Update the progress for a Builder/Scanner to show where it's working
 */

public class MessageUpdateProgress extends MessageBase {
	public enum ProgressType {
		STOP(0), //Stop showing updates for the given source
		BUILDER(1);
		
		private final int value;
		
		//Map of values for rapid lookup from serialized
		private static Map<Integer, ProgressType> enumMap;
		static {
			enumMap = new HashMap<Integer, ProgressType>();
			for(ProgressType state : EnumSet.allOf(ProgressType.class))
				enumMap.put(state.value, state);
		}
		
		ProgressType(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
		
		public static ProgressType fromValue(int value) {
			return enumMap.get(value);
		}
	};
	
	private ProgressType type;
	private TileEntityInfo tileInfo;
	private int targetX, targetY, targetZ;
	
	//Receive Constructor
	public MessageUpdateProgress() {
	}
	
	//Send Constructor(Stop)
	public MessageUpdateProgress(TileEntity tile) {
		tileInfo = new TileEntityInfo(tile);
		this.type = ProgressType.STOP;
	}
	
	//Send Constructor
	public MessageUpdateProgress(TileEntity tile, int targetX, int targetY, int targetZ) {
		tileInfo = new TileEntityInfo(tile);
		this.type = ProgressType.BUILDER;
		this.targetX = targetX;
		this.targetY = targetY;
		this.targetZ = targetZ;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		tileInfo = readTileEntity(buf);
		type = ProgressType.fromValue(buf.readInt());
		
		switch(type) {
		case STOP:
			break;
		case BUILDER:
			targetX = buf.readInt();
			targetY = buf.readInt();
			targetZ = buf.readInt();
			break;
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		writeTileEntity(buf, tileInfo);
		buf.writeInt(type.getValue());
		
		switch(type) {
		case STOP:
			break;
		case BUILDER:
			buf.writeInt(targetX);
			buf.writeInt(targetY);
			buf.writeInt(targetZ);
			break;
		}
	}
	
	public static class Handler implements IMessageHandler<MessageUpdateProgress, IMessage> {
        
        @Override
        public IMessage onMessage(MessageUpdateProgress message, MessageContext ctx) {

        	WorldSchematicVisualizer visualizer = WorldSchematicVisualizer.instance;
        	switch(message.type) {
        	case STOP:
        		visualizer.progressRender.render = false;
        		break;
        	case BUILDER:
        		visualizer.progressRender.targetX = message.targetX;
        		visualizer.progressRender.targetY = message.targetY;
        		visualizer.progressRender.targetZ = message.targetZ;
        		break;
        	}
        	
        	return null;
        }
    }

}
