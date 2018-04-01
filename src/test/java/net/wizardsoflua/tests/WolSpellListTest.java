package net.wizardsoflua.tests;

import org.junit.Test;
import org.junit.runner.RunWith;

import net.wizardsoflua.testenv.MinecraftJUnitRunner;
import net.wizardsoflua.testenv.WolTestBase;
import net.wizardsoflua.testenv.event.ServerLog4jEvent;
import net.wizardsoflua.testenv.event.TestPlayerReceivedChatEvent;

/**
 * Testing the "/wol spell list" command
 */
@RunWith(MinecraftJUnitRunner.class)
public class WolSpellListTest extends WolTestBase {
  private static final int MAX_LENGTH = 40;

  // /test net.wizardsoflua.tests.WolSpellListTest test_spell_list__Executed_by_Server
  @Test
  public void test_spell_list__Executed_by_Server() throws Exception {
    // Given:
    String code = "while true do sleep(20); end";
    String clientCode = "print('client'); " + code;
    String serverCode = "--[[server]] " + code;
    mc().player().chat("/lua %s", clientCode);
    mc().executeCommand("/lua %s", serverCode);
    assertThat(mc().waitFor(TestPlayerReceivedChatEvent.class).getMessage()).isEqualTo("client");

    // When:
    mc().executeCommand("/wol spell list");

    // Then:
    ServerLog4jEvent act = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act.getMessage()).startsWith("[WoL] Your active spells:\n");
    assertThat(act.getMessage()).doesNotContain(clientCode.substring(0, MAX_LENGTH));
    assertThat(act.getMessage()).contains(serverCode.substring(0, MAX_LENGTH));
  }

  // /test net.wizardsoflua.tests.WolSpellListTest test_spell_list__Executed_by_Player
  @Test
  public void test_spell_list__Executed_by_Player() throws Exception {
    // Given:
    String code = "while true do sleep(20); end";
    String clientCode = "--[[client]] " + code;
    String serverCode = "--[[server]] " + code;
    mc().player().chat("/lua %s", clientCode);
    mc().executeCommand("/lua %s", serverCode);

    // When:
    mc().player().chat("/wol spell list");

    // Then:
    TestPlayerReceivedChatEvent act = mc().waitFor(TestPlayerReceivedChatEvent.class);
    assertThat(act.getMessage()).startsWith("[WoL] Your active spells:\n");
    assertThat(act.getMessage()).contains(clientCode.substring(0, MAX_LENGTH));
    assertThat(act.getMessage()).doesNotContain(serverCode.substring(0, MAX_LENGTH));
  }

  // /test net.wizardsoflua.tests.WolSpellListTest test_spell_list_by_owner
  @Test
  public void test_spell_list_by_owner() throws Exception {
    // Given:
    String code = "while true do sleep(20); end";
    String clientCode = "--[[client]] " + code;
    String serverCode = "--[[server]] " + code;
    mc().player().chat("/lua %s", clientCode);
    mc().executeCommand("/lua %s", serverCode);
    String owner = mc().player().getName();

    // When:
    mc().player().chat("/wol spell list byOwner %s", owner);

    // Then:
    TestPlayerReceivedChatEvent act = mc().waitFor(TestPlayerReceivedChatEvent.class);
    assertThat(act.getMessage()).startsWith("[WoL] Active spells of " + owner + ":\n");
    assertThat(act.getMessage()).contains(clientCode.substring(0, MAX_LENGTH));
    assertThat(act.getMessage()).doesNotContain(serverCode.substring(0, MAX_LENGTH));
  }

  // /test net.wizardsoflua.tests.WolSpellListTest test_spell_list_all
  @Test
  public void test_spell_list_all() throws Exception {
    // Given:
    String code = "while true do sleep(20); end";
    String clientCode = "print('client'); " + code;
    String serverCode = "--[[server]] " + code;
    mc().player().chat("/lua %s", clientCode);
    mc().executeCommand("/lua %s", serverCode);
    assertThat(mc().nextClientMessage()).isEqualTo("client");

    // When:
    mc().executeCommand("/wol spell list all");

    // Then:
    ServerLog4jEvent act = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act.getMessage()).startsWith("[WoL] Active spells:\n");
    assertThat(act.getMessage()).contains(clientCode.substring(0, MAX_LENGTH));
    assertThat(act.getMessage()).contains(serverCode.substring(0, MAX_LENGTH));
  }

  // /test net.wizardsoflua.tests.WolSpellListTest test_spell_list_by_sid
  @Test
  public void test_spell_list_by_sid() throws Exception {
    // Given:
    String code = "print(spell.sid); while true do sleep(100); end";
    String clientCode = "--[[client]] " + code;
    String serverCode = "--[[server]] " + code;
    mc().player().chat("/lua %s", clientCode);
    mc().executeCommand("/lua %s", serverCode);
    String sid = mc().nextClientMessage();

    // When:
    mc().executeCommand("/wol spell list bySid %s", sid);

    // Then:
    ServerLog4jEvent act = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act.getMessage()).startsWith("[WoL] Active spells with sid " + sid + ":\n");
    assertThat(act.getMessage()).contains(clientCode.substring(0, MAX_LENGTH));
    assertThat(act.getMessage()).doesNotContain(serverCode.substring(0, MAX_LENGTH));
  }

  // /test net.wizardsoflua.tests.WolSpellListTest test_spell_list_by_name
  @Test
  public void test_spell_list_by_name() throws Exception {
    // Given:
    String code = "print(spell.name); while true do sleep(100); end";
    String clientCode = "--[[client]] " + code;
    String serverCode = "--[[server]] " + code;
    mc().player().chat("/lua %s", clientCode);
    mc().executeCommand("/lua %s", serverCode);
    String name = mc().nextClientMessage();

    // When:
    mc().executeCommand("/wol spell list byName %s", name);

    // Then:
    ServerLog4jEvent act = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act.getMessage()).startsWith("[WoL] Active spells with name '" + name + "':\n");
    assertThat(act.getMessage()).contains(clientCode.substring(0, MAX_LENGTH));
    assertThat(act.getMessage()).doesNotContain(serverCode.substring(0, MAX_LENGTH));
  }

}
