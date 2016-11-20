package net.karneim.luamod.cursor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.mockito.Mockito;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Cursor_Move_Facing_Test {

  private World world = Mockito.mock(World.class);
  private ICommandSender commandSender = Mockito.mock(ICommandSender.class);
  private Cursor underTest = new Cursor(commandSender, world);


  @Test
  public void test_move_North() {
    // Given:

    // When:
    underTest.move(EnumFacing.NORTH);

    // Then:
    assertThat(underTest.getPosition()).isEqualTo(new BlockPos(0, 0, -1));
  }

  @Test
  public void test_move_East() {
    // Given:

    // When:
    underTest.move(EnumFacing.EAST);

    // Then:
    assertThat(underTest.getPosition()).isEqualTo(new BlockPos(1, 0, 0));
  }

  @Test
  public void test_move_South() {
    // Given:

    // When:
    underTest.move(EnumFacing.SOUTH);

    // Then:
    assertThat(underTest.getPosition()).isEqualTo(new BlockPos(0, 0, 1));
  }

  @Test
  public void test_move_West() {
    // Given:

    // When:
    underTest.move(EnumFacing.WEST);

    // Then:
    assertThat(underTest.getPosition()).isEqualTo(new BlockPos(-1, 0, 0));
  }
}
