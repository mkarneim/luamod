package net.wizardsoflua.lua.classes.event;

import com.google.auto.service.AutoService;

import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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

/**
 * The <span class="notranslate">LeftClickBlockEvent</span> class is fired when a player left-clicks
 * at some block.
 */
@AutoService(LuaConverter.class)
@LuaClassAttributes(name = LeftClickBlockEventClass.NAME,
    superClass = PlayerInteractEventClass.class)
@GenerateLuaClassTable(instance = LeftClickBlockEventClass.Instance.class)
@GenerateLuaDoc(type = EventClass.TYPE, subtitle = "When a Player Left-Clicks on a Block")
public final class LeftClickBlockEventClass extends
    BasicLuaClass<PlayerInteractEvent.LeftClickBlock, LeftClickBlockEventClass.Instance<PlayerInteractEvent.LeftClickBlock>> {
  public static final String NAME = "LeftClickBlockEvent";
  @Resource
  private LuaConverters converters;
  @Resource
  private Injector injector;

  @Override
  protected Table createRawTable() {
    return new LeftClickBlockEventClassTable<>(this, converters);
  }

  @Override
  protected Delegator<Instance<PlayerInteractEvent.LeftClickBlock>> toLuaInstance(
      PlayerInteractEvent.LeftClickBlock javaInstance) {
    return new LeftClickBlockEventClassInstanceTable<>(
        new Instance<>(javaInstance, getName(), injector), getTable(), converters);
  }

  @GenerateLuaInstanceTable
  public static class Instance<D extends PlayerInteractEvent.LeftClickBlock>
      extends PlayerInteractEventClass.Instance<D> {
    public Instance(D delegate, String name, Injector injector) {
      super(delegate, name, injector);
    }

    /**
     * The exact position the player clicked at.
     *
     * #### Example
     *
     * Creating some particle effect at the left-click hit position.
     *
     * <code>
     * local queue = Events.collect("LeftClickBlockEvent")
     * while true do
     *   local event = queue:next()
     *   local v = event.hitVec
     *   spell:execute([[
     *     /particle angryVillager %s %s %s 0 0 0 0 1 true
     *   ]], v.x, v.y, v.z)
     * end
     * </code>
     *
     */
    @LuaProperty
    public Vec3d getHitVec() {
      return delegate.getHitVec();
    }
  }
}
