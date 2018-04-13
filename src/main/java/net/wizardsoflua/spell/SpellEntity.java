package net.wizardsoflua.spell;

import static com.google.common.base.Preconditions.checkNotNull;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.impl.DefaultTable;
import net.wizardsoflua.WizardsOfLua;
import net.wizardsoflua.lua.SpellProgram;
import net.wizardsoflua.lua.classes.LuaClassLoader;

public class SpellEntity extends Entity {
  public static final String NAME = "Spell";

  private ICommandSender owner;
  private SpellProgram program;
  private long sid; // immutable spell id
  private ChunkLoaderTicketSupport chunkLoaderTicketSupport;
  private boolean visible = false;
  private Table data = new DefaultTable();

  public SpellEntity(World world) {
    // Used by MC when loading this entity from persistent data
    super(checkNotNull(world, "world==null!"));
  }

  public SpellEntity(World world, ICommandSender owner, SpellProgram program,
      PositionAndRotation posRot, long sid) {
    this(world);
    this.owner = checkNotNull(owner, "owner==null!");
    this.program = checkNotNull(program, "program==null!");
    this.sid = sid;
    setPositionAndRotation(posRot);
    String name = SpellEntity.NAME + "-" + sid;
    setCustomNameTag(name);
    chunkLoaderTicketSupport = new ChunkLoaderTicketSupport(WizardsOfLua.instance, this);
    chunkLoaderTicketSupport.request();
  }

  @Override
  public NBTTagCompound serializeNBT() {
    NBTTagCompound ret = new NBTTagCompound();
    //ret.setString("id", this.getEntityString());
    return this.writeToNBT(ret);
  }

  public PositionAndRotation getPositionAndRotation() {
    return new PositionAndRotation(getPositionVector(), rotationYaw, rotationPitch);
  }

  private void setPositionAndRotation(PositionAndRotation posRot) {
    Vec3d pos = posRot.getPos();
    float yaw = posRot.getRotationYaw();
    float pitch = posRot.getRotationPitch();
    setPositionAndRotation(pos.x, pos.y, pos.z, yaw, pitch);
  }

  public ICommandSender getOwner() {
    return owner;
  }

  public long getSid() {
    return sid;
  }

  public Entity getOwnerEntity() {
    return owner.getCommandSenderEntity();
  }

  public SpellProgram getProgram() {
    return program;
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  public Object getData(LuaClassLoader viewingClassLoader) {
    LuaClassLoader spellClassLoader = program.getLuaClassLoader();
    if (viewingClassLoader == spellClassLoader) {
      return data;
    }
    return viewingClassLoader.getTransferenceProxyFactory().getProxy(data, spellClassLoader);
  }

  @Override
  protected void readEntityFromNBT(NBTTagCompound compound) {}

  @Override
  protected void writeEntityToNBT(NBTTagCompound compound) {}

  @Override
  protected void entityInit() {}

  @Override
  public void setPosition(double x, double y, double z) {
    if (chunkLoaderTicketSupport != null) {
      chunkLoaderTicketSupport.updatePosition();
    }
    super.setPosition(x, y, z);
  }

  @Override
  public Vec3d getLookVec() {
    return this.getLook(1.0F);
  }

  @Override
  public void onUpdate() {
    super.onUpdate();
    if (program == null) {
      setDead();
      return;
    }
    program.resume();
    if (program.isTerminated()) {
      setDead();
    }

    if (visible) {
      SpellAuraFX.spawnParticle(this);
    }
    applyMotion();
  }

  private void applyMotion() {
    if (motionX != 0 || motionY != 0 || motionZ != 0) {
      setPositionAndUpdate(posX + motionX, posY + motionY, posZ + motionZ);
    }
  }

  @Override
  public void setDead() {
    if (program != null) {
      program.terminate();
    }
    if (chunkLoaderTicketSupport != null) {
      chunkLoaderTicketSupport.release();
    }
    super.setDead();
    MinecraftForge.EVENT_BUS.post(new SpellTerminatedEvent(this));
  }

  public void replacePlayerInstance(EntityPlayerMP player) {
    if (this.owner.getCommandSenderEntity() instanceof EntityPlayer) {
      if (this.owner.getCommandSenderEntity().getUniqueID().equals(player.getUniqueID())) {
        this.owner = player;
      }
    }
    getProgram().replacePlayerInstance(player);
  }

}
