package net.karneim.luamod.lua.wrapper;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.sandius.rembulan.ByteString;
import net.sandius.rembulan.impl.ImmutableTable;

public class WrapperFactory {
  public static ImmutableTable wrap(BlockPos delegate) {
    return new BlockPosWrapper(delegate).getLuaObject();
  }

  public static ByteString wrap(Enum<?> delegate) {
    return new EnumWrapper(delegate).getLuaObject();
  }

  public static ImmutableTable wrap(Iterable<String> delegate) {
    return new StringIterableWrapper(delegate).getLuaObject();
  }

  public static ImmutableTable wrap(Vec3d delegate) {
    return new Vec3dWrapper(delegate).getLuaObject();
  }
}