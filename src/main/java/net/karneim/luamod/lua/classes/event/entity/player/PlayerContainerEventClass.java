// TODO Adrodoc55 07.05.2017: Implement Event Class
//package net.karneim.luamod.lua.classes.event.entity.player;
//
//import net.karneim.luamod.lua.classes.LuaModule;
//import net.karneim.luamod.lua.classes.LuaTypesRepo;
//import net.karneim.luamod.lua.util.table.DelegatingTable.Builder;
//import net.karneim.luamod.lua.util.wrapper.DelegatingLuaClass;
//import net.minecraftforge.event.entity.player.PlayerContainerEvent;
//import net.sandius.rembulan.Table;
//
//@LuaModule("PlayerContainerEvent")
//public class PlayerContainerEventClass extends DelegatingLuaClass<PlayerContainerEvent> {
//  public PlayerContainerEventClass(LuaTypesRepo repo) {
//    super(repo);
//  }
//
//  @Override
//  protected void addProperties(Builder<? extends PlayerContainerEvent> b, PlayerContainerEvent d) {
//    b.addReadOnly("container", () -> repo.wrap(d.getContainer()));
//  }
//
//  @Override
//  protected void addFunctions(Table luaClass) {}
//}
