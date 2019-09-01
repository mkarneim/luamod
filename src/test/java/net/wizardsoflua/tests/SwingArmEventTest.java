package net.wizardsoflua.tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.wizardsoflua.testenv.WolTestBase;
import net.wizardsoflua.testenv.event.ServerLog4jEvent;

public class SwingArmEventTest extends WolTestBase {
  BlockPos playerPos = new BlockPos(0, 4, 0);
  BlockPos clickPos = new BlockPos(2, 5, 0);
  BlockPos blockPos = new BlockPos(1, 5, 0);

  @AfterEach
  @BeforeEach
  public void clearBlocks() {
    mc().setBlock(playerPos, Blocks.AIR);
    mc().setBlock(clickPos, Blocks.AIR);
    mc().setBlock(blockPos, Blocks.AIR);
  }

  // /test net.wizardsoflua.tests.SwingArmEventTest test_rightclick_MAIN_HAND
  @Test
  public void test_rightclick_MAIN_HAND() {
    // Given:
    mc().player().setPosition(playerPos);
    ItemStack item = new ItemStack(Blocks.SAND);
    mc().player().setMainHandItem(item);
    mc().setBlock(clickPos, Blocks.OBSIDIAN);
    EnumFacing facing = EnumFacing.WEST;
    String expected = EnumHand.MAIN_HAND.name();

    mc().executeCommand("/lua q=Events.collect('SwingArmEvent'); e=q:next(); print(e.hand)");

    // When:
    mc().player().rightclick(clickPos, facing);

    // Then:
    RightClickBlock act1 = mc().waitFor(RightClickBlock.class);
    assertThat(act1.getPos()).isEqualTo(clickPos);
    ServerLog4jEvent act = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act.getMessage()).isEqualTo(expected);
  }

  // /test net.wizardsoflua.tests.SwingArmEventTest test_rightclick_OFF_HAND
  @Test
  public void test_rightclick_OFF_HAND() {
    // Given:
    mc().player().setPosition(playerPos);
    ItemStack item = new ItemStack(Blocks.SAND);
    mc().player().setOffHandItem(item);
    mc().setBlock(clickPos, Blocks.OBSIDIAN);
    EnumFacing facing = EnumFacing.WEST;
    String expected = EnumHand.OFF_HAND.name();

    mc().executeCommand("/lua q=Events.collect('SwingArmEvent'); e=q:next(); print(e.hand)");

    // When:
    mc().player().rightclick(clickPos, facing);

    // Then:
    RightClickBlock act1 = mc().waitFor(RightClickBlock.class);
    assertThat(act1.getPos()).isEqualTo(clickPos);
    ServerLog4jEvent act = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act.getMessage()).isEqualTo(expected);
  }

  // /test net.wizardsoflua.tests.SwingArmEventTest test_leftclick
  @Test
  public void test_leftclick() {
    // Given:
    mc().player().setPosition(playerPos);
    BlockPos clickPos = new BlockPos(0, 3, 0);
    mc().setBlock(clickPos, Blocks.DIRT);
    EnumFacing facing = EnumFacing.UP;
    String expected = EnumHand.MAIN_HAND.name();

    mc().executeCommand("/lua q=Events.collect('SwingArmEvent'); e=q:next(); print(e.hand)");

    // When:
    mc().player().leftclick(clickPos, facing);

    // Then:
    ServerLog4jEvent act = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act.getMessage()).isEqualTo(expected);
  }

}
