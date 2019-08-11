package net.wizardsoflua.lua.classes.entity;

import static java.lang.String.format;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;
import com.google.auto.service.AutoService;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketPlayerPosLook.EnumFlags;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.sandius.rembulan.LuaRuntimeException;
import net.sandius.rembulan.Table;
import net.wizardsoflua.annotation.GenerateLuaClassTable;
import net.wizardsoflua.annotation.GenerateLuaDoc;
import net.wizardsoflua.annotation.GenerateLuaInstanceTable;
import net.wizardsoflua.annotation.LuaProperty;
import net.wizardsoflua.extension.api.inject.PostConstruct;
import net.wizardsoflua.extension.api.inject.PreDestroy;
import net.wizardsoflua.extension.api.inject.Resource;
import net.wizardsoflua.extension.spell.api.resource.Injector;
import net.wizardsoflua.extension.spell.api.resource.LuaConverters;
import net.wizardsoflua.extension.spell.spi.LuaConverter;
import net.wizardsoflua.lua.classes.BasicLuaClass;
import net.wizardsoflua.lua.classes.LuaClassAttributes;
import net.wizardsoflua.lua.classes.common.Delegator;

@AutoService(LuaConverter.class)
@LuaClassAttributes(name = PlayerClass.NAME, superClass = EntityClass.class)
@GenerateLuaClassTable(instance = PlayerClass.Instance.class)
@GenerateLuaDoc(subtitle = "Controlling the Player")
public final class PlayerClass
    extends BasicLuaClass<EntityPlayer, PlayerClass.Instance<EntityPlayerMP>> {
  public static final String NAME = "Player";
  @Resource
  private LuaConverters converters;
  @Resource
  private Injector injector;

  @PostConstruct
  private void postConstruct() {
    MinecraftForge.EVENT_BUS.register(this);
  }

  @PreDestroy
  private void preDestroy() {
    MinecraftForge.EVENT_BUS.unregister(this);
  }

  @SubscribeEvent
  public void onPlayerClone(PlayerEvent.Clone event) {
    EntityPlayer oldPlayer = event.getOriginal();
    EntityPlayer newPlayer = event.getEntityPlayer();
    if (oldPlayer instanceof EntityPlayerMP && newPlayer instanceof EntityPlayerMP) {
      replacePlayer((EntityPlayerMP) oldPlayer, (EntityPlayerMP) newPlayer);
    }
  }

  private void replacePlayer(EntityPlayerMP oldPlayer, EntityPlayerMP newPlayer) {
    ConcurrentMap<EntityPlayer, Delegator<? extends Instance<EntityPlayerMP>>> cache =
        getCache().asMap();
    Delegator<? extends Instance<EntityPlayerMP>> value = cache.remove(oldPlayer);
    if (value != null) {
      Instance<EntityPlayerMP> instance = value.getDelegate();
      instance.setDelegate(newPlayer);
      cache.put(newPlayer, value);
    }
  }

  @Override
  protected Table createRawTable() {
    return new PlayerClassTable<>(this, converters);
  }

  @Override
  protected Delegator<Instance<EntityPlayerMP>> toLuaInstance(EntityPlayer javaInstance) {
    return new PlayerClassInstanceTable<>(new Instance<>((EntityPlayerMP) javaInstance, injector),
        getTable(), converters);
  }

  @GenerateLuaInstanceTable
  public static class Instance<D extends EntityPlayerMP> extends EntityLivingBaseClass.Instance<D> {
    public Instance(D delegate, Injector injector) {
      super(delegate, injector);
    }

    @Override
    public String getEntityType() {
      return "player";
    }

    @Override
    public void putNbt(Table nbt) {
      throw new LuaRuntimeException(format("%s not supported for class %s", "putNbt", NAME));
    }

    @LuaProperty
    public GameType getGamemode() {
      return delegate.interactionManager.getGameType();
    }

    @LuaProperty
    public void setGamemode(GameType gamemode) {
      delegate.setGameType(gamemode);
    }

    @LuaProperty
    public boolean isOperator() {
      MinecraftServer server = delegate.getServer();
      GameProfile gameProfile = delegate.getGameProfile();
      return server.getPlayerList().canSendCommands(gameProfile);
    }

    // TODO: isn't team supported for entities?

    @LuaProperty
    public @Nullable String getTeam() {
      Team team = delegate.getTeam();
      if (team == null) {
        return null;
      }
      return team.getName();
    }

    @LuaProperty
    public void setTeam(@Nullable String team) {
      Scoreboard scoreboard = delegate.getWorldScoreboard();
      String name = delegate.getScoreboardName();
      if (team == null) {
        scoreboard.removePlayerFromTeams(name);
      } else {
        ScorePlayerTeam teamObj = scoreboard.getTeam(team);
        if (teamObj == null) {
          throw new IllegalArgumentException(
              String.format("Couldn't add player %s to unknown team %s!", name, team));
        }
        scoreboard.addPlayerToTeam(name, teamObj);
      }
    }

    @Override
    protected void setPos(double x, double y, double z) {
      Set<EnumFlags> relativeFlags = EnumSet.allOf(EnumFlags.class);
      relativeFlags.remove(EnumFlags.X);
      relativeFlags.remove(EnumFlags.Y);
      relativeFlags.remove(EnumFlags.Z);
      float rotationYaw = delegate.rotationYaw;
      float rotationPitch = delegate.rotationPitch;
      delegate.connection.setPlayerLocation(x, y, z, rotationYaw, rotationPitch, relativeFlags);
    }

    @Override
    public void setRotationPitch(float rotationPitch) {
      Set<EnumFlags> relativeFlags = EnumSet.allOf(EnumFlags.class);
      double x = delegate.posX;
      double y = delegate.posY;
      double z = delegate.posZ;
      float rotationYaw = delegate.rotationYaw;
      relativeFlags.remove(EnumFlags.X_ROT);
      delegate.connection.setPlayerLocation(x, y, z, rotationYaw, rotationPitch, relativeFlags);
    }

    @Override
    public void setRotationYaw(float rotationYaw) {
      Set<EnumFlags> relativeFlags = EnumSet.allOf(EnumFlags.class);
      double x = delegate.posX;
      double y = delegate.posY;
      double z = delegate.posZ;
      relativeFlags.remove(EnumFlags.Y_ROT);
      float rotationPitch = delegate.rotationPitch;
      delegate.connection.setPlayerLocation(x, y, z, rotationYaw, rotationPitch, relativeFlags);
    }

    @Override
    protected void setRotation(float rotationYaw, float rotationPitch) {
      Set<EnumFlags> relativeFlags = EnumSet.allOf(EnumFlags.class);
      double x = delegate.posX;
      double y = delegate.posY;
      double z = delegate.posZ;
      relativeFlags.remove(EnumFlags.X_ROT);
      relativeFlags.remove(EnumFlags.Y_ROT);
      delegate.connection.setPlayerLocation(x, y, z, rotationYaw, rotationPitch, relativeFlags);
    }
  }
}
