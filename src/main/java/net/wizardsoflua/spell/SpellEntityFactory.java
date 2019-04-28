package net.wizardsoflua.spell;

import javax.annotation.Nullable;
import javax.inject.Inject;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.wizardsoflua.ServerScoped;
import net.wizardsoflua.chunk.ChunkForceManager;
import net.wizardsoflua.lua.SpellProgram;
import net.wizardsoflua.lua.SpellProgramFactory;
import net.wizardsoflua.lua.module.print.PrintRedirector.PrintReceiver;

/**
 * Factory for creating {@link SpellEntity} objects.
 */
@ServerScoped
public class SpellEntityFactory {
  @Inject
  private SpellRegistry spellRegistry;
  @Inject
  private SpellProgramFactory programFactory;
  @Inject
  private ChunkForceManager chunkForceManager;

  private long nextSid = 1;

  public SpellEntity create(CommandSource source, PrintReceiver printReceiver, String code,
      @Nullable String... arguments) {
    Entity owner = getOwner(source);
    World world = source.getWorld();
    PositionAndRotation pos = getPositionAndRotation(source);
    return create(owner, printReceiver, world, pos, code, arguments);
  }

  public SpellEntity create(@Nullable Entity owner, PrintReceiver printReceiver, World world,
      PositionAndRotation pos, String code, String... arguments) {
    SpellProgram program = programFactory.create(world, owner, printReceiver, code, arguments);
    nextSid++;
    SpellEntity result = new SpellEntity(world, program, pos, nextSid, chunkForceManager);
    program.setSpellEntity(result);
    spellRegistry.add(result);
    return result;
  }

  private Entity getOwner(CommandSource source) {
    if (source instanceof WolCommandSource) {
      WolCommandSource wolSource = (WolCommandSource) source;
      ICommandSource source2 = wolSource.getSource();
      if (source2 instanceof SpellEntity) {
        SpellEntity spell = (SpellEntity) source2;
        return spell.getOwner();
      }
    }
    return source.getEntity();
  }

  private PositionAndRotation getPositionAndRotation(CommandSource source) {
    Entity entity = source.getEntity();
    Vec3d pos = source.getPos();
    Vec2f pitchYaw = source.getPitchYaw();
    if (entity instanceof EntityPlayer) {
      EntityPlayer player = (EntityPlayer) entity;
      if (pos.equals(player.getPositionVector()) && pitchYaw.equals(player.getPitchYaw())) {
        pos = SpellUtil.getPositionLookingAt(player);
        float rotationYaw = SpellUtil.getRotationYaw(player.getHorizontalFacing());
        float rotationPitch = 0;
        return new PositionAndRotation(pos, rotationYaw, rotationPitch);
      }
    }
    return new PositionAndRotation(pos, pitchYaw);
  }

}
