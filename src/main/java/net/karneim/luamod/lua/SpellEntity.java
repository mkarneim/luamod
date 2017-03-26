package net.karneim.luamod.lua;

import javax.annotation.Nullable;

import net.karneim.luamod.LuaMod;
import net.karneim.luamod.credentials.Credentials;
import net.karneim.luamod.credentials.Realm;
import net.karneim.luamod.cursor.Clipboard;
import net.karneim.luamod.cursor.Snapshots;
import net.karneim.luamod.cursor.Spell;
import net.karneim.luamod.lua.event.Events;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.sandius.rembulan.exec.CallPausedException;
import net.sandius.rembulan.exec.Continuation;
import net.sandius.rembulan.load.LoaderException;

public class SpellEntity extends Entity {

  private LuaMod mod;
  private ICommandSender owner;
  private Clipboard clipboard;
  private Spell spell;

  private enum State {
    START, RESUME, SUSPEND, PAUSE, STOP, DEAD
  }

  private State state = State.START;
  private String command;
  private Continuation continuation;
  private LuaUtil luaUtil;
  private Ticket chunkLoaderTicket;
  private ChunkPos chunkPos;
  private String profile;

  public SpellEntity(World worldIn) {
    super(worldIn);
  }

  public SpellEntity(World worldIn, LuaMod mod, ICommandSender aOwner, Clipboard clipboard,
      Vec3d pos, Rotation rotation, EnumFacing surface) {
    this(worldIn);
    this.mod = mod;
    this.owner = aOwner;
    this.clipboard = clipboard;
    Entity entity = owner.getCommandSenderEntity();
    String userId;
    if (entity == null || entity.getUniqueID() == null) {
      userId = null;
    } else {
      userId = owner.getCommandSenderEntity().getUniqueID().toString();
    }
    Snapshots snapshots = new Snapshots();
    this.spell = new Spell(owner, this, this.getEntityWorld(), pos, rotation, surface, snapshots);
    Credentials credentials = mod.getCredentialsStore().retrieveCredentials(Realm.GitHub, userId);
    luaUtil = new LuaUtil(this.getEntityWorld(), owner, spell, clipboard, credentials, snapshots);
    if (surface != null) {
      pos = pos.add(new Vec3d(surface.getDirectionVec()));
    }
    updatePosition();
    mod.getSpellRegistry().register(this);
  }

  public ICommandSender getOwner() {
    return owner;
  }

  public Clipboard getClipboard() {
    return clipboard;
  }

  public void setProfile(String profile) {
    this.profile = profile;
  }

  public @Nullable String getCommand() {
    return command;
  }

  public void setCommand(String command) throws LoaderException {
    this.command = command;
    if (profile != null) {
      luaUtil.setProfile(profile);
    }
    luaUtil.compile(command);
    onUpdate();
  }

  @Override
  protected void entityInit() {
    requestChunkLoaderTicket();
    chunkLoaderTicket.bindEntity(this);
    chunkPos = new ChunkPos(getPosition());
    ForgeChunkManager.forceChunk(chunkLoaderTicket, chunkPos);
  }

  boolean isInside(ChunkPos cPos, Vec3d pos) {
    return cPos.getXStart() <= pos.xCoord && pos.xCoord <= cPos.getXEnd()
        && cPos.getZStart() <= pos.zCoord && pos.zCoord <= cPos.getZEnd();
  }

  @Override
  protected void readEntityFromNBT(NBTTagCompound compound) {
    // TODO Auto-generated method stub

  }

  @Override
  protected void writeEntityToNBT(NBTTagCompound compound) {
    // TODO Auto-generated method stub

  }

  // @Override
  // public void addChatMessage(ITextComponent component) {
  // // whispers to you
  // String unformattedComponentText = component.getUnformattedComponentText();
  // System.out.println("unformattedComponentText "+unformattedComponentText);
  // // Player695 whispers to you: hi
  // String unformattedText = component.getUnformattedText();
  // System.out.println("unformattedText "+unformattedText);
  //
  // luaUtil.handleEvent(new Event(EventType.MESSAGE_EVENT, unformattedText));
  // }

