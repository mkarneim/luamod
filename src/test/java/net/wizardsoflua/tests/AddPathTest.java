package net.wizardsoflua.tests;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import net.wizardsoflua.testenv.MinecraftJUnitRunner;
import net.wizardsoflua.testenv.WolTestBase;
import net.wizardsoflua.testenv.event.ServerLog4jEvent;

@RunWith(MinecraftJUnitRunner.class)
public class AddPathTest extends WolTestBase {

  private static final String DEMOMODULE = "my.demomodule";

  @After
  public void after() {
    mc().player().deleteModule(DEMOMODULE);
  }

  // /test net.wizardsoflua.tests.AddPathTest test_server_can_call_player_module
  @Test
  public void test_server_can_call_player_module() throws Exception {
    // Given:
    mc().player().createModule(DEMOMODULE, "function dummy() print('hello') end");

    // When:
    mc().executeCommand("/lua addpath('%s'); require('%s'); dummy()", mc().player().getName(),
        DEMOMODULE);

    // Then:
    ServerLog4jEvent act = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act.getMessage()).isEqualTo("hello");
  }

}
