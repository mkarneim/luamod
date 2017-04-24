package net.karneim.luamod.lua.event;

import java.lang.reflect.Field;
import java.lang.reflect.UndeclaredThrowableException;

import javax.annotation.Nullable;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import net.karneim.luamod.LuaMod;
import net.karneim.luamod.lua.SpellEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerConnectionFromClientEvent;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;

public class ModEventHandler {
  private LuaMod mod;

  public ModEventHandler(LuaMod mod) {
    this.mod = mod;
  }

  @SubscribeEvent
  public void onChat(ServerChatEvent evt) {
    onEvent(EventType.CHAT, evt);
  }

  @SubscribeEvent
  public void onClientConnected(ServerConnectionFromClientEvent evt) {
    NetworkManager networkManager = evt.getManager();
    Channel channel = networkManager.channel();
    ChannelPipeline pipeline = channel.pipeline();
    ChannelHandler handler = new ChannelInboundHandlerAdapter() {
      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof CPacketAnimation) {
          EntityPlayer player = getPlayer(ctx);
          if (player != null) {
            CPacketAnimation animation = (CPacketAnimation) msg;
            onEvent(EventType.ANIMATION_HAND, new AnimationHandEvent(player, animation.getHand()));
          }
        } else if (msg instanceof CPacketClickWindow) {
          EntityPlayer player = getPlayer(ctx);
          if (player != null) {
            CPacketClickWindow clickWindow = (CPacketClickWindow) msg;
            onEvent(EventType.CLICK_WINDOW, new ClickWindowEvent(player, clickWindow));
          }
        }
        super.channelRead(ctx, msg);
      }

      private EntityPlayer getEntityPlayer(NetworkDispatcher nx) {
        try {
          Field f = NetworkDispatcher.class.getDeclaredField("player");
          f.setAccessible(true);
          EntityPlayer result = (EntityPlayer) f.get(nx);
          return result;
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
          throw new UndeclaredThrowableException(e);
        }
      }

      private @Nullable EntityPlayer getPlayer(ChannelHandlerContext ctx) {
        ChannelHandler xx = ctx.pipeline().get("fml:packet_handler");
        if (xx instanceof NetworkDispatcher) {
          NetworkDispatcher nx = (NetworkDispatcher) xx;
          return getEntityPlayer(nx);
        }
        return null;
      }

      private void onEvent(EventType eventType, Object event) {
        mod.getServer().addScheduledTask(() -> ModEventHandler.this.onEvent(eventType, event));
      }
    };
    pipeline.addAfter("fml:packet_handler", "luamod:packet_handler", handler);
  }

  @SubscribeEvent
  public void onCommand(CommandEvent evt) {
    // System.out.println("onCommand: " + evt.getCommand().getCommandName() + " "
    // + Arrays.toString(evt.getParameters()));
  }

  private void onEvent(EventType type, Object evt) {
    for (SpellEntity e : mod.getSpellRegistry().getAll()) {
      e.getEvents().handle(type, evt);
    }
  }

  @SubscribeEvent
  public void onLeftClickBlock(LeftClickBlock evt) {
    if (evt.getWorld().isRemote) {
      return;
    }
    onEvent(EventType.LEFT_CLICK, evt);
  }

  @SubscribeEvent
  public void onPlayerLoggedIn(PlayerLoggedInEvent evt) {
    onEvent(EventType.PLAYER_JOINED, evt);
  }

  @SubscribeEvent
  public void onPlayerLoggedOut(PlayerLoggedOutEvent evt) {
    onEvent(EventType.PLAYER_LEFT, evt);
  }

  @SubscribeEvent
  public void onPlayerRespawn(PlayerRespawnEvent evt) {
    onEvent(EventType.PLAYER_SPAWNED, evt);
  }

  /////

  @SubscribeEvent
  public void onRightClickBlock(RightClickBlock evt) {
    if (evt.getWorld().isRemote) {
      return;
    }
    onEvent(EventType.RIGHT_CLICK, evt);
  }

  ////

  public void onWhisper(WhisperEvent evt) {
    onEvent(EventType.WHISPER, evt);
  }

}
