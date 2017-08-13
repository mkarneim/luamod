package net.wizardsoflua.lua.wrapper.entity;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.sandius.rembulan.ByteString;
import net.sandius.rembulan.Table;
import net.wizardsoflua.lua.wrapper.WrapperFactory;
import net.wizardsoflua.lua.wrapper.common.DelegatingWrapper;

public class EntityWrapper extends DelegatingWrapper {

  public static final String METATABLE_NAME = "Entity";

  private final Entity delegate;

  public EntityWrapper(WrapperFactory wrappers, Entity delegate) {
    super(wrappers, delegate);
    this.delegate = delegate;
    addReadOnly("dimension", () -> delegate.dimension);
    addReadOnly("uuid", this::getUuid);
    add("name", this::getName, this::setName);
    add("pos", this::getPos, this::setPos);

    setMetatable((Table) wrappers.getEnv().rawget(METATABLE_NAME));
  }

  public Table getPos() {
    return getWrappers().wrap(delegate.getPositionVector());
  }

  public void setPos(Object luaObj) {
    Vec3d pos = getWrappers().unwrapVec3(luaObj);
    delegate.setPositionAndUpdate(pos.xCoord, pos.yCoord, pos.zCoord);
  }

  public ByteString getUuid() {
    return getWrappers().wrap(delegate.getUniqueID().toString());
  }

  public ByteString getName() {
    return getWrappers().wrap(delegate.getName());
  }

  public void setName(Object luaObj) {
    String name = getWrappers().unwrapString(luaObj);
    delegate.setCustomNameTag(name);
  }

}