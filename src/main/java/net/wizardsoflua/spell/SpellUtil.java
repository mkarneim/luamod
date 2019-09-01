package net.wizardsoflua.spell;

import static net.minecraftforge.common.ForgeHooks.rayTraceEyes;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.particles.IParticleData;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

public class SpellUtil {
  private static final double LOOK_DISTANCE = 5;

  public static Vec3d getPositionLookingAt(EntityLivingBase entity) {
    RayTraceResult git = rayTraceEyes(entity, LOOK_DISTANCE);
    if (git == null) {
      return getPositionAtLookDistance(entity);
    }
    BlockPos blockPos = git.getBlockPos();
    BlockPos blockPos2 = new BlockPos(git.hitVec);
    if (blockPos.equals(blockPos2)) {
      return git.hitVec;
    }
    // force the hit vector to be inside the hit block's real bounding box
    Vec3d newHit = git.hitVec.add(entity.getLookVec().scale(0.01));
    return newHit;
  }

  public static @Nullable EnumFacing getSideLookingAt(EntityLivingBase entity) {
    RayTraceResult git = rayTraceEyes(entity, LOOK_DISTANCE);
    return git == null ? null : git.sideHit;
  }

  public static Vec3d getPositionAtLookDistance(Entity entity) {
    Vec3d startPos = new Vec3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
    Vec3d endPos = startPos.add(new Vec3d(entity.getLookVec().x * LOOK_DISTANCE,
        entity.getLookVec().y * LOOK_DISTANCE, entity.getLookVec().z * LOOK_DISTANCE));
    return endPos;
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
    if (Math.abs(MathHelper.wrapDegrees(rotation) - MathHelper.wrapDegrees(yaw)) <= precision) {
      return result;
    }
    return null;
  }

  /**
   * Spawns particles at the given location.
   *
   * @param world
   * @param particleData
   * @param x
   * @param y
   * @param z
   * @param offsetX
   * @param offsetY
   * @param offsetZ
   * @param particleSpeed
   * @param numberOfParticles
   * @param force
   * @param viewers
   *
   * @see net.minecraft.command.impl.ParticleCommand.spawnParticle
   */
  public static void spawnParticle(WorldServer world, IParticleData particleData, double x,
      double y, double z, double offsetX, double offsetY, double offsetZ, float particleSpeed,
      int numberOfParticles, boolean force, Collection<EntityPlayerMP> viewers) {

    for (EntityPlayerMP entityplayermp : viewers) {
      if (world.spawnParticle(entityplayermp, particleData, force, x, y, z, numberOfParticles,
          offsetX, offsetY, offsetZ, (double) particleSpeed)) {
      }
    }
  }

}
