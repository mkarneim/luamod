package net.wizardsoflua.lua.classes.entity;

import static java.lang.String.format;

import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.auto.service.AutoService;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketPlayerPosLook.EnumFlags;
import net.minecraft.scoreboard.Team;
import net.minecraft.world.GameType;
import net.sandius.rembulan.LuaRuntimeException;
import net.sandius.rembulan.Table;
import net.wizardsoflua.WizardsOfLua;
import net.wizardsoflua.annotation.GenerateLuaClassTable;
import net.wizardsoflua.annotation.GenerateLuaDoc;
import net.wizardsoflua.annotation.GenerateLuaInstanceTable;
import net.wizardsoflua.annotation.LuaProperty;
import net.wizardsoflua.extension.api.inject.Resource;
import net.wizardsoflua.extension.spell.api.resource.Injector;
import net.wizardsoflua.extension.spell.api.resource.LuaConverters;
import net.wizardsoflua.extension.spell.spi.LuaConverter;
import net.wizardsoflua.lua.classes.BasicLuaClass;
import net.wizardsoflua.lua.classes.LuaClassAttributes;
import net.wizardsoflua.lua.classes.common.Delegator;

/**
 * An instance of the <span class="notranslate">Player</span> class represents a specific player who
 * is currently logged into the world.
 */
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

    @Resource
    private WizardsOfLua wol;

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

    /**
     * This is this player's game mode. It can be one of 'survival', 'adventure', 'creative',
     * 'spectator'.
     *
     * #### Example
     *
     * Setting the gamemode of this spell's owner to 'adventure'.
     *
     * <code>
     * spell.owner.gamemode = "adventure"
     * </code>
     */
    @LuaProperty
    public GameType getGamemode() {
      return delegate.interactionManager.getGameType();
    }

    @LuaProperty
    public void setGamemode(GameType gamemode) {
      delegate.setGameType(gamemode);
    }

    /**
     * This is <span class="notranslate">true</span> if this player has operator privileges.
     */
    @LuaProperty
    public boolean isOperator() {
      return wol.getPermissions().hasOperatorPrivileges(delegate.getPersistentID());
    }

    /**
     * The 'team' is the name of the team this player belongs to, or
     * <span class="notranslate">nil</span> if he is not a member of any team.
     *
     * #### Example
     *
     * Adding the wizard to the 'rogues' team.
     *
     * <code>
     * spell.owner.team = "rogues"
     * </code>
     *
     * To make this work, don't forget to create the 'rogues' team first:
     *
     * <code>
     * /scoreboard teams add rogues
     * </code>
     *
     * #### Example
     *
     * Printing the wizard's team name.
     *
     * <code>
     * print( spell.owner.team)
     * </code>
     */
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
      if (team == null) {
        delegate.getWorldScoreboard().removePlayerFromTeams(delegate.getName());
      } else {
        boolean success = delegate.getWorldScoreboard().addPlayerToTeam(delegate.getName(), team);
        if (!success) {
          throw new IllegalArgumentException(String
              .format("Couldn't add player %s to unknown team %s!", delegate.getName(), team));
        }
      }
    }

    @Override
    protected void setPos(double x, double y, double z) {
      Set<EnumFlags> relativeFlags = EnumSet.allOf(EnumFlags.class);
      relativeFlags.remove(EnumFlags.X);
      relativeFlags.remove(EnumFlags.Y);
      relativeFlags.remove(EnumFlags.Z);
      delegate.connection.setPlayerLocation(x, y, z, 0, 0, relativeFlags);
    }

    @Override
    public void setRotationPitch(float rotationPitch) {
      Set<EnumFlags> relativeFlags = EnumSet.allOf(EnumFlags.class);
      relativeFlags.remove(EnumFlags.X_ROT);
      delegate.connection.setPlayerLocation(0, 0, 0, 0, rotationPitch, relativeFlags);
    }

    @Override
    public void setRotationYaw(float rotationYaw) {
      Set<EnumFlags> relativeFlags = EnumSet.allOf(EnumFlags.class);
      relativeFlags.remove(EnumFlags.Y_ROT);
      delegate.connection.setPlayerLocation(0, 0, 0, rotationYaw, 0, relativeFlags);
    }

    @Override
    protected void setRotation(float rotationYaw, float rotationPitch) {
      Set<EnumFlags> relativeFlags = EnumSet.allOf(EnumFlags.class);
      relativeFlags.remove(EnumFlags.X_ROT);
      relativeFlags.remove(EnumFlags.Y_ROT);
      delegate.connection.setPlayerLocation(0, 0, 0, rotationYaw, rotationPitch, relativeFlags);
    }


    // TODO remove the following part when the lua doc generator is fixed

    /**
     * The 'health' is the energy of this entity. When it falls to zero this entity dies.
     */
    @Override
    @LuaProperty
    public float getHealth() {
      // NOTE: this method is redeclared here to force the lua doc generator to write this docs
      return super.getHealth();
    }

    @Override
    @LuaProperty
    public void setHealth(float value) {
      // NOTE: this method is redeclared here to force the lua doc generator to write this docs
      super.setHealth(value);
    }

    /**
     * This is the [item](/modules/Item) this entity is holding in its main hand.
     */
    @Override
    @LuaProperty
    public @Nullable ItemStack getMainhand() {
      // NOTE: this method is redeclared here to force the lua doc generator to write this docs
      return super.getMainhand();
    }

    @Override
    @LuaProperty
    public void setMainhand(@Nullable ItemStack mainhand) {
      // NOTE: this method is redeclared here to force the lua doc generator to write this docs
      super.setMainhand(mainhand);
    }

    /**
     * This is the [item](/modules/Item) this entity is holding in his off hand.
     */
    @Override
    @LuaProperty
    public @Nullable ItemStack getOffhand() {
      // NOTE: this method is redeclared here to force the lua doc generator to write this docs
      return super.getOffhand();
    }

    @Override
    @LuaProperty
    public void setOffhand(@Nullable ItemStack offhand) {
      // NOTE: this method is redeclared here to force the lua doc generator to write this docs
      super.setOffhand(offhand);
    }
  }
}
