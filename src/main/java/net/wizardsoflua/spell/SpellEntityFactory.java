package net.wizardsoflua.spell;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;

import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.wizardsoflua.lua.SpellProgram;
import net.wizardsoflua.lua.SpellProgramFactory;
import net.wizardsoflua.lua.module.print.PrintRedirector.PrintReceiver;

/**
 * Factory for creating {@link SpellEntity} objects.
 */
public class SpellEntityFactory {
  private final SpellRegistry spellRegistry;
  private final SpellProgramFactory programFactory;

  private long nextSid = 1;

  public SpellEntityFactory(SpellRegistry spellRegistry, SpellProgramFactory programFactory) {
    this.spellRegistry = checkNotNull(spellRegistry, "spellRegistry==null!");
    this.programFactory = checkNotNull(programFactory, "programFactory==null!");
  }

  public SpellEntity create(World world, CommandSource source, PrintReceiver spellLogger,
      String code, @Nullable String[] arguments) {
    checkNotNull(world, "world==null!");
    Entity owner = getOwner(source);
    SpellProgram program =
        programFactory.create(world, source, owner, spellLogger, code, arguments);
    PositionAndRotation pos = getPositionAndRotation(source);
    nextSid++;
    SpellEntity result = new SpellEntity(world, owner, program, pos, nextSid);
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
        return spell.getOwnerEntity();
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
