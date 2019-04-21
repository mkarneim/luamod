package net.wizardsoflua.tests;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wizardsoflua.testenv.MinecraftJUnitRunner;
import net.wizardsoflua.testenv.WolTestBase;
import net.wizardsoflua.testenv.event.ServerLog4jEvent;
import net.wizardsoflua.testenv.event.TestPlayerReceivedChatEvent;

@RunWith(MinecraftJUnitRunner.class)
public class SpellTest extends WolTestBase {
  private BlockPos playerPos = new BlockPos(0, 4, 0);
  private BlockPos posP1 = new BlockPos(1, 4, 1);
  private BlockPos posP2 = new BlockPos(1, 5, 1);

  @After
  public void clearBlock() {
    mc().setBlock(posP1, Blocks.AIR);
    mc().setBlock(posP2, Blocks.AIR);
  }


  // /test net.wizardsoflua.tests.SpellTest test_spell_is_not_nil
  @Test
  public void test_spell_is_not_nil() throws Exception {
    // Given:

    // When:
    mc().executeCommand("/lua print(spell~=nil)");

    // Then:
    ServerLog4jEvent act = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act.getMessage()).isEqualTo("true");
  }

  // /test net.wizardsoflua.tests.SpellTest test_spell_pos_is_world_spawn_point_casted_by_server
  @Test
  public void test_spell_pos_is_world_spawn_point_casted_by_server() throws Exception {
    // Given:
    BlockPos spawnPoint = mc().getWorldSpawnPoint();
    String expected = format(spawnPoint);

    // When:
    mc().executeCommand("/lua p=spell.pos; print(p)");

    // Then:
    ServerLog4jEvent act = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act.getMessage()).isEqualTo(expected);
  }

  // /test net.wizardsoflua.tests.SpellTest test_spell_facing_casted_by_server
  @Test
  public void test_spell_facing_casted_by_server() throws Exception {
    // Given:
    String expected = EnumFacing.WEST.getName();
    int facing = 4; // west
    String command = "/lua spell:execute('say '..spell.facing)";

    mc().executeCommand("/setblock %s %s %s minecraft:command_block %s replace {Command:\"%s\"}",
        posP1.getX(), posP1.getY(), posP1.getZ(), facing, command);
    mc().waitFor(ServerLog4jEvent.class);
    // When:
    mc().executeCommand("/setblock %s %s %s minecraft:redstone_block", posP2.getX(), posP2.getY(),
        posP2.getZ());
    mc().waitFor(ServerLog4jEvent.class);

    // Then:
    ServerLog4jEvent act = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act.getMessage()).contains(expected);
  }

  // /test net.wizardsoflua.tests.SpellTest test_spell_pos_when_casted_by_player
  @Test
  public void test_spell_pos_when_casted_by_player() throws Exception {
    // Given:
    mc().player().setPosition(playerPos);
    Vec3d lookPoint = mc().player().getPositionLookingAt();
    String expected = format(lookPoint);

    // When:
    mc().player().chat("/lua p=spell.pos; print(p)");


    // Then:
    TestPlayerReceivedChatEvent act = mc().waitFor(TestPlayerReceivedChatEvent.class);
    assertThat(act.getMessage()).isEqualTo(expected);
  }

  // /test net.wizardsoflua.tests.SpellTest test_spell_facing_when_casted_by_player
  @Test
  public void test_spell_facing_when_casted_by_player() throws Exception {
    // Given:
    EnumFacing orienation = mc().player().getFacing();
    String expected = orienation.getName();

    // When:
    mc().player().chat("/lua o=spell.facing; print(o)");

    // Then:
    TestPlayerReceivedChatEvent act = mc().waitFor(TestPlayerReceivedChatEvent.class);
    assertThat(act.getMessage()).isEqualTo(expected);
  }

  // /test net.wizardsoflua.tests.SpellTest test_spell_owner_is_not_nil_for_player
  @Test
  public void test_spell_owner_is_not_nil_for_player() throws Exception {
    // Given:

    // When:
    mc().player().chat("/lua print(spell.owner~=nil)");

    // Then:
    TestPlayerReceivedChatEvent act = mc().waitFor(TestPlayerReceivedChatEvent.class);
    assertThat(act.getMessage()).isEqualTo("true");
  }

  // /test net.wizardsoflua.tests.SpellTest test_spell_owner_is_nil_for_server
  @Test
  public void test_spell_owner_is_nil_for_server() throws Exception {
    // Given:

    // When:
    mc().executeCommand("/lua print(spell.owner==nil)");

    // Then:
    ServerLog4jEvent act = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act.getMessage()).isEqualTo("true");
  }

  // /test net.wizardsoflua.tests.SpellTest test_spell_owner_uuid_is_current_player_uuid
  @Test
  public void test_spell_owner_uuid_is_current_player_uuid() throws Exception {
    // Given:
    String expected = mc().player().getDelegate().getUniqueID().toString();

    // When:
    mc().player().chat("/lua print(spell.owner.uuid)");

    // Then:
    TestPlayerReceivedChatEvent act = mc().waitFor(TestPlayerReceivedChatEvent.class);
    assertThat(act.getMessage()).isEqualTo(expected);
  }

  // /test net.wizardsoflua.tests.SpellTest test_spell_owner_name_is_current_player_name
  @Test
  public void test_spell_owner_name_is_current_player_name() throws Exception {
    // Given:
    String expected = mc().player().getDelegate().getName().getString();

    // When:
    mc().player().chat("/lua print(spell.owner.name)");

    // Then:
    TestPlayerReceivedChatEvent act = mc().waitFor(TestPlayerReceivedChatEvent.class);
    assertThat(act.getMessage()).isEqualTo(expected);
  }

  // /test net.wizardsoflua.tests.SpellTest test_spell_owner_is_readonly
  @Test
  public void test_spell_owner_is_readonly() throws Exception {
    // Given:

    // When:
    mc().player().chat("/lua spell.owner = nil");

    // Then:
    TestPlayerReceivedChatEvent act = mc().waitFor(TestPlayerReceivedChatEvent.class);
    assertThat(act.getMessage())
        .startsWith("Error during spell execution: attempt to modify read-only table index");
  }

  // /test net.wizardsoflua.tests.SpellTest test_spell_execute_command_casted_by_server
  @Test
  public void test_spell_execute_command_casted_by_server() throws Exception {
    // Given:

    // When:
    mc().executeCommand("/lua spell:execute('/say hi')");

    // Then:
    ServerLog4jEvent act = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act.getMessage()).matches("\\[Spell-\\d+\\] hi");
  }

  // /test net.wizardsoflua.tests.SpellTest test_spell_execute_command_casted_by_player
  @Test
  public void test_spell_execute_command_casted_by_player() throws Exception {
    // Given:

    // When:
    mc().player().chat("/lua spell:execute('/say ho')");

    // Then:
    TestPlayerReceivedChatEvent act = mc().waitFor(TestPlayerReceivedChatEvent.class);
    String message = act.getMessage();
    assertThat(message).matches("\\[Spell-\\d+\\] ho");
  }

  // /test net.wizardsoflua.tests.SpellTest test_spell_can_intercept_its_own_break_event
  @Test
  public void test_spell_can_intercept_its_own_break_event() {
    // Given:

    // When:
    mc().executeCommand(
        "/lua spell.name='runner'; Events.on('SpellBreakEvent'):call(function(e) print('Gotcha'); end);");
    mc().executeCommand("/wol spell break byName runner");

    // Then:
    ServerLog4jEvent act = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act.getMessage()).isEqualTo("Gotcha");
  }

  // /test net.wizardsoflua.tests.SpellTest test_spell_forceChunk_is_true_by_default
  @Test
  public void test_spell_forceChunk_is_false_by_default() throws Exception {
    // Given:
    String expected = "true";

    // When:
    mc().player().chat("/lua print(spell.forceChunk)");

    // Then:
    TestPlayerReceivedChatEvent act = mc().waitFor(TestPlayerReceivedChatEvent.class);
    assertThat(act.getMessage()).isEqualTo(expected);
  }

  // /test net.wizardsoflua.tests.SpellTest test_spell_forceChunk_is_writable
  @Test
  public void test_spell_forceChunk_is_writable() throws Exception {
    // Given:
    String expected = "false";

    // When:
    mc().player().chat("/lua spell.forceChunk=false; print(spell.forceChunk)");

    // Then:
    TestPlayerReceivedChatEvent act = mc().waitFor(TestPlayerReceivedChatEvent.class);
    assertThat(act.getMessage()).isEqualTo(expected);
  }

}
