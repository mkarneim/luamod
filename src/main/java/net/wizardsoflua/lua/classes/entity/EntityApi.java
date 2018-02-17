package net.wizardsoflua.lua.classes.entity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.sandius.rembulan.Table;
import net.wizardsoflua.annotation.LuaFunction;
import net.wizardsoflua.annotation.LuaModule;
import net.wizardsoflua.annotation.LuaProperty;
import net.wizardsoflua.lua.classes.ObjectClass;
import net.wizardsoflua.lua.classes.ProxyingLuaClass;
import net.wizardsoflua.scribble.LuaApiBase;

/**
 * The Entity class is the base class of all entities that populate the world.
 */
@LuaModule(name = EntityApi.NAME, superClass = ObjectClass.class)
public class EntityApi<D extends Entity> extends LuaApiBase<D> {
  public static final String NAME = "Entity";

  public EntityApi(ProxyingLuaClass<?, ?> luaClass, D delegate) {
    super(luaClass, delegate);
  }

  /**
   * This is true, if this entity is alive, false otherwise.
   */
  @LuaProperty
  public boolean isAlive() {
    return !delegate.isDead;
  }

  /**
   * The 'dimension' is a magic number that tells us something about the world where this entity
   * currently is living in. 0 means the Overworld. -1 is the Nether, and 1 is the End.
   */
  @LuaProperty
  public int getDimension() {
    return delegate.dimension;
  }

  /**
   * The 'eyeHeight' is the distance from this entity's feet to its eyes in Y direction.
   */
  @LuaProperty
  public float getEyeHeight() {
    return delegate.getEyeHeight();
  }

  /**
   * The 'facing' is the compass direction this entity is facing. This is one of 'north', 'east',
   * 'south', and 'west'.
   */
  @LuaProperty
  public EnumFacing getFacing() {
    return delegate.getHorizontalFacing();
  }

  /**
   * The 'lookVec' is a 3-dimensional vector that points into the direction this entity is looking
   * at, or nil, if it is not looking anywhere, for example, if it has no eyes.
   */
  @LuaProperty(type = "Vec3")
  public @Nullable Vec3d getLookVec() {
    return delegate.getLookVec();
  }

  @LuaProperty(type = "Vec3")
  public void setLookVec(Vec3d lookVec) {
    double pitch = Math.toDegrees(Math.asin(-lookVec.yCoord));
    double yaw = Math.toDegrees(MathHelper.atan2(-lookVec.xCoord, lookVec.zCoord));
    setRotationYawAndPitch((float) yaw, (float) pitch);
  }

  /**
   * The 'motion' is a 3-dimensional vector that represents the velocity of this entity when it is
   * moved by some external force, e.g. when it is falling or when it is pushed by an explosion.
   */
  @LuaProperty(type = "Vec3")
  public Vec3d getMotion() {
    double x = delegate.motionX;
    double y = delegate.motionY;
    double z = delegate.motionZ;
    return new Vec3d(x, y, z);
  }

