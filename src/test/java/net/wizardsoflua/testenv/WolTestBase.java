package net.wizardsoflua.testenv;

import org.junit.After;
import org.junit.Before;
import com.google.common.collect.Iterables;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

public class WolTestBase extends TestDataFactory {
  private WolTestEnvironment testEnv = WolTestEnvironment.instance;
  private MinecraftBackdoor mcBackdoor = new MinecraftBackdoor(testEnv, MinecraftForge.EVENT_BUS);
  private boolean wasOperator;
  private boolean oldDoDaylighCycle;
  private long oldDayTime;

  @Before
  public void beforeTest() throws Exception {
    testEnv.runAndWait(() -> testEnv.getEventRecorder().setEnabled(true));

    mc().resetClock();
    mc().breakAllSpells();

    mc().clearWizardConfigs();
    mc().executeCommand("/gamerule logAdminCommands false");
    mc().executeCommand("/gamerule sendCommandFeedback false");
    mc().executeCommand("/gamerule doMobSpawning false");
    mc().executeCommand("/kill @e[type=!Player]");
    mc().executeCommand("/wol spell break all");
    mc().clearEvents();
    mc().clearLuaFunctionCache();
    mc().player().setMainHandItem(null);
    mc().player().setOffHandItem(null);
    mc().player().clearInventory();
    oldDoDaylighCycle = mc().isDoDaylighCycle();
    wasOperator = mc().player().isOperator();
    oldDayTime = mc().getWorldtime();
  }

  @After
  public void afterTest() throws Exception {
    testEnv.runAndWait(() -> testEnv.getEventRecorder().setEnabled(false));
    mc().player().setOperator(wasOperator);
    mc().clearEvents();
    mc().breakAllSpells();
    mc().setDoDaylightCycle(oldDoDaylighCycle);
    mc().setWorldTime(oldDayTime);

    mc().resetClock();
  }

  protected MinecraftBackdoor mc() {
    return mcBackdoor;
  }

  protected Iterable<String> messagesOf(Iterable<ServerChatEvent> events) {
    return Iterables.transform(events, ServerChatEvent::getMessage);
  }

  protected Iterable<BlockPos> positionsOf(Iterable<RightClickBlock> events) {
    return Iterables.transform(events, RightClickBlock::getPos);
  }

  protected String format(BlockPos pos) {
    return formatPos((double) pos.getX(), (double) pos.getY(), (double) pos.getZ());
  }

  protected String format(Vec3d pos) {
    return formatPos(pos.x, pos.y, pos.z);
  }

  protected String format(float value) {
    return String.valueOf(value);
  }

  protected String formatPos(int x, int y, int z) {
    return "{" + x + ", " + y + ", " + z + "}";
  }

  protected String formatPos(double x, double y, double z) {
    return ("{" + x + ", " + y + ", " + z + "}").replace('E', 'e');
  }

  protected void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
