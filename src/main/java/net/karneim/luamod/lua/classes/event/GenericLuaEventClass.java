package net.karneim.luamod.lua.classes.event;

import net.karneim.luamod.lua.classes.AbstractLuaType;
import net.karneim.luamod.lua.classes.Constants;
import net.karneim.luamod.lua.classes.ModulePackage;
import net.karneim.luamod.lua.classes.TypeName;
import net.karneim.luamod.lua.event.GenericLuaEventInstance;
import net.karneim.luamod.lua.wrapper.Metatables;
import net.sandius.rembulan.StateContext;
import net.sandius.rembulan.Variable;
import net.sandius.rembulan.exec.CallException;
import net.sandius.rembulan.exec.CallPausedException;
import net.sandius.rembulan.exec.DirectCallExecutor;
import net.sandius.rembulan.load.ChunkLoader;
import net.sandius.rembulan.load.LoaderException;
import net.sandius.rembulan.runtime.LuaFunction;

@TypeName("GenericEvent")
@ModulePackage(Constants.MODULE_PACKAGE)
public class GenericLuaEventClass extends AbstractLuaType {

  public void installInto(ChunkLoader loader, DirectCallExecutor executor, StateContext state)
      throws LoaderException, CallException, CallPausedException, InterruptedException {
    LuaFunction classFunc = loader.loadTextChunk(new Variable(getRepo().getEnv()), getTypeName(),
        String.format("require \"%s\"", getModule()));
    executor.call(state, classFunc);
  }

  public GenericLuaEventInstance newInstance(Object delegate, String name) {
    return new GenericLuaEventInstance(getRepo(), delegate, name,
        Metatables.get(getRepo().getEnv(), getTypeName()));
  }

}