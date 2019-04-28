package net.wizardsoflua.testenv.player;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static net.minecraft.inventory.EntityEquipmentSlot.MAINHAND;
import static net.minecraft.inventory.EntityEquipmentSlot.OFFHAND;
import static net.minecraft.item.ItemStack.EMPTY;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import javax.annotation.Nullable;
import com.google.common.io.Files;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import net.wizardsoflua.WizardsOfLua;
import net.wizardsoflua.spell.SpellUtil;
import net.wizardsoflua.testenv.MinecraftBackdoor;
import net.wizardsoflua.testenv.WolTestenv;
import net.wizardsoflua.testenv.net.ChatMessage;
import net.wizardsoflua.testenv.net.LeftClickMessage;
import net.wizardsoflua.testenv.net.NetworkMessage;
import net.wizardsoflua.testenv.net.ReconnectMessage;
import net.wizardsoflua.testenv.net.RespawnMessage;
import net.wizardsoflua.testenv.net.RightClickMessage;

public class PlayerBackdoor {
  private final MinecraftBackdoor minecraftBackdoor;

  public PlayerBackdoor(MinecraftBackdoor minecraftBackdoor) {
    this.minecraftBackdoor = requireNonNull(minecraftBackdoor, "minecraftBackdoor");
  }

  private WolTestenv getTestenv() {
    return minecraftBackdoor.getTestenv();
  }

  private WizardsOfLua getWol() {
    return getTestenv().getWol();
  }

  public void setOperator(boolean value) {
    if (isOperator() == value) {
      return;
    }
    if (value) {
      minecraftBackdoor.executeCommand("/op " + getName());
    } else {
      minecraftBackdoor.executeCommand("/deop " + getName());
    }
  }

  public boolean isOperator() {
    return getTestenv().getPermissions().hasOperatorPrivileges(getDelegate().getUniqueID());
  }

  public EntityPlayerMP getDelegate() {
    EntityPlayerMP testPlayer = getTestenv().getTestPlayer();
    checkNotNull(testPlayer, "testPlayer==null!");
    return testPlayer;
  }

  public void leftclick(BlockPos pos, EnumFacing face) {
    perform(new LeftClickMessage(pos, face));
  }

  public void rightclick(BlockPos pos, EnumFacing face) {
    perform(new RightClickMessage(pos, face));
  }

  public void chat(String format, Object... args) {
    perform(new ChatMessage(String.format(format, args)));
  }

  public void perform(NetworkMessage message) {
    EntityPlayerMP player = getDelegate();
    getTestenv().sendTo(player, message);
  }

  public void setPosition(BlockPos pos) {
    getTestenv().runOnMainThreadAndWait(
        () -> getDelegate().setPositionAndUpdate(pos.getX(), pos.getY(), pos.getZ()));
  }

  public void setRotationYaw(float yaw) {
    getTestenv().runOnMainThreadAndWait(() -> {
      EntityPlayerMP delegate = getDelegate();
      delegate.setRotationYawHead(yaw);
      delegate.setRenderYawOffset(yaw);
      delegate.connection.setPlayerLocation(delegate.posX, delegate.posY, delegate.posZ,
          delegate.rotationYaw, delegate.rotationPitch);
    });
  }

  public BlockPos getBlockPos() {
    return getDelegate().getPosition();
  }

  public Vec3d getPositionLookingAt() {
    Vec3d result = SpellUtil.getPositionLookingAt(getDelegate());
    return result;
  }

  public BlockPos getBlockPosLookingAt() {
    return new BlockPos(SpellUtil.getPositionLookingAt(getDelegate()));
  }

  public void setTeam(String teamName) {
    Scoreboard worldScoreboard = getDelegate().getWorldScoreboard();
    ScorePlayerTeam team = worldScoreboard.getTeam(teamName);
    worldScoreboard.addPlayerToTeam(getDelegate().getScoreboardName(), team);
  }

  public @Nullable String getTeam() {
    Team team = getDelegate().getTeam();
    if (team == null) {
      return null;
    } else {
      return team.getName();
    }
  }

  public void setHealth(float value) {
    getDelegate().setHealth(value);
  }

  public float getHealth() {
    return getDelegate().getHealth();
  }

  public EnumFacing getFacing() {
    return getDelegate().getHorizontalFacing();
  }

  public void createModule(String moduleName, String content) {
    File moduleFile = getModuleFile(moduleName);
    if (moduleFile.exists()) {
      moduleFile.delete();
    }
    moduleFile.getParentFile().mkdirs();
    try {
      Files.asCharSink(moduleFile, UTF_8).write(content);
    } catch (IOException e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  public void deleteModule(String moduleName) {
    File moduleFile = getModuleFile(moduleName);
    if (moduleFile.exists()) {
      moduleFile.delete();
    }
  }

  private File getModuleFile(String moduleName) {
    String path = moduleName.replace(".", File.separator) + ".lua";
    return new File(
        getTestenv().getConfig().getOrCreateWizardConfig(getDelegate().getUniqueID()).getLibDir(),
        path);
  }

  public String getName() {
    return getDelegate().getName().getString();
  }

  public void setMainHandItem(ItemStack item) {
    getTestenv().runOnMainThreadAndWait(() -> {
      getDelegate().setItemStackToSlot(MAINHAND, ofNullable(item).orElse(EMPTY));
      getDelegate().inventoryContainer.detectAndSendChanges();
    });
  }

  public ItemStack getMainHandItem() {
    return getDelegate().getHeldItemMainhand();
  }

  public void setOffHandItem(ItemStack item) {
    getTestenv().runOnMainThreadAndWait(() -> {
      getDelegate().setItemStackToSlot(OFFHAND, ofNullable(item).orElse(EMPTY));
      getDelegate().inventoryContainer.detectAndSendChanges();
    });
  }

  public ItemStack getOffHandItem() {
    return getDelegate().getHeldItemOffhand();
  }

  public void changeDimension(DimensionType dim) {
    getTestenv().runOnMainThreadAndWait(() -> {
      getDelegate().getEntityWorld().getServer().getPlayerList()
          .changePlayerDimension(getDelegate(), dim);
    });
  }

  public void reconnect() {
    perform(new ReconnectMessage());
  }

  public void respawn() {
    perform(new RespawnMessage());
  }

  public void waitForPlayer(long duration) {
    long started = System.currentTimeMillis();
    while (getTestenv().getTestPlayer() == null) {
      if (started + duration > System.currentTimeMillis()) {
        sleep(100);
      } else {
        throw new RuntimeException("Timeout! Testplayer not available within " + duration + " ms");
      }
    }
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
