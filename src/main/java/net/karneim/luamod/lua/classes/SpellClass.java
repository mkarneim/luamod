package net.karneim.luamod.lua.classes;

import java.io.StringWriter;
import java.util.List;

import org.apache.commons.io.output.WriterOutputStream;

import com.google.common.collect.Lists;

import net.karneim.luamod.TabEncoder;
import net.karneim.luamod.cursor.EnumDirection;
import net.karneim.luamod.cursor.Selection;
import net.karneim.luamod.cursor.Spell;
import net.karneim.luamod.lua.util.wrapper.DelegatingTableWrapper;
import net.karneim.luamod.lua.wrapper.Metatables;
import net.karneim.luamod.lua.wrapper.SpellInstance;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.sandius.rembulan.StateContext;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.Variable;
import net.sandius.rembulan.exec.CallException;
import net.sandius.rembulan.exec.CallPausedException;
import net.sandius.rembulan.exec.DirectCallExecutor;
import net.sandius.rembulan.impl.DefaultTable;
import net.sandius.rembulan.impl.NonsuspendableFunctionException;
import net.sandius.rembulan.lib.BasicLib;
import net.sandius.rembulan.lib.TableLib;
import net.sandius.rembulan.load.ChunkLoader;
import net.sandius.rembulan.load.LoaderException;
import net.sandius.rembulan.runtime.AbstractFunction1;
import net.sandius.rembulan.runtime.AbstractFunction2;
import net.sandius.rembulan.runtime.AbstractFunction3;
import net.sandius.rembulan.runtime.AbstractFunction4;
import net.sandius.rembulan.runtime.AbstractFunctionAnyArg;
import net.sandius.rembulan.runtime.ExecutionContext;
import net.sandius.rembulan.runtime.LuaFunction;
import net.sandius.rembulan.runtime.ResolvedControlThrowable;

public class SpellClass {

  private final String classname = "Spell";
  public final String module = "net.karneim.luamod.lua.classes." + classname;

  private static final SpellClass SINGLETON = new SpellClass();

  public static SpellClass get() {
    return SINGLETON;
  }

  public void installInto(Table env, ChunkLoader loader, DirectCallExecutor executor,
      StateContext state)
      throws LoaderException, CallException, CallPausedException, InterruptedException {
    LuaFunction classFunc =
        loader.loadTextChunk(new Variable(env), classname, String.format("require \"%s\"", module));
    executor.call(state, classFunc);
    addFunctions(env);
  }

  public SpellInstance newInstance(Table env, Spell delegate) {
    return new SpellInstance(env, delegate, Metatables.get(env, classname));
  }

  private void addFunctions(Table env) {
    Table metatable = Metatables.get(env, classname);
    metatable.rawset("move", new MoveFunction());
    metatable.rawset("moveBy", new MoveByFunction());
    metatable.rawset("rotate", new RotateFunction());
    metatable.rawset("say", new SayFunction(env));
    metatable.rawset("msg", new MsgFunction(env));
    metatable.rawset("execute", new ExecuteFunction());
    metatable.rawset("reset", new ResetFunction());
    metatable.rawset("resetRotation", new ResetRotationFunction());
    metatable.rawset("resetPosition", new ResetPositionFunction());
    metatable.rawset("pushLocation", new PushLocationFunction());
    metatable.rawset("popLocation", new PopLocationFunction());
    metatable.rawset("cut", new CutFunction());
    metatable.rawset("copy", new CopyFunction());
    metatable.rawset("paste", new PasteFunction());
  }

  private class MoveFunction extends AbstractFunction3 {

    @Override
    public void invoke(ExecutionContext context, Object arg0, Object arg1, Object arg2)
        throws ResolvedControlThrowable {
      if (arg0 == null) {
        throw new IllegalArgumentException(String.format("Spell expected but got nil!"));
      }
      if (arg2 == null) {
        arg2 = 1;
      }
      if (!(arg2 instanceof Number)) {
        throw new IllegalArgumentException(
            String.format("Integer value expected but got %s!", arg2));
      }
      Spell delegate = DelegatingTableWrapper.getDelegate(Spell.class, arg0);
      int length = ((Number) arg2).intValue();
      EnumFacing x = EnumFacing.byName(String.valueOf(arg1));
      if (x != null) {
        delegate.move(x, length);
      } else {
        EnumDirection y = EnumDirection.byName(String.valueOf(arg1));
        if (y != null) {
          delegate.move(y, length);
        } else if ("SURFACE".equals(String.valueOf(arg1))) {
          delegate.move(delegate.getSurface(), length);
        } else {
          throw new IllegalArgumentException(
              String.format("Direction value expected but got %s!", arg1));
        }
      }
      context.getReturnBuffer().setTo();
    }

