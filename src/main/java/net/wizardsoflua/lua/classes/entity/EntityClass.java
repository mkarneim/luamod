package net.wizardsoflua.lua.classes.entity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.runtime.ExecutionContext;
import net.sandius.rembulan.runtime.ResolvedControlThrowable;
import net.wizardsoflua.lua.classes.DeclareLuaClass;
import net.wizardsoflua.lua.classes.ProxyCachingLuaClass;
import net.wizardsoflua.lua.classes.ProxyingLuaClass;
import net.wizardsoflua.lua.classes.common.LuaInstanceProxy;
import net.wizardsoflua.lua.function.NamedFunction1;
import net.wizardsoflua.lua.function.NamedFunction2;
import net.wizardsoflua.lua.function.NamedFunction3;
import net.wizardsoflua.lua.nbt.NbtConverter;

@DeclareLuaClass(name = EntityClass.NAME)
public class EntityClass extends ProxyCachingLuaClass<Entity, EntityClass.Proxy<?>> {
  public static final String NAME = "Entity";

  public EntityClass() {
    add(new MoveFunction());
    add(new PutNbtFunction());
    add(new AddTagFunction());
    add(new RemoveTagFunction());
    add(new ScanViewFunction());
    add(new DropItemFunction());
    add(new KillFunction());
  }

  @Override
  public Proxy<?> toLua(Entity delegate) {
    if (delegate instanceof EntityLivingBase) {
      return new EntityLivingBaseProxy<>(this, (EntityLivingBase) delegate);
    }
    return new Proxy<>(this, delegate);
  }

  public static class Proxy<D extends Entity> extends LuaInstanceProxy<D> {
    public Proxy(ProxyingLuaClass<?, ?> luaClass, D delegate) {
      super(luaClass, delegate);
      addReadOnly("dimension", () -> delegate.dimension);
      addReadOnly("uuid", this::getUuid);
      addReadOnly("alive", this::isAlive);
      add("name", this::getName, this::setName);
      add("pos", this::getPos, this::setPos);
      addReadOnly("nbt", this::getNbt);

      addReadOnly("facing", this::getFacing);
      add("lookVec", this::getLookVec, this::setLookVec);
      add("rotationYaw", this::getRotationYaw, this::setRotationYaw);
      add("rotationPitch", this::getRotationPitch, this::setRotationPitch);
      addReadOnly("eyeHeight", () -> delegate.getEyeHeight());
      add("motion", this::getMotion, this::setMotion);
      add("tags", this::getTags, this::setTags);

      // addReadOnly("world", this::getWorld);
    }

    @Override
    public boolean isTransferable() {
      return true;
    }

    public Object isAlive() {
      return getConverters().toLua(!delegate.isDead);
    }

    public Object getPos() {
      return getConverters().toLua(delegate.getPositionVector());
    }

    public void setPos(Object luaObj) {
      Vec3d pos = getConverters().toJava(Vec3d.class, luaObj, "pos");
      delegate.setPositionAndUpdate(pos.xCoord, pos.yCoord, pos.zCoord);
    }

    public Object getFacing() {
      EnumFacing result = delegate.getHorizontalFacing();
      return getConverters().toLua(result);
    }

    public @Nullable Object getLookVec() {
      Vec3d result = delegate.getLookVec();
      return getConverters().toLuaNullable(result);
    }

    public void setLookVec(Object luaObject) {
      Vec3d lookVec = getConverters().toJava(Vec3d.class, luaObject, "lookVec");
      double pitch = Math.toDegrees(Math.asin(-lookVec.yCoord));
      double yaw = Math.toDegrees(MathHelper.atan2(-lookVec.xCoord, lookVec.zCoord));
      setRotationYawAndPitch((float) yaw, (float) pitch);
    }

    public float getRotationYaw() {
      return MathHelper.wrapDegrees(delegate.rotationYaw);
    }

    /**
     * Sets the rotation yaw (rot-y) value.
     *
     * @param luaObj
     * @see Entity#readFromNBT(NBTTagCompound)
     */
    public void setRotationYaw(Object luaObj) {
      float yaw = getConverters().toJava(Float.class, luaObj, "rotationYaw");
      delegate.setRotationYawHead(yaw);
      delegate.setRenderYawOffset(yaw);
      delegate.setPositionAndRotation(delegate.posX, delegate.posY, delegate.posZ, yaw,
          delegate.rotationPitch);
    }

    public double getRotationPitch() {
      return delegate.rotationPitch;
    }

    public void setRotationPitch(Object luaObj) {
      float pitch = getConverters().toJava(Float.class, luaObj, "rotationPitch");
      delegate.setPositionAndRotation(delegate.posX, delegate.posY, delegate.posZ,
          delegate.rotationYaw, pitch);
    }

