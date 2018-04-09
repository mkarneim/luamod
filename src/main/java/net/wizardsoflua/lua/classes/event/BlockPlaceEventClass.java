package net.wizardsoflua.lua.classes.event;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.world.BlockEvent;
import net.wizardsoflua.block.ImmutableWolBlock;
import net.wizardsoflua.lua.classes.DeclareLuaClass;
import net.wizardsoflua.lua.classes.DelegatorLuaClass;

@DeclareLuaClass(name = BlockPlaceEventClass.NAME, superClass = BlockEventClass.class)
public class BlockPlaceEventClass extends
    DelegatorLuaClass<BlockEvent.PlaceEvent, BlockPlaceEventClass.Proxy<BlockEvent.PlaceEvent>> {
  public static final String NAME = "BlockPlaceEvent";

  @Override
  public Proxy<BlockEvent.PlaceEvent> toLua(BlockEvent.PlaceEvent javaObj) {
    return new Proxy<>(this, javaObj);
  }

  public static class Proxy<D extends BlockEvent.PlaceEvent> extends BlockEventClass.Proxy<D> {
    public Proxy(DelegatorLuaClass<?, ?> luaClass, D delegate) {
      super(luaClass, delegate);
      addReadOnly("hand", this::getHand);
      addReadOnly("placedAgainst", this::getPlacedAgainst);
      addReadOnly("player", this::getPlayer);
    }

    @Override
    protected Object getBlock() {
      IBlockState blockState = delegate.getState();
      NBTTagCompound nbt = delegate.getBlockSnapshot().getNbt();
      return getConverter().toLua(new ImmutableWolBlock(blockState, nbt));
    }

    protected Object getHand() {
      return getConverter().toLua(delegate.getHand());
    }

    protected Object getPlacedAgainst() {
      IBlockState blockState = delegate.getPlacedAgainst();
      NBTTagCompound nbt = null;
      return getConverter().toLua(new ImmutableWolBlock(blockState, nbt));
    }

    protected Object getPlayer() {
      return getConverter().toLua(delegate.getPlayer());
    }
  }
}
