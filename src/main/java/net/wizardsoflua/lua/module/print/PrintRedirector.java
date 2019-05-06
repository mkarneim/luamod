package net.wizardsoflua.lua.module.print;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.lib.BasicLib;
import net.sandius.rembulan.runtime.LuaFunction;

public class PrintRedirector {
  public interface PrintReceiver {
    void send(String message);
  }

  public static PrintRedirector installInto(Table env, PrintReceiver printReceiver) {
    return new PrintRedirector(env, printReceiver);
  }

  private final PrintReceiver printReceiver;

  public PrintRedirector(Table env, PrintReceiver printReceiver) {
    this.printReceiver = printReceiver;
    OutputStream out = new ChatOutputStream();
    LuaFunction printFunc = BasicLib.print(out, env);
    env.rawset("print", printFunc);
  }

  private class ChatOutputStream extends org.apache.commons.io.output.ByteArrayOutputStream {
    @Override
    public void flush() throws IOException {
      String message = toString(Charset.defaultCharset());
      // Remove trailing line-feed.
      if (message.endsWith("\n")) {
        message = message.substring(0, message.length() - 1);
      }
      reset();
      print(message);
    }
  }

  private void print(String message) {
    printReceiver.send(TabEncoder.encode(message));
  }
}