  @LuaProperty(type = "Vec3")
  public void setMotion(Vec3d motion) {
    double x = motion.xCoord;
    double y = motion.yCoord;
    double z = motion.zCoord;

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

  /**
   * The 'name' of this entity is unlike the UUID not unique in the world. For most entities it is
   * just something like 'Pig' or 'Zombie'. For player entities it is the nickkname of the
   * character, like 'mickkay' or 'bytemage'.
   */
  @LuaProperty
  public String getName() {
    return delegate.getName();
  }

  @LuaProperty
  public void setName(String name) {
    delegate.setCustomNameTag(name);
  }

  /**
   * The 'nbt' value (short for Named Binary Tag) is a table of entity-specifc key-value pairs also
   * called [data tags](https://minecraft.gamepedia.com/Commands#Data_tags). The nbt property is
   * readonly but gives you a modifiable copy of the internal value. You can change the contents,
   * but to activate them you have to assign the modified table to the entity by using the
   * [putNbt()](/modules/Entity/#putNbt) function.
   */
  @LuaProperty
  public NBTTagCompound getNbt() {
    NBTTagCompound nbt = new NBTTagCompound();
    delegate.writeToNBT(nbt);
    return nbt;
  }

  /**
   * The 'pos' is short for 'position'. It is a 3-dimensional vector containing the location of the
   * entity inside the world it is living in.
   */
  @LuaProperty(type = "Vec3")
  public Vec3d getPos() {
    return delegate.getPositionVector();
  }

  @LuaProperty(type = "Vec3")
  public void setPos(Vec3d pos) {
    delegate.setPositionAndUpdate(pos.xCoord, pos.yCoord, pos.zCoord);
  }

  /**
   * The 'rotationPitch' is the rotation of this entity's head around its X axis in degrees. A value
   * of -90 means the entity is looking straight up. A value of 90 means it is looking straight
   * down.
   */
  @LuaProperty
  public float getRotationPitch() {
    return delegate.rotationPitch;
  }

  @LuaProperty
  public void setRotationPitch(float rotationPitch) {
    delegate.setPositionAndRotation(delegate.posX, delegate.posY, delegate.posZ,
        delegate.rotationYaw, rotationPitch);
  }

  /**
   * The 'rotationYaw' is the rotation of this entity around its Y axis in degrees. For example, a
   * value of 0 means the entity is facing south. 90 corresponds to west, and 45 to south-west.
   */
  @LuaProperty
  public float getRotationYaw() {
    return MathHelper.wrapDegrees(delegate.rotationYaw);
  }

  @LuaProperty
  public void setRotationYaw(float rotationYaw) {
    delegate.setRotationYawHead(rotationYaw);
    delegate.setRenderYawOffset(rotationYaw);
    delegate.setPositionAndRotation(delegate.posX, delegate.posY, delegate.posZ, rotationYaw,
        delegate.rotationPitch);
  }

  protected void setRotationYawAndPitch(float yaw, float pitch) {
    delegate.setRotationYawHead(yaw);
    delegate.setRenderYawOffset(yaw);
    delegate.setPositionAndRotation(delegate.posX, delegate.posY, delegate.posZ, yaw, pitch);
  }

  /**
   * The 'tags' value is a list of strings that have been assigned to this entity.
   */
  @LuaProperty(type = "table")
  public Collection<String> getTags() {
    return delegate.getTags();
  }

  @LuaProperty(type = "table")
  public void setTags(Object luaObj) {
    Collection<String> tags = getConverters().toJavaCollection(String.class, luaObj, "tags");

    for (String oldTag : Lists.newArrayList(delegate.getTags())) {
      delegate.removeTag(oldTag);
    }
    for (String newTag : tags) {
      delegate.addTag(newTag);
    }
  }

  /**
   * The 'uuid' is a string of 36 characters forming an immutable universally unique identifier that
   * identifies this entity inside the world. This means if entities have the same ID they are
   * actually the same object.
   */
  @LuaProperty
  public String getUuid() {
    return delegate.getUniqueID().toString();
  }

  /**
   * The 'addTag' function adds the given tag to the set of [tags](!SITE_URL!/modules/Entity/#tags)
   * of this entity. This function returns true if the tag was added successfully.
   */
  @LuaFunction
  public boolean addTag(String tag) {
    return delegate.addTag(tag);
  }

  /**
   * The 'dropItem' function drops the given item at this entity's position modified by the
   * optionally given vertical offset.
   */
  @LuaFunction
  public EntityItem dropItem(ItemStack item, float offsetY) {
    if (item.getCount() == 0) {
      throw new IllegalArgumentException("Can't drop an item with count==0");
    }
    return delegate.entityDropItem(item, offsetY);
  }

  /**
   * The 'kill' function kills this entity during the next game tick.
   */
  @LuaFunction
  public void kill() {
    delegate.onKillCommand();
  }

  /**
   * The 'move' function teleports this entity instantly to the position relative to its current
   * position specified by the given direction and distance. If no distance is specified, 1 meter is
   * taken as default distance. Valid direction values are absolute directions ('up', 'down',
   * 'north', 'east', 'south', and 'west'), as well as relative directions ('forward', 'back',
   * 'left', and 'right'). Relative directions are interpreted relative to the direction the entity
   * is [facing](!SITE_URL!/modules/Entity/#facing).
   */
  @LuaFunction
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

  /**
   * The 'putNbt' function inserts the given table entries into the
   * [nbt](!SITE_URL!/modules/Entity/#nbt) property of this entity. Please note that this function
   * is not supported for [Player](!SITE_URL!/modules/Player/) objects.
   */
  @LuaFunction
  public void putNbt(Table nbt) {
    NBTTagCompound oldNbt = delegate.serializeNBT();
    NBTTagCompound newNbt = getConverters().getNbtConverter().merge(oldNbt, nbt);
    delegate.readFromNBT(newNbt);
  }

  /**
   * The 'removeTag' function removes the given tag from the set of
   * [tags](!SITE_URL!/modules/Entity/#tags) of this entity. This function returns true if the tag
   * has been removed successfully, and false if there was no such tag.
   */
  @LuaFunction
  public boolean removeTag(String tag) {
    return delegate.removeTag(tag);
  }

  /**
   * The 'scanView' function scans the view of this entity for the next (non-liquid) block. On
   * success it returns a [BlockHit](!SITE_URL!/modules/BlockHit/), otherwise nil. It scans the view
   * with a line-of-sight-range of up to the given distance (meter).
   */
  @LuaFunction
  public RayTraceResult scanView(float distance) {
    Vec3d start = delegate.getPositionEyes(0);
    Vec3d end = start.add(delegate.getLookVec().scale(distance));
    return delegate.getEntityWorld().rayTraceBlocks(start, end, false);
  }
}