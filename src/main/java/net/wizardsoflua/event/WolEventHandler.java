package net.wizardsoflua.event;

import static com.google.common.base.Preconditions.checkNotNull;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.wizardsoflua.WizardsOfLua;
import net.wizardsoflua.lua.extension.api.service.SpellExtensions;
import net.wizardsoflua.lua.module.events.EventsModule;
import net.wizardsoflua.spell.SpellEntity;

public class WolEventHandler {

  private static final String WOL_PACKET_HANDLER_NAME = WizardsOfLua.MODID + ":packet_handler";
  private static final String VANILLA_PACKET_HANDLER_NAME = "packet_handler";

  public interface Context {
    Iterable<SpellEntity> getSpells();

    boolean isSupportedLuaEvent(Event event);

    String getEventName(Event event);
  }

  private final Context context;

  public WolEventHandler(Context context) {
    this.context = checkNotNull(context, "context==null!");
  }

  @SubscribeEvent
  public void onEvent(Event event) {
    if (FMLCommonHandler.instance().getEffectiveSide() != Side.SERVER) {
      return;
    }
    if (context.isSupportedLuaEvent(event)) {
      for (SpellEntity spellEntity : context.getSpells()) {
        String eventName = context.getEventName(event);
        SpellExtensions extensions = spellEntity.getProgram().getLuaExtensionLoader();
        EventsModule events = extensions.getSpellExtension(EventsModule.class);
        events.onEvent(eventName, event);
      }
    }
  }

  @SubscribeEvent
  public void onPlayerRespawnEvent(PlayerRespawnEvent evt) {
    if (FMLCommonHandler.instance().getEffectiveSide() != Side.SERVER) {
      return;
    }
    EntityPlayerMP player = (EntityPlayerMP) evt.player;
    addWolPacketHandler(player);
    replacePlayerInstance(player);
  }

  @SubscribeEvent
  public void onPlayerLoggedIn(PlayerLoggedInEvent evt) {
    if (FMLCommonHandler.instance().getEffectiveSide() != Side.SERVER) {
      return;
    }
    EntityPlayerMP player = (EntityPlayerMP) evt.player;
    addWolPacketHandler(player);
    replacePlayerInstance(player);
  }

  private void replacePlayerInstance(EntityPlayerMP player) {
    for (SpellEntity spellEntity : context.getSpells()) {
      spellEntity.replacePlayerInstance(player);
    }
  }

  private void addWolPacketHandler(EntityPlayerMP player) {
    NetworkManager networkManager = player.connection.getNetworkManager();
    Channel channel = networkManager.channel();
    ChannelPipeline pipeline = channel.pipeline();
    WolChannelInboundHandlerAdapter handler = pipeline.get(WolChannelInboundHandlerAdapter.class);
    if (handler == null) {
      if (pipeline.get(VANILLA_PACKET_HANDLER_NAME) != null) {
        handler = new WolChannelInboundHandlerAdapter(player);
        pipeline.addBefore(VANILLA_PACKET_HANDLER_NAME, WOL_PACKET_HANDLER_NAME, handler);
      } else {
        WizardsOfLua.instance.logger.error("Can't add WolPacketHandler: vanilla packet handler '"
            + VANILLA_PACKET_HANDLER_NAME + "' not found!");
        throw new RuntimeException("Can't add WolPacketHandler!");
      }
    } else {
      // This is essential when a player respawns.
      handler.setPlayer(player);
    }
  }

}