    @Override
    public void resume(ExecutionContext context, Object suspendedState)
        throws ResolvedControlThrowable {
      throw new NonsuspendableFunctionException();
    }
  }

  private class MoveByFunction extends AbstractFunction4 {

    @Override
    public void invoke(ExecutionContext context, Object arg0, Object arg1, Object arg2, Object arg3)
        throws ResolvedControlThrowable {
      if (arg0 == null) {
        throw new IllegalArgumentException(String.format("Spell expected but got nil!"));
      }
      if (arg1 == null || !(arg1 instanceof Number)) {
        throw new IllegalArgumentException(
            String.format("Integer value for dx expected but got %s!", arg1));
      }
      if (arg2 == null || !(arg2 instanceof Number)) {
        throw new IllegalArgumentException(
            String.format("Integer value for dy expected but got %s!", arg2));
      }
      if (arg3 == null || !(arg3 instanceof Number)) {
        throw new IllegalArgumentException(
            String.format("Integer value for dz expected but got %s!", arg3));
      }
      Spell delegate = DelegatingTableWrapper.getDelegate(Spell.class, arg0);
      int dx = ((Number) arg1).intValue();
      int dy = ((Number) arg2).intValue();
      int dz = ((Number) arg3).intValue();
      delegate.moveBy(dx, dy, dz);
      context.getReturnBuffer().setTo();
    }

    @Override
    public void resume(ExecutionContext context, Object suspendedState)
        throws ResolvedControlThrowable {
      throw new NonsuspendableFunctionException();
    }
  }



  private class RotateFunction extends AbstractFunction2 {

    @Override
    public void invoke(ExecutionContext context, Object arg0, Object arg1)
        throws ResolvedControlThrowable {
      if (arg0 == null) {
        throw new IllegalArgumentException(String.format("Spell expected but got nil!"));
      }
      Spell delegate = DelegatingTableWrapper.getDelegate(Spell.class, arg0);
      Rotation x = rotationByName(String.valueOf(arg1));
      if (x != null) {
        delegate.rotate(x);
      } else {
        throw new IllegalArgumentException(
            String.format("Rotation value expected but got %s!", arg1));
      }
      context.getReturnBuffer().setTo();
    }

    private Rotation rotationByName(String name) {
      return Rotation.valueOf(name);
    }

    @Override
    public void resume(ExecutionContext context, Object suspendedState)
        throws ResolvedControlThrowable {
      throw new NonsuspendableFunctionException();
    }
  }


  private class SayFunction extends AbstractFunctionAnyArg {

    private final Table env;

    public SayFunction(Table env) {
      this.env = env;
    }

    @Override
    public void invoke(ExecutionContext context, Object[] args) throws ResolvedControlThrowable {
      Object arg0 = args[0];
      if (arg0 == null) {
        throw new IllegalArgumentException(String.format("Spell expected but got nil!"));
      }
      Spell delegate = DelegatingTableWrapper.getDelegate(Spell.class, arg0);
      List<Object> values = Lists.newArrayList(args);
      values.remove(0);

      String text = format(context, env, values);
      delegate.say(text);
      context.getReturnBuffer().setTo();
    }

    @Override
    public void resume(ExecutionContext context, Object suspendedState)
        throws ResolvedControlThrowable {
      throw new NonsuspendableFunctionException();
    }
  }

  private class MsgFunction extends AbstractFunction3 {

    private final Table env;

    public MsgFunction(Table env) {
      this.env = env;
    }

    @Override
    public void invoke(ExecutionContext context, Object arg0, Object arg1, Object arg2)
        throws ResolvedControlThrowable {
      if (arg0 == null) {
        throw new IllegalArgumentException(String.format("Spell expected but got nil!"));
      }
      if (arg1 == null) {
        throw new IllegalArgumentException(
            String.format("Entity name value expected but got nil!"));
      }
      if (arg2 == null) {
        throw new IllegalArgumentException(String.format("Text value expected but got nil!"));
      }
      Spell delegate = DelegatingTableWrapper.getDelegate(Spell.class, arg0);
      String username = String.valueOf(arg1);
      
      String text = format(context, env, Lists.newArrayList(arg2));
      delegate.msg(username, text);
      context.getReturnBuffer().setTo();
    }

