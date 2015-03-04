package com.wildex999.schematicbuilder.network;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.wildex999.schematicbuilder.network.MessageUpdateSchematicBuilder.UpdateType;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

//Message sent to Server to perform an action

public class MessageActionSchematicBuilder extends MessageBase {

	public enum ActionType {
		BUILD(0),
		STOP(1);
		
		private final int value;
		
		//Map of values for rapid lookup from serialized
		private static Map<Integer, ActionType> enumMap;
		static {
			enumMap = new HashMap<Integer, ActionType>();
			for(ActionType state : EnumSet.allOf(ActionType.class))
				enumMap.put(state.value, state);
		}
		
		ActionType(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
		
		public static ActionType fromValue(int value) {
			return enumMap.get(value);
		}
	};
	
	private ActionType action;
	private TileEntityInfo tileInfo;
	
	//Receive Constructor
	public MessageActionSchematicBuilder() {
	}
	
	//Send Constructor
	public MessageActionSchematicBuilder(TileEntity tile, ActionType action) {
		tileInfo = new TileEntityInfo(tile);
		this.action = action;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		tileInfo = readTileEntity(buf);
		action = ActionType.fromValue(buf.readInt());
	}

	@Override
	public void toBytes(ByteBuf buf) {
		writeTileEntity(buf, tileInfo);
		buf.writeInt(action.getValue());
	}
	
	public static class Handler implements IMessageHandler<MessageActionSchematicBuilder, IMessage> {
        
        @Override
        public IMessage onMessage(MessageActionSchematicBuilder message, MessageContext ctx) {
        	World world = getWorld(ctx);
        	TileEntity baseTile = message.tileInfo.getTileEntity(world);
        	
        	if(!(baseTile instanceof TileSchematicBuilder))
        		return null;
        	TileSchematicBuilder tile = (TileSchematicBuilder)baseTile;

        	switch(message.action) {
        	case BUILD:
        		tile.actionBuild();
        		break;
        	case STOP:
        		tile.actionStop();
        		break;
        	}
        	
        	return null;
        }
    }

}
