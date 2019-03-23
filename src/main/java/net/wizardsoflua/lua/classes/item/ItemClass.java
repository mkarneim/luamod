package net.wizardsoflua.lua.classes.item;

import javax.inject.Inject;

import com.google.auto.service.AutoService;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.sandius.rembulan.Table;
import net.wizardsoflua.annotation.GenerateLuaClassTable;
import net.wizardsoflua.annotation.GenerateLuaDoc;
import net.wizardsoflua.annotation.GenerateLuaInstanceTable;
import net.wizardsoflua.annotation.LuaFunction;
import net.wizardsoflua.annotation.LuaProperty;
import net.wizardsoflua.extension.api.inject.Resource;
import net.wizardsoflua.extension.spell.api.resource.Injector;
import net.wizardsoflua.extension.spell.api.resource.LuaConverters;
import net.wizardsoflua.extension.spell.api.resource.LuaScheduler;
import net.wizardsoflua.extension.spell.spi.LuaConverter;
import net.wizardsoflua.lua.classes.BasicLuaClass;
import net.wizardsoflua.lua.classes.LuaClassAttributes;
import net.wizardsoflua.lua.classes.LuaInstance;
import net.wizardsoflua.lua.classes.common.Delegator;
import net.wizardsoflua.lua.nbt.NbtConverter;

@AutoService(LuaConverter.class)
@LuaClassAttributes(name = ItemClass.NAME)
@GenerateLuaClassTable(instance = ItemClass.Instance.class)
@GenerateLuaDoc(subtitle = "Things You can Carry Around")
public final class ItemClass extends BasicLuaClass<ItemStack, ItemClass.Instance> {
  public static final String NAME = "Item";
  @Resource
  private LuaConverters converters;
  @Resource
  private Injector injector;
  @Resource
  private LuaScheduler scheduler;

  @Override
  protected Table createRawTable() {
    return new ItemClassTable<>(this, converters);
  }

  @Override
  protected Delegator<Instance> toLuaInstance(ItemStack javaInstance) {
    return new ItemClassInstanceTable<>(new Instance(javaInstance, injector), getTable(),
        converters);
  }

  @GenerateLuaInstanceTable
  public static class Instance extends LuaInstance<ItemStack> {
    @Inject
    private NbtConverter nbtConverter;

    public Instance(ItemStack delegate, Injector injector) {
      super(delegate);
      injector.injectMembers(this);
    }

    @LuaProperty
    public int getCount() {
      return delegate.getCount();
    }

    @LuaProperty
    public void setCount(int count) {
      delegate.setCount(count);
    }

    @LuaProperty
    public int getDamage() {
      return delegate.getDamage();
    }

    @LuaProperty
    public void setDamage(int meta) {
      delegate.setDamage(meta);
    }

    @LuaProperty
    public String getDisplayName() {
      return delegate.getDisplayName().getUnformattedComponentText();
    }

    @LuaProperty
    public void setDisplayName(String displayName) {
      delegate.setDisplayName(new TextComponentString(displayName));
    }

    @LuaProperty
    public String getId() {
      ResourceLocation name = delegate.getItem().getRegistryName();
      if ("minecraft".equals(name.getNamespace())) {
        return name.getPath();
      } else {
        return name.toString();
      }
    }

    @LuaProperty
    public NBTTagCompound getNbt() {
      return delegate.serializeNBT();
    }

    @LuaProperty
    public int getRepairCost() {
      return delegate.getRepairCost();
    }

    @LuaProperty
    public void setRepairCost(int repairCost) {
      delegate.setRepairCost(repairCost);
    }

    @LuaFunction
    public void putNbt(Table nbt) {
      NBTTagCompound oldNbt = delegate.serializeNBT();
      NBTTagCompound newNbt = nbtConverter.merge(oldNbt, nbt);
      delegate.deserializeNBT(newNbt);
    }
  }
}
