package net.wizardsoflua.testenv;

import static com.google.common.base.Preconditions.checkArgument;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.EntitySelector;
import net.minecraft.command.arguments.EntitySelectorParser;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.Village;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.wizardsoflua.spell.SpellEntity;
import net.wizardsoflua.testenv.event.ServerLog4jEvent;
import net.wizardsoflua.testenv.event.TestPlayerReceivedChatEvent;
import net.wizardsoflua.testenv.player.PlayerBackdoor;

public class MinecraftBackdoor {

  private final WolTestEnvironment testEnv;
  private final IEventBus eventBus;
  private final PlayerBackdoor player;

  public MinecraftBackdoor(WolTestEnvironment testEnv, IEventBus eventBus) {
    this.testEnv = testEnv;
    this.eventBus = eventBus;
    player = new PlayerBackdoor(this);
  }

  public String getWorldName() {
    return getWorldInfo().getWorldName();
  }

  private WorldInfo getWorldInfo() {
    return getOverworld().getWorldInfo();
  }

  private WorldServer getOverworld() {
    return testEnv.getServer().getWorld(DimensionType.OVERWORLD);
  }

  public int getWorldDimension() {
    return DimensionType.OVERWORLD.getId();
  }

  public EnumDifficulty getDifficulty() {
    return getWorldInfo().getDifficulty();
  }

  public void setDifficulty(EnumDifficulty newDifficulty) {
    getWorldInfo().setDifficulty(newDifficulty);
  }

  public void post(Event event) {
    eventBus.post(event);
  }

  public PlayerBackdoor player() {
    return player;
  }