    @Override
    public void resume(ExecutionContext context, Object suspendedState)
        throws ResolvedControlThrowable {
      throw new NonsuspendableFunctionException();
    }
  }

  private class ExecuteFunction extends AbstractFunction2 {

    @Override
    public void invoke(ExecutionContext context, Object arg0, Object arg1)
        throws ResolvedControlThrowable {
      if (arg0 == null) {
        throw new IllegalArgumentException(String.format("Spell expected but got nil!"));
      }
      if (arg1 == null) {
        throw new IllegalArgumentException(String.format("Text value expected but got nil!"));
      }
      Spell delegate = DelegatingTableWrapper.getDelegate(Spell.class, arg0);
      String command = String.valueOf(arg1);
      int result = delegate.execute(command);
      context.getReturnBuffer().setTo(result);
    }

    @Override
    public void resume(ExecutionContext context, Object suspendedState)
        throws ResolvedControlThrowable {
      throw new NonsuspendableFunctionException();
    }
  }

  private class ResetFunction extends AbstractFunction1 {

    @Override
    public void invoke(ExecutionContext context, Object arg0) throws ResolvedControlThrowable {
      if (arg0 == null) {
        throw new IllegalArgumentException(String.format("Spell expected but got nil!"));
      }
      Spell delegate = DelegatingTableWrapper.getDelegate(Spell.class, arg0);
      delegate.reset();
      context.getReturnBuffer().setTo();
    }

    @Override
    public void resume(ExecutionContext context, Object suspendedState)
        throws ResolvedControlThrowable {
      throw new NonsuspendableFunctionException();
    }
  }

  private class ResetRotationFunction extends AbstractFunction1 {

    @Override
    public void invoke(ExecutionContext context, Object arg0) throws ResolvedControlThrowable {
      if (arg0 == null) {
        throw new IllegalArgumentException(String.format("Spell expected but got nil!"));
      }
      Spell delegate = DelegatingTableWrapper.getDelegate(Spell.class, arg0);
      delegate.resetRotation();
      context.getReturnBuffer().setTo();
    }

    @Override
    public void resume(ExecutionContext context, Object suspendedState)
        throws ResolvedControlThrowable {
      throw new NonsuspendableFunctionException();
    }
  }

  private class ResetPositionFunction extends AbstractFunction1 {

    @Override
    public void invoke(ExecutionContext context, Object arg0) throws ResolvedControlThrowable {
      if (arg0 == null) {
        throw new IllegalArgumentException(String.format("Spell expected but got nil!"));
      }
      Spell delegate = DelegatingTableWrapper.getDelegate(Spell.class, arg0);
      delegate.resetPosition();
      context.getReturnBuffer().setTo();
    }

    @Override
    public void resume(ExecutionContext context, Object suspendedState)
        throws ResolvedControlThrowable {
      throw new NonsuspendableFunctionException();
    }
  }

  private class PushLocationFunction extends AbstractFunction1 {

    @Override
    public void invoke(ExecutionContext context, Object arg0) throws ResolvedControlThrowable {
      if (arg0 == null) {
        throw new IllegalArgumentException(String.format("Spell expected but got nil!"));
      }
      Spell delegate = DelegatingTableWrapper.getDelegate(Spell.class, arg0);
      delegate.pushLocation();
      context.getReturnBuffer().setTo();
    }

    @Override
    public void resume(ExecutionContext context, Object suspendedState)
        throws ResolvedControlThrowable {
      throw new NonsuspendableFunctionException();
    }
  }

  private class PopLocationFunction extends AbstractFunction1 {

    @Override
    public void invoke(ExecutionContext context, Object arg0) throws ResolvedControlThrowable {
      if (arg0 == null) {
        throw new IllegalArgumentException(String.format("Spell expected but got nil!"));
      }
      Spell delegate = DelegatingTableWrapper.getDelegate(Spell.class, arg0);
      boolean result = delegate.popLocation();
      context.getReturnBuffer().setTo(result);
    }

    @Override
    public void resume(ExecutionContext context, Object suspendedState)
        throws ResolvedControlThrowable {
      throw new NonsuspendableFunctionException();
    }
  }
  
 

  private class CutFunction extends AbstractFunction2 {

