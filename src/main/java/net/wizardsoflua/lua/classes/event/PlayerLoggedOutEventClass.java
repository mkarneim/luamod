package net.wizardsoflua.lua.classes.event;

import com.google.auto.service.AutoService;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.sandius.rembulan.Table;
import net.wizardsoflua.annotation.GenerateLuaClassTable;
import net.wizardsoflua.annotation.GenerateLuaDoc;
import net.wizardsoflua.annotation.GenerateLuaInstanceTable;
import net.wizardsoflua.annotation.LuaProperty;
import net.wizardsoflua.extension.api.inject.Resource;
import net.wizardsoflua.extension.spell.api.resource.Injector;
import net.wizardsoflua.extension.spell.api.resource.LuaConverters;
import net.wizardsoflua.extension.spell.spi.LuaConverter;
import net.wizardsoflua.lua.classes.BasicLuaClass;
import net.wizardsoflua.lua.classes.LuaClassAttributes;
import net.wizardsoflua.lua.classes.common.Delegator;

@AutoService(LuaConverter.class)
@LuaClassAttributes(name = PlayerLoggedOutEventClass.NAME, superClass = EventClass.class)
@GenerateLuaClassTable(instance = PlayerLoggedOutEventClass.Instance.class)
@GenerateLuaDoc(type = EventClass.TYPE)
public final class PlayerLoggedOutEventClass extends BasicLuaClass<PlayerEvent.PlayerLoggedOutEvent, PlayerLoggedOutEventClass.Instance<PlayerEvent.PlayerLoggedOutEvent>> {
  public static final String NAME = "PlayerLoggedOutEvent";
  @Resource
  private LuaConverters converters;
  @Resource
  private Injector injector;

  @Override
  protected Table createRawTable() {
    return new PlayerLoggedOutEventClassTable<>(this, converters);
  }

  @Override
  protected Delegator<Instance<PlayerEvent.PlayerLoggedOutEvent>> toLuaInstance(
      PlayerEvent.PlayerLoggedOutEvent javaInstance) {
    return new PlayerLoggedOutEventClassInstanceTable<>(
        new Instance<>(javaInstance, getName(), injector), getTable(), converters);
  }

  @GenerateLuaInstanceTable
  public static class Instance<D extends PlayerEvent.PlayerLoggedOutEvent>
      extends EventClass.Instance<D> {
    public Instance(D delegate, String name, Injector injector) {
      super(delegate, name, injector);
    }

    @LuaProperty
    public EntityPlayer getPlayer() {
      return delegate.player;
    }
  }
}