  public <E extends Event> E waitFor(Class<E> eventType) {
    try {
      return testEnv.getEventRecorder().waitFor(eventType, 5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  public String nextClientMessage() {
    return waitFor(TestPlayerReceivedChatEvent.class).getMessage();
  }

  public String nextServerMessage() {
    return waitFor(ServerLog4jEvent.class).getMessage();
  }

  public void executeCommand(String format, Object... args) {
    String command;
    if (args != null && args.length > 0) {
      command = String.format(format, args);
    } else {
      command = format;
    }
    MinecraftServer server = testEnv.getServer();
    CommandSource source = server.getCommandSource();
    testEnv.runAndWait(() -> server.getCommandManager().handleCommand(source, command));
  }

  public void freezeClock(long millis) {
    ZoneId zoneId = ZoneId.systemDefault();
    Clock clock = Clock.fixed(Instant.ofEpochMilli(millis), zoneId);
    testEnv.getWol().setClock(clock);
  }

  public void freezeClock(LocalDateTime date) {
    ZoneId zoneId = ZoneId.systemDefault();
    Clock clock = Clock.fixed(date.atZone(zoneId).toInstant(), zoneId);
    testEnv.getWol().setClock(clock);
  }

  public void resetClock() {
    Clock clock = testEnv.getWol().getDefaultClock();
    testEnv.getWol().setClock(clock);
  }

  public void clearEvents() {
    testEnv.runAndWait(() -> testEnv.getEventRecorder().clear());
  }

  public void breakAllSpells() {
    testEnv.runAndWait(() -> {
      Collection<SpellEntity> spells = testEnv.getWol().getSpellRegistry().getAll();
      for (SpellEntity spell : spells) {
        spell.setDead();
      }
    });
  }

  public Iterable<SpellEntity> spells() {
    return testEnv.getWol().getSpellRegistry().getAll();
  }

  public void setBlock(BlockPos pos, Block blockType) {
    World world = testEnv.getTestPlayer().getEntityWorld();
    testEnv.runAndWait(() -> world.setBlockState(pos, blockType.getDefaultState()));
  }

  public IBlockState getBlock(BlockPos pos) {
    World world = testEnv.getTestPlayer().getEntityWorld();
    return world.getBlockState(pos);
  }

  public void setChest(BlockPos pos, ItemStack itemStack) {
    World world = testEnv.getTestPlayer().getEntityWorld();
    testEnv.runAndWait(() -> {
      world.setBlockState(pos, Blocks.CHEST.getDefaultState());
      TileEntityChest chest = (TileEntityChest) world.getTileEntity(pos);
      chest.setInventorySlotContents(0, itemStack);
    });
  }

  public BlockPos getWorldSpawnPoint() {
    return getOverworld().getSpawnPoint();
  }

  public void setWorldSpawnPoint(BlockPos pos) {
    getOverworld().setSpawnPoint(pos);
  }

  public BlockPos getNearestVillageCenter(BlockPos pos, int radius) {
    Village v = getOverworld().getVillageCollection().getNearestVillage(pos, radius);
    if (v == null) {
      return null;
    }
    return v.getCenter();
  }

  public int getLuaTicksLimit() {
    return testEnv.getWol().getConfig().getGeneralConfig().getLuaTicksLimit();
  }

  public void setLuaTicksLimit(int luaTicksLimit) {
    testEnv.runAndWait(
        () -> testEnv.getWol().getConfig().getGeneralConfig().setLuaTicksLimit(luaTicksLimit));
  }

  public int getEventListenerLuaTicksLimit() {
    return testEnv.getWol().getConfig().getGeneralConfig().getEventListenerLuaTicksLimit();
  }

  public void setEventListenerLuaTicksLimit(int eventListenerluaTicksLimit) {
    testEnv.runAndWait(() -> testEnv.getWol().getConfig().getGeneralConfig()
        .setEventListenerLuaTicksLimit(eventListenerluaTicksLimit));
  }

  public @Nullable List<? extends Entity> findEntities(String target) {
    try {
      CommandSource source = testEnv.getServer().getCommandSource();
      EntitySelectorParser parser = new EntitySelectorParser(new StringReader(target));
      EntitySelector selector = parser.parse();
      return selector.select(source);
    } catch (CommandSyntaxException e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  public void createTeam(String team) {
    testEnv.getTestPlayer().getWorldScoreboard().createTeam(team);
  }

  public void deleteTeams() {
    Scoreboard scoreBoard = testEnv.getTestPlayer().getWorldScoreboard();
    for (ScorePlayerTeam team : new ArrayList<>(scoreBoard.getTeams())) {
      scoreBoard.removeTeam(team);
    }
  }

  public void clearWizardConfigs() throws IOException {
    testEnv.getWol().getConfig().clearWizardConfigs();
  }

  public void createSharedModule(String moduleName, String content) {
    File moduleFile = getSharedModuleFile(moduleName);
    if (moduleFile.exists()) {
      moduleFile.delete();
    }
    moduleFile.getParentFile().mkdirs();
    try {
      Files.asCharSink(moduleFile, Charsets.UTF_8).write(content);
    } catch (IOException e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  public void deleteModule(String moduleName) {
    File moduleFile = getSharedModuleFile(moduleName);
    if (moduleFile.exists()) {
      moduleFile.delete();
    }
  }

  private File getSharedModuleFile(String moduleName) {
    String path = moduleName.replace(".", File.separator) + ".lua";
    return new File(testEnv.getWol().getConfig().getSharedLibDir(), path);
  }

  public void deleteSharedModule(String moduleName) {
    File file = getSharedModuleFile(moduleName);
    file.delete();
  }

  public void writeWorldFile(String path, String content) {
    FileSystem fs = testEnv.getWol().getWorldFileSystem();
    Path p = fs.getPath(path);
    try {
      Files.createParentDirs(p.toFile());
      Files.asCharSink(p.toFile(), Charsets.UTF_8).write(content);
    } catch (IOException e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  public String readWorldFile(String path) {
    FileSystem fs = testEnv.getWol().getWorldFileSystem();
    Path p = fs.getPath(path);
    try {
      if (java.nio.file.Files.exists(p)) {
        return Files.asByteSource(p.toFile()).asCharSource(Charsets.UTF_8).read();
      } else {
        return null;
      }
    } catch (IOException e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  public void deleteWorldFile(String path) {
    try {
      FileSystem fs = testEnv.getWol().getWorldFileSystem();
      delete(fs.getPath(path));
    } catch (IOException e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  private void delete(Path path) throws IOException {
    if (java.nio.file.Files.isDirectory(path)) {
      for (Path child : java.nio.file.Files.list(path).collect(Collectors.toList())) {
        delete(child);
      }
    }
    java.nio.file.Files.deleteIfExists(path);
  }

  public boolean existsWorldFile(String path) {
    FileSystem fs = testEnv.getWol().getWorldFileSystem();
    Path p = fs.getPath(path);
    return java.nio.file.Files.exists(p);
  }

  public void clearLuaFunctionCache() {
    testEnv.getWol().clearLuaFunctionCache();
  }

  public void bar(BlockPos startPos, EnumFacing direction, int meter, Block blockType) {
    checkArgument(meter >= 1, "meter must be greater than or equal to 1, but was %s!", meter);
    for (int i = 0; i < meter; ++i) {
      BlockPos pos = startPos.offset(direction, i);
      setBlock(pos, blockType);
    }
  }

  public WolTestEnvironment getTestEnv() {
    return testEnv;
  }

  public void setDoDaylightCycle(boolean value) {
    getOverworld().getGameRules().setOrCreateGameRule("doDaylightCycle",
        Boolean.valueOf(value).toString(), testEnv.getServer());
  }

  public boolean isDoDaylighCycle() {
    return getOverworld().getGameRules().getBoolean("doDaylightCycle");
  }

  public void setWorldTime(long value) {
    getOverworld().getWorldInfo().setWorldTotalTime(value);
  }

  public long getWorldtime() {
    return getOverworld().getGameTime();
  }

}