    @Override
    public void invoke(ExecutionContext context, Object arg0, Object arg1)
        throws ResolvedControlThrowable {
      if (arg0 == null) {
        throw new IllegalArgumentException(String.format("Spell expected but got nil!"));
      }
      if (arg1 == null) {
        throw new IllegalArgumentException(String.format("Array of vectors expected but got nil!"));
      }
      if (!(arg1 instanceof Table)) {
        throw new IllegalArgumentException(
            String.format("Array of vectors expected but got %s!", arg1));
      }
      Spell delegate = DelegatingTableWrapper.getDelegate(Spell.class, arg0);
      Table table = (Table) arg1;

      Selection selection = new Selection();
      Object k = table.initialKey();
      while (k != null) {
        // process the key k
        Object v = table.rawget(k);
        BlockPos pos = toBlockPos(v);
        selection.add(pos);
        k = table.successorKeyOf(k);
      }
      String id = delegate.cutSelection(selection);

      context.getReturnBuffer().setTo(id);
    }

    @Override
    public void resume(ExecutionContext context, Object suspendedState)
        throws ResolvedControlThrowable {
      throw new NonsuspendableFunctionException();
    }
  }

  private class CopyFunction extends AbstractFunction2 {

    @Override
    public void invoke(ExecutionContext context, Object arg0, Object arg1)
        throws ResolvedControlThrowable {
      if (arg0 == null) {
        throw new IllegalArgumentException(String.format("Spell expected but got nil!"));
      }
      if (arg1 == null) {
        throw new IllegalArgumentException(String.format("Array of vectors expected but got nil!"));
      }
      if (!(arg1 instanceof Table)) {
        throw new IllegalArgumentException(
            String.format("Array of vectors expected but got %s!", arg1));
      }
      Spell delegate = DelegatingTableWrapper.getDelegate(Spell.class, arg0);
      Table table = (Table) arg1;

      Selection selection = new Selection();
      Object k = table.initialKey();
      while (k != null) {
        // process the key k
        Object v = table.rawget(k);
        BlockPos pos = toBlockPos(v);
        selection.add(pos);
        k = table.successorKeyOf(k);
      }
      String id = delegate.copySelection(selection);

      context.getReturnBuffer().setTo(id);
    }

    @Override
    public void resume(ExecutionContext context, Object suspendedState)
        throws ResolvedControlThrowable {
      throw new NonsuspendableFunctionException();
    }
  }

  private class PasteFunction extends AbstractFunction2 {

    @Override
    public void invoke(ExecutionContext context, Object arg0, Object arg1)
        throws ResolvedControlThrowable {
      if (arg0 == null) {
        throw new IllegalArgumentException(String.format("Spell expected but got nil!"));
      }
      if (arg1 == null) {
        throw new IllegalArgumentException(
            String.format("String being a snapshot id expected, but got nil!"));
      }
      Spell delegate = DelegatingTableWrapper.getDelegate(Spell.class, arg0);
      String id = String.valueOf(arg1);

      Selection resultSelection = delegate.paste(id);

      Table result = DefaultTable.factory().newTable();
      long idx = 0;
      for (BlockPos pos : resultSelection.getPositions()) {
        idx++;
        Table vec3d = toVec3dTable(pos);
        result.rawset(idx, vec3d);
      }
      context.getReturnBuffer().setTo(result);
    }

    private Table toVec3dTable(BlockPos pos) {
      Table result = DefaultTable.factory().newTable();
      result.rawset("x", pos.getX());
      result.rawset("y", pos.getY());
      result.rawset("z", pos.getZ());
      return result;
    }

    @Override
    public void resume(ExecutionContext context, Object suspendedState)
        throws ResolvedControlThrowable {
      throw new NonsuspendableFunctionException();
    }
  }

  private BlockPos toBlockPos(Object v) {
    if (!(v instanceof Table)) {
      throw new IllegalArgumentException(String.format("Vec3 expected, but got %s!", v));
    }
    Table table = (Table) v;
    Object ox = table.rawget("x");
    Object oy = table.rawget("y");
    Object oz = table.rawget("z");
    int x = ((Number) ox).intValue();
    int y = ((Number) oy).intValue();
    int z = ((Number) oz).intValue();

    return new BlockPos(x, y, z);
  }

  private String format(ExecutionContext context, Table env, List<Object> args)
      throws ResolvedControlThrowable {
    StringWriter writer = new StringWriter();
    WriterOutputStream out = new WriterOutputStream(writer);
    LuaFunction printFunc = BasicLib.print(out, env);
    Object[] argArray = args.toArray();
    printFunc.invoke(context, argArray);
    return TabEncoder.encode(writer.toString());
  }

}
