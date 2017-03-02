package net.karneim.luamod.lua;

import java.io.IOException;
import java.io.OutputStream;

import net.karneim.luamod.cursor.EnumDirection;
import net.karneim.luamod.lua.event.EventType;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.text.TextComponentString;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.impl.DefaultTable;
import net.sandius.rembulan.impl.NonsuspendableFunctionException;
import net.sandius.rembulan.lib.BasicLib;
import net.sandius.rembulan.runtime.AbstractFunctionAnyArg;
import net.sandius.rembulan.runtime.ExecutionContext;
import net.sandius.rembulan.runtime.LuaFunction;
import net.sandius.rembulan.runtime.ResolvedControlThrowable;

public class LuaModLib {

  public static LuaModLib installInto(Table env, ICommandSender owner) {
    return new LuaModLib(env, owner);
  }

  private ICommandSender owner;

  public LuaModLib(Table env, ICommandSender owner) {
    this.owner = owner;
    for (EnumDirection e : EnumDirection.values()) {
      env.rawset(e.name(), e.name());
    }
    for (EnumFacing e : EnumFacing.values()) {
      env.rawset(e.name(), e.name());
    }
    for (Rotation e : Rotation.values()) {
      env.rawset(e.name(), e.name());
    }
    for (EventType e : EventType.values()) {
      env.rawset(e.name(), e.name());
    }
    env.rawset("SURFACE","SURFACE");
    //env.rawset("print", new PrintFunction());
    
    OutputStream out = new ChatOutputStream();
    LuaFunction printFunc = BasicLib.print(out, env);
    env.rawset("print", printFunc);
  }

  private class ChatOutputStream extends org.apache.commons.io.output.ByteArrayOutputStream {
    @Override
    public void flush() throws IOException {
      String message = toString();
      reset();
      print(message);
    }
  }
  
  void print(String message) {
    owner.addChatMessage(new TextComponentString(message));
  }
}
