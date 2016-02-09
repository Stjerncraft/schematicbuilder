package com.wildex999.schematicbuilder.network;

import io.netty.buffer.ByteBuf;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class MessageBase implements IMessage {
	
	//Queued messages are mostly used by GUI to send initial status messages,
	//queuing the message to send after the GUI is done initializing on the client.
	private static class QueuedMessage {
		public EntityPlayerMP player = null;
		public IMessage message = null;
        public Packet packet = null;
        public Integer dimensionId = null;
        public TargetPoint point = null;

        //Queue to player
		public QueuedMessage(EntityPlayerMP player, IMessage message)  {
			this.player = player; this.message = message;
		}
        public QueuedMessage(EntityPlayerMP player, Packet packet) {
            this.player = player;
            this.packet = packet;
        }

        //Queue to dimension
        public QueuedMessage(Integer dimensionId, IMessage message) {
            this.dimensionId = dimensionId;
            this.message = message;
        }

        //Queue to all around
        public QueuedMessage(TargetPoint point, IMessage message) {
            this.point = point;
            this.message = message;
        }
	}
	private static List<QueuedMessage> queueList = new LinkedList<QueuedMessage>(); 
	
	public class TileEntityInfo {
		public int posX, posY, posZ;
		
		public TileEntityInfo(TileEntity tile) {
			BlockPos pos = tile.getPos();
			posX = pos.getX(); posY = pos.getY(); posZ = pos.getZ();
		}
		
		public TileEntityInfo(int x, int y, int z) {
			posX = x; posY = y; posZ = z; 
		}
		
		public TileEntity getTileEntity(World world) {
			return world.getTileEntity(new BlockPos(posX, posY, posZ));
		}
	}
	
	//Constructor used when receiving
	public MessageBase() {}
	
	//Write the tile entity position
	public void writeTileEntity(ByteBuf buf, TileEntityInfo tile) {
		buf.writeInt(tile.posX);
		buf.writeInt(tile.posY);
		buf.writeInt(tile.posZ);
	}
	
	//Get the TileEntity written to the packet
	public TileEntityInfo readTileEntity(ByteBuf buf) {
		return new TileEntityInfo(buf.readInt(), buf.readInt(), buf.readInt());
	}
	
	public static World getWorld(MessageContext ctx) {
		if(ctx.side == Side.CLIENT)
			return getWorldClient();
		else
			return getWorldServer(ctx);
	}
	
	@SideOnly(Side.CLIENT)
	public static World getWorldClient() {
		return net.minecraft.client.Minecraft.getMinecraft().theWorld;
	}
	
	public static World getWorldServer(MessageContext ctx) {
		return ctx.getServerHandler().playerEntity.worldObj;
	}
	
	public static void sendQueuedMessages() {
		for(QueuedMessage message : queueList) {
            if(message.packet != null) {
                message.player.playerNetServerHandler.sendPacket(message.packet);
                //ModLog.logger.info("Send packet: " + message.packet);
            }
            else if(message.player != null)
                Networking.getChannel().sendTo(message.message, message.player);
            else if(message.dimensionId != null)
                Networking.getChannel().sendToDimension(message.message, message.dimensionId);
            else if(message.point != null)
                Networking.getChannel().sendToAllAround(message.message, message.point);

		}
		queueList.clear();
	}
	
	
	//--Send Packet--//
	
	//--From Server
	public void sendToPlayer(EntityPlayerMP player) {
		Networking.getChannel().sendTo(this, player);
	}
	
	//Queue packet for beginning of next tick
	public void queueToPlayer(EntityPlayerMP player) {
		queueList.add(new QueuedMessage(player, this));
	}
    public static void queueToPlayer(EntityPlayerMP player, IMessage msg) {
        queueList.add(new QueuedMessage(player, msg));
    }
    public static void queueToPlayer(EntityPlayerMP player, Packet packet) {
        queueList.add(new QueuedMessage(player, packet));
    }
	
	public void sendToDimension(int dimensionId) {
		Networking.getChannel().sendToDimension(this, dimensionId);
	}

    //Queue packet for beginning of next tick
    public void queueToDimension(int dimensionId) {
        queueList.add(new QueuedMessage(dimensionId, this));
    }
    public static void queueToDimension(int dimensionId, IMessage msg) {
        queueList.add(new QueuedMessage(dimensionId, msg));
    }
	
	public void sendToAllAround(TargetPoint point) {
		Networking.getChannel().sendToAllAround(this, point);
	}

    //Queue packet for beginning of next tick
    public void queueToAllAround(TargetPoint point) {
        queueList.add(new QueuedMessage(point, this));
    }
    public static void queueToAllAround(TargetPoint point, IMessage msg) {
        queueList.add(new QueuedMessage(point, msg));
    }
	
	public void sendToAll() {
		Networking.getChannel().sendToAll(this);
	}
	
	
	//--From Client
	public void sendToServer() {
		Networking.getChannel().sendToServer(this);
	}
}
