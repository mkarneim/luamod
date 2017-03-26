package net.karneim.luamod.cursor;

import static net.minecraftforge.common.ForgeHooks.rayTraceEyes;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public class SpellUtil {
  private static final double LOOK_DISTANCE = 5;

  public static BlockPos getPositionLookingAt(Entity entity) {
    RayTraceResult git = rayTraceEyes((EntityLivingBase) entity, LOOK_DISTANCE);
    return git == null ? getPositionAtLookDistance(entity) : git.getBlockPos();
  }

  public static @Nullable EnumFacing getSideLookingAt(Entity entity) {
    RayTraceResult git = rayTraceEyes((EntityLivingBase) entity, LOOK_DISTANCE);
    return git == null ? null : git.sideHit;
  }

  public static BlockPos getPositionAtLookDistance(Entity entity) {
    Vec3d startPos = new Vec3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
    Vec3d endPos = startPos.add(new Vec3d(entity.getLookVec().xCoord * LOOK_DISTANCE,
        entity.getLookVec().yCoord * LOOK_DISTANCE, entity.getLookVec().zCoord * LOOK_DISTANCE));
    return new BlockPos(endPos);
  }

  public static Rotation getRotation(EnumFacing facing) {
    switch (facing) {
      case NORTH:
        return Rotation.CLOCKWISE_180;
      case EAST:
        return Rotation.COUNTERCLOCKWISE_90;
      case SOUTH:
        return Rotation.NONE;
      case WEST:
        return Rotation.CLOCKWISE_90;
      default:
        return null;
    }
  }

  public static EnumFacing getFacing(Rotation rotation) {
    switch (rotation) {
      case NONE:
        return EnumFacing.SOUTH;
      case CLOCKWISE_90:
        return EnumFacing.WEST;
      case CLOCKWISE_180:
        return EnumFacing.NORTH;
      case COUNTERCLOCKWISE_90:
        return EnumFacing.EAST;
      default:
        return null;
    }
  }

  public static float getRotationYaw(Rotation rot) {
    return getRotationYaw(getFacing(rot));
  }

  public static float getRotationYaw(EnumFacing facing) {
    switch (facing) {
      case SOUTH:
        return 0;
      case WEST:
        return 90;
      case NORTH:
        return -180;
      case EAST:
        return -90;
      default:
        throw new Error("Unexpected facing " + facing);
    }
  }

  public static EnumFacing getFacing(float rotation) {
    rotation = MathHelper.wrapDegrees(rotation);
    if (rotation < -135) {
      return EnumFacing.NORTH;
    }
    if (rotation < -45) {
      return EnumFacing.EAST;
    }
    if (rotation < 45) {
      return EnumFacing.SOUTH;
    }
    if (rotation < 135) {
      return EnumFacing.WEST;
    }
    return EnumFacing.NORTH;
  }

  public static Rotation roundRotation(float rotation) {
    EnumFacing facing = getFacing(rotation);
    return getRotation(facing);
  }

  public static @Nullable Rotation roundRotation(float rotation, double precision) {
    Rotation result = roundRotation(rotation);
    float yaw = getRotationYaw(result);
    if ( Math.abs(MathHelper.wrapDegrees(rotation)-MathHelper.wrapDegrees(yaw)) <= precision) {
      return result;
    }
    return null;
  }

}