    public void setRotationYawAndPitch(float yaw, float pitch) {
      delegate.setRotationYawHead(yaw);
      delegate.setRenderYawOffset(yaw);
      delegate.setPositionAndRotation(delegate.posX, delegate.posY, delegate.posZ, yaw, pitch);
    }

    public Object getMotion() {
      double x = delegate.motionX;
      double y = delegate.motionY;
      double z = delegate.motionZ;
      Object result = getConverters().toLua(new Vec3d(x, y, z));
      return result;
    }

    public void setMotion(Object luaObj) {
      Vec3d v = getConverters().toJava(Vec3d.class, luaObj, "motion");
      double x = v.xCoord;
      double y = v.yCoord;
      double z = v.zCoord;

      // see SPacketEntityVelocity
      double maxLen = 3.9;
      double lenSqr = x * x + y * y + z * z;
      if (lenSqr > maxLen * maxLen) {
        double f = maxLen / Math.sqrt(lenSqr);
        x = x * f;
        y = y * f;
        z = z * f;
      }

      delegate.motionX = x;
      delegate.motionY = y;
      delegate.motionZ = z;
      delegate.velocityChanged = true;
    }

    public Object getTags() {
      Set<String> result = delegate.getTags();
      return getConverters().toLua(result);
    }

    public void setTags(Object luaObj) {
      Collection<String> tags = getConverters().toJavaList(String.class, luaObj, "tags");

      for (String oldTag : Lists.newArrayList(delegate.getTags())) {
        delegate.removeTag(oldTag);
      }
      for (String newTag : tags) {
        delegate.addTag(newTag);
      }
    }

    public Object getUuid() {
      return getConverters().toLua(delegate.getUniqueID().toString());
    }

    public Object getName() {
      return getConverters().toLua(delegate.getName());
    }

    public void setName(Object luaObj) {
      String name = getConverters().toJava(String.class, luaObj, "name");
      delegate.setCustomNameTag(name);
    }

    public Table getNbt() {
      NBTTagCompound nbt = new NBTTagCompound();
      delegate.writeToNBT(nbt);
      Table result = NbtConverter.toLua(nbt);
      return result;
    }

    public void putNbt(Table nbt) {
      NBTTagCompound oldNbt = delegate.serializeNBT();
      NBTTagCompound newNbt = getConverters().getNbtConverter().merge(oldNbt, nbt);
      delegate.readFromNBT(newNbt);
    }

    public void move(String directionName, @Nullable Double distance) {
      Direction direction = Direction.byName(directionName);
      checkNotNull(direction != null, "expected direction but got %s", direction);
      if (distance == null) {
        distance = 1d;
      }
      Vec3d vec = direction.getDirectionVec(getRotationYaw());
      double x = delegate.posX + vec.xCoord * distance;
      double y = delegate.posY + vec.yCoord * distance;
      double z = delegate.posZ + vec.zCoord * distance;
      delegate.setPositionAndUpdate(x, y, z);
    }

    public boolean addTag(String tag) {
      return delegate.addTag(tag);
    }

    public boolean removeTag(String tag) {
      return delegate.removeTag(tag);
    }

    public Object scanView(float distance) {
      Vec3d start = delegate.getPositionEyes(0);
      Vec3d end = start.add(delegate.getLookVec().scale(distance));
      RayTraceResult hit = delegate.getEntityWorld().rayTraceBlocks(start, end, false);
      Object result = getConverters().toLuaNullable(hit);
      return result;
    }

    public EntityItem dropItem(ItemStack item, float offsetY) {
      if (item.getCount() == 0) {
        throw new IllegalArgumentException("Can't drop an item with count==0");
      }
      return delegate.entityDropItem(item, offsetY);
    }

    public void kill() {
      delegate.onKillCommand();
    }

    // public Object getWorld() {
    // World world = delegate.getEntityWorld();
    // return getConverters().toLua(world);
    // }

  }

  public static class EntityLivingBaseProxy<D extends EntityLivingBase> extends Proxy<D> {
    public EntityLivingBaseProxy(ProxyingLuaClass<?, ?> luaClass, D delegate) {
      super(luaClass, delegate);
      add("mainhand", this::getMainhand, this::setMainhand);
      add("offhand", this::getOffhand, this::setOffhand);
    }

    @Override
    public float getRotationYaw() {
      float v = delegate.renderYawOffset;
      return MathHelper.wrapDegrees(v);
    }

    public @Nullable Object getMainhand() {
      ItemStack itemStack = delegate.getHeldItemMainhand();
      if (itemStack.isEmpty()) {
        return null;
      }
      return getConverters().toLuaNullable(itemStack);
    }

