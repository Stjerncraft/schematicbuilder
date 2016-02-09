package com.wildex999.schematicbuilder.network;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.wildex999.schematicbuilder.ModSchematicBuilder;
import com.wildex999.schematicbuilder.ResourceEntry;
import com.wildex999.schematicbuilder.ResourceItem;
import com.wildex999.schematicbuilder.ResourceManager;
import com.wildex999.schematicbuilder.network.MessageUpdateSchematicBuilder.UpdateType;
import com.wildex999.schematicbuilder.tiles.TileSchematicBuilder;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

//Message sent to Server to perform an action

public class MessageActionSchematicBuilder extends MessageBase {

	public enum ActionType {
		BUILD(0),
		STOP(1),
		CONFIG(2),
		DOWNLOAD(3),
		PROGRESS(4), //Player requesting Progress update(Or stop if already watching)
		RESOURCESWAP(5); //Player selected new Entry for Resource
		
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
	private TileSchematicBuilder.Config config;
	private ResourceItem resource;
	
	//Receive Constructor
	public MessageActionSchematicBuilder() {
	}
	
	//Send Constructor
	public MessageActionSchematicBuilder(TileEntity tile, ActionType action) {
		tileInfo = new TileEntityInfo(tile);
		this.action = action;
	}
	
	//Send Config constructor
	public MessageActionSchematicBuilder(TileEntity tile, TileSchematicBuilder.Config config) {
		tileInfo = new TileEntityInfo(tile);
		this.action = ActionType.CONFIG;
		this.config = config;
	}
	
	//Send ResourceSwap constructor
	public MessageActionSchematicBuilder(TileEntity tile, ResourceItem resource) {
		tileInfo = new TileEntityInfo(tile);
		this.action = ActionType.RESOURCESWAP;
		this.resource = resource;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		tileInfo = readTileEntity(buf);
		action = ActionType.fromValue(buf.readInt());
		
		if(action == ActionType.CONFIG)
		{
			config = new TileSchematicBuilder.Config();
			config.fromBytes(buf);
		}
		else if(action == ActionType.RESOURCESWAP) {
			short schematicBlockId = buf.readShort();
			byte schematicMeta = buf.readByte();
			short serverBlockId = buf.readShort();
			byte serverMeta = buf.readByte();
			
			Block block = (Block) Block.blockRegistry.getObjectById(serverBlockId);
			if(block == null){
				throw new RuntimeException("No block for the given BlockID: " + serverBlockId + 
											", while trying to change Resource Entry! Tile: " + tileInfo.posX + " : " + tileInfo.posY + " : " + tileInfo.posZ);
			}
			
			ResourceEntry entry = ModSchematicBuilder.resourceManager.getEntry(block, serverMeta);
			if(entry == null) {
				throw new RuntimeException("No Entry for " + schematicBlockId + ":" + schematicMeta +
						", while trying to change Resource Entry! Tile: " + tileInfo.posX + " : " + tileInfo.posY + " : " + tileInfo.posZ);
			}
			
			resource = new ResourceItem(schematicBlockId, schematicMeta, entry);
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		writeTileEntity(buf, tileInfo);
		buf.writeInt(action.getValue());
		
		if(action == ActionType.CONFIG)
			config.toBytes(buf);
		else if(action == ActionType.RESOURCESWAP) {
			buf.writeShort(resource.getSchematicBlockId());
			buf.writeByte(resource.getSchematicMeta());
			buf.writeShort(resource.getBlockId());
			buf.writeByte(resource.getMeta());
		}
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
        	case CONFIG:
        		tile.actionConfig(message.config);
        		break;
        	case DOWNLOAD:
        		tile.actionDownload(ctx.getServerHandler().playerEntity);
        		break;
        	case PROGRESS:
        		tile.actionProgress(ctx.getServerHandler().playerEntity);
        		break;
        	case RESOURCESWAP:
        		tile.actionSwapResource(ctx.getServerHandler().playerEntity, message.resource);
        	}
        	
        	return null;
        }
    }

}
