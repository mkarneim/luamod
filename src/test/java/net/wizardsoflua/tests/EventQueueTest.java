package net.wizardsoflua.tests;

import org.junit.jupiter.api.Test;
import net.wizardsoflua.testenv.WolTestBase;

public class EventQueueTest extends WolTestBase {

  // /test net.wizardsoflua.tests.EventQueueTest test_latest__Two_custom_Events
  @Test
  public void test_latest__Two_custom_Events() {
    // When:
    mc().executeCommand("lua q=Events.collect('custom-event')\n"//
        + "Events.collect('continue'):next()\n"//
        + "e=q:latest()\n"//
        + "print(e.data)\n"//
        + "print(str(q:isEmpty()))\n"//
    );
    mc().executeCommand("lua Events.fire('custom-event', 1)");
    mc().executeCommand("lua Events.fire('custom-event', 2)");
    mc().executeCommand("lua Events.fire('continue')");

    // Then:
    assertThat(mc().nextServerMessage()).isEqualTo("2");
    assertThat(mc().nextServerMessage()).isEqualTo("true");
  }

  // /test net.wizardsoflua.tests.EventQueueTest test_latest__No_event
  @Test
  public void test_latest__No_event() {
    // When:
    mc().executeCommand(
        "/lua q=Events.collect('LeftClickBlockEvent'); sleep(20); e=q:latest(); print(str(e == nil))");

    // Then:
    assertThat(mc().nextServerMessage()).isEqualTo("true");
  }

  // /test net.wizardsoflua.tests.EventQueueTest test_next_with_timeout_of_zero_does_not_wait
  @Test
  public void test_next_with_timeout_of_zero_does_not_wait() {
    // Given:

    // When:
    mc().executeCommand(
        "/lua q=Events.collect('ChatEvent'); t1=Time.gametime; e=q:next(0); t2=Time.gametime; print(t2-t1)");

    // Then:
    assertThat(mc().nextServerMessage()).isEqualTo("0");
  }

  // /test net.wizardsoflua.tests.EventQueueTest
  // test_next_with_timeout_of_one_does_wait_one_gametick
  @Test
  public void test_next_with_timeout_of_one_does_wait_one_gametick() {
    // Given:

    // When:
    mc().executeCommand(
        "/lua q=Events.collect('ChatEvent'); t1=Time.gametime; e=q:next(1); t2=Time.gametime; print(t2-t1)");

    // Then:
    assertThat(mc().nextServerMessage()).isEqualTo("1");
  }

  // /test net.wizardsoflua.tests.EventQueueTest
  // test_next_with_timeout_of_zero_returns_event_in_same_gametick
  @Test
  public void test_next_with_timeout_of_zero_returns_event_in_same_gametick() {
    // Given:

    // When:
    mc().executeCommand(
        "/lua q=Events.collect('DummyEvent'); sleep(10); t1=Time.gametime; for i=1,5 do e=q:next(0); if not e then error('e is nil'); end; end; t2=Time.gametime; print(t2-t1)");

    mc().executeCommand("/lua for i=1,5 do Events.fire('DummyEvent'); end");

    // Then:
    assertThat(mc().nextServerMessage()).isEqualTo("0");
  }

  // /test net.wizardsoflua.tests.EventQueueTest
  // test_next_with_timeout_of_one_returns_event_in_same_gametick
  @Test
  public void test_next_with_timeout_of_one_returns_event_in_same_gametick() {
    // Given:

    // When:
    mc().executeCommand(
        "/lua q=Events.collect('DummyEvent'); sleep(10); t1=Time.gametime; for i=1,5 do e=q:next(1); if not e then error('e is nil'); end; end; t2=Time.gametime; print(t2-t1)");

    mc().executeCommand("/lua for i=1,5 do Events.fire('DummyEvent'); end");

    // Then:
    assertThat(mc().nextServerMessage()).isEqualTo("0");
  }

}