    public void setMainhand(@Nullable Object luaObj) {
      ItemStack itemStack = getConverters().toJavaNullable(ItemStack.class, luaObj, "mainhand");
      if (itemStack == null) {
        itemStack = ItemStack.EMPTY;
      }
      delegate.setHeldItem(EnumHand.MAIN_HAND, itemStack);
    }

    public @Nullable Object getOffhand() {
      ItemStack itemStack = delegate.getHeldItemOffhand();
      if (itemStack.isEmpty()) {
        return null;
      }
      return getConverters().toLuaNullable(itemStack);
    }

    public void setOffhand(@Nullable Object luaObj) {
      ItemStack itemStack = getConverters().toJavaNullable(ItemStack.class, luaObj, "offhand");
      if (itemStack == null) {
        itemStack = ItemStack.EMPTY;
      }
      delegate.setHeldItem(EnumHand.OFF_HAND, itemStack);
    }
  }

  private class MoveFunction extends NamedFunction3 {
    @Override
    public String getName() {
      return "move";
    }

    @Override
    public void invoke(ExecutionContext context, Object arg1, Object arg2, Object arg3)
        throws ResolvedControlThrowable {
      Proxy<?> proxy = getConverters().toJava(Proxy.class, arg1, 1, "self", getName());
      String direction = getConverters().toJava(String.class, arg2, 2, "direction", getName());
      Double distance =
          getConverters().toJavaNullable(Double.class, arg3, 3, "distance", getName());
      proxy.move(direction, distance);
      context.getReturnBuffer().setTo();
    }
  }

  private class PutNbtFunction extends NamedFunction2 {
    @Override
    public String getName() {
      return "putNbt";
    }

    @Override
    public void invoke(ExecutionContext context, Object arg1, Object arg2) {
      Proxy<?> proxy = getConverters().toJava(Proxy.class, arg1, 1, "self", getName());
      Table nbt = getConverters().toJava(Table.class, arg2, 2, "nbt", getName());
      proxy.putNbt(nbt);
      context.getReturnBuffer().setTo();
    }
  }

  private class AddTagFunction extends NamedFunction2 {
    @Override
    public String getName() {
      return "addTag";
    }

    @Override
    public void invoke(ExecutionContext context, Object arg1, Object arg2) {
      Proxy<?> proxy = getConverters().toJava(Proxy.class, arg1, 1, "self", getName());
      String tag = getConverters().toJava(String.class, arg2, 2, "tag", getName());
      boolean result = proxy.addTag(tag);
      context.getReturnBuffer().setTo(result);
    }
  }

  private class RemoveTagFunction extends NamedFunction2 {
    @Override
    public String getName() {
      return "removeTag";
    }

    @Override
    public void invoke(ExecutionContext context, Object arg1, Object arg2) {
      Proxy<?> proxy = getConverters().toJava(Proxy.class, arg1, 1, "self", getName());
      String tag = getConverters().toJava(String.class, arg2, 2, "tag", getName());
      boolean result = proxy.removeTag(tag);
      context.getReturnBuffer().setTo(result);
    }
  }

  private class ScanViewFunction extends NamedFunction2 {
    @Override
    public String getName() {
      return "scanView";
    }

    @Override
    public void invoke(ExecutionContext context, Object arg1, Object arg2) {
      Proxy<?> proxy = getConverters().toJava(Proxy.class, arg1, 1, "self", getName());
      float distance = getConverters().toJava(Float.class, arg2, 2, "distance", getName());
      Object result = proxy.scanView(distance);
      context.getReturnBuffer().setTo(result);
    }
  }

  private class DropItemFunction extends NamedFunction3 {
    @Override
    public String getName() {
      return "dropItem";
    }

    @Override
    public void invoke(ExecutionContext context, Object arg1, Object arg2, Object arg3) {
      Proxy<?> proxy = getConverters().toJava(Proxy.class, arg1, 1, "self", getName());
      ItemStack item = getConverters().toJava(ItemStack.class, arg2, 2, "item", getName());
      float offsetY =
          getConverters().toJavaOptional(Float.class, arg3, 3, "offsetY", getName()).orElse(0f);
      EntityItem result = proxy.dropItem(item, offsetY);
      context.getReturnBuffer().setTo(getConverters().toLuaNullable(result));
    }
  }

  private class KillFunction extends NamedFunction1 {
    @Override
    public String getName() {
      return "kill";
    }

    @Override
    public void invoke(ExecutionContext context, Object arg1) {
      Proxy<?> proxy = getConverters().toJava(Proxy.class, arg1, 1, "self", getName());
      proxy.kill();
      context.getReturnBuffer().setTo();
    }
  }
}