  private void requestChunkLoaderTicket() {
    chunkLoaderTicket = ForgeChunkManager.requestTicket(LuaMod.instance, getEntityWorld(),
        ForgeChunkManager.Type.ENTITY);
  }

  private void releaseChunkLoaderTicket() {
    if (chunkLoaderTicket != null) {
      try {
        ForgeChunkManager.releaseTicket(chunkLoaderTicket);
      } catch (Throwable e) {
        // ignored
      }
      chunkLoaderTicket = null;
    }
  }

  private void updatePosition() {
    Vec3d pos = spell.getWorldPosition();
    setPosition(pos.xCoord, pos.yCoord, pos.zCoord);
    if (chunkLoaderTicket != null) {
      if (!isInside(chunkPos, pos)) {
        ForgeChunkManager.unforceChunk(chunkLoaderTicket, chunkPos);
        chunkPos = new ChunkPos(new BlockPos(pos));
        ForgeChunkManager.forceChunk(chunkLoaderTicket, chunkPos);
      }
    }
  }

  public void onUpdate() {
    super.onUpdate();
    if (luaUtil == null) {
      setDead();
      return;
    }
    if (!getEntityWorld().isRemote) {
      luaUtil.setCurrentTime(ticksExisted);
      switch (state) {
        case START:
          try {
            luaUtil.run();
            state = State.STOP;
          } catch (CallPausedException e) {
            continuation = e.getContinuation();
            if (canResume()) {
              state = State.RESUME;
            }
          } catch (Exception e) {
            e.printStackTrace();
            String message = String.format("Error during command execution: %s!", e.getMessage());
            TextComponentString txt = new TextComponentString(message);
            txt.setStyle((new Style()).setColor(TextFormatting.RED).setBold(Boolean.valueOf(true)));
            owner.addChatMessage(txt);
            state = State.STOP;
          }
          break;
        case RESUME:
          try {
            if (!luaUtil.isWaiting()) {
              luaUtil.resume(continuation);
              state = State.STOP;
            }
          } catch (CallPausedException e) {
            continuation = e.getContinuation();
            if (canResume()) {
              state = State.RESUME;
            }
          } catch (Exception e) {
            e.printStackTrace();
            String message = String.format("Error during command execution: %s!", e.getMessage());
            TextComponentString txt = new TextComponentString(message);
            txt.setStyle((new Style()).setColor(TextFormatting.RED).setBold(Boolean.valueOf(true)));
            owner.addChatMessage(txt);
            state = State.STOP;
          }
          break;
        case STOP:
          setDead();
          break;
        case DEAD:
          System.err.println("onUpdate() called on dead entity!");
        case SUSPEND:
        default:
          break;
      }
    }
    updatePosition();
  }


  public Spell getCursor() {
    return spell;
  }

  @Override
  public void setDead() {
    state = State.DEAD;
    System.out.println("Terminating " + getName() + "!");
    super.setDead();
    releaseChunkLoaderTicket();
    LuaMod.instance.getSpellRegistry().unregister(this);
  }

  public void pause() {
    if (canPause()) {
      state = State.PAUSE;
    }
  }

  public void unpause() {
    if (canUnpause()) {
      state = State.RESUME;
    }
  }

  private boolean canResume() {
    switch (state) {
      case START:
      case RESUME:
        return true;
      default:
        return false;
    }
  }

  private boolean canPause() {
    switch (state) {
      case START:
      case RESUME:
        return true;
      default:
        return false;
    }
  }

  private boolean canUnpause() {
    switch (state) {
      case PAUSE:
        return true;
      default:
        return false;
    }
  }

  public Events getEvents() {
    return luaUtil.getEvents();
  }

}
