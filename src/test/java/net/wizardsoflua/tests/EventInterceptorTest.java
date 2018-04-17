package net.wizardsoflua.tests;

import org.junit.Test;
import org.junit.runner.RunWith;

import net.wizardsoflua.testenv.MinecraftJUnitRunner;
import net.wizardsoflua.testenv.WolTestBase;
import net.wizardsoflua.testenv.event.ServerLog4jEvent;

@RunWith(MinecraftJUnitRunner.class)
public class EventInterceptorTest extends WolTestBase {

  // /test net.wizardsoflua.tests.EventInterceptorTest test_stop__After_stopping_the_EventHandler_is_no_longer_called
  @Test
  public void test_stop__After_stopping_the_EventHandler_is_no_longer_called() {
    // When:
    mc().executeCommand("lua interceptor = Events.on('custom-event'):call(function(event)\n"//
        + "print(event.data)\n"//
        + "interceptor:stop()\n"//
        + "end)\n"//
    );
    mc().executeCommand("lua Events.fire('custom-event', 1)");
    mc().executeCommand("lua Events.fire('custom-event', 2)");
    mc().executeCommand("lua print(3)");

    // Then:
    ServerLog4jEvent act1 = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act1.getMessage()).isEqualTo("1");
    ServerLog4jEvent act2 = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act2.getMessage()).isEqualTo("3");
  }

  // /test net.wizardsoflua.tests.EventInterceptorTest test_stop__After_last_stopping_the_Spell_can_terminate
  @Test
  public void test_stop__After_last_stopping_the_Spell_can_terminate() {
    // When:
    mc().executeCommand("lua interceptor1 = Events.on('stop-1'):call(function(event)\n"//
        + "interceptor1:stop()\n"//
        + "end)\n"//
        + "interceptor2 = Events.on('stop-2'):call(function(event)\n"//
        + "interceptor2:stop()\n"//
        + "end)\n"//
    );
    sleep(1000);
    mc().executeCommand("wol spell list all");
    mc().executeCommand("lua Events.fire('stop-1')");
    sleep(1000);
    mc().executeCommand("wol spell list all");
    mc().executeCommand("lua Events.fire('stop-2')");
    sleep(1000);
    mc().executeCommand("wol spell list all");

    // Then:
    ServerLog4jEvent act1 = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act1.getMessage()).contains("interceptor1 = Events.on('stop-1')");
    ServerLog4jEvent act2 = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act2.getMessage()).contains("interceptor1 = Events.on('stop-1')");
    ServerLog4jEvent act3 = mc().waitFor(ServerLog4jEvent.class);
    assertThat(act3.getMessage()).isEqualTo("[WoL] Active spells:\n");
  }

}
