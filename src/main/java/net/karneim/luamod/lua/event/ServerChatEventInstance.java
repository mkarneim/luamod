package net.karneim.luamod.lua.event;

import javax.annotation.Nullable;

import net.karneim.luamod.lua.classes.LuaTypesRepo;
import net.karneim.luamod.lua.util.table.DelegatingTable;
import net.minecraftforge.event.ServerChatEvent;
import net.sandius.rembulan.Table;

public class ServerChatEventInstance extends EventWrapper<ServerChatEvent> {
  public ServerChatEventInstance(LuaTypesRepo repo, @Nullable ServerChatEvent delegate,
      Table metatable) {
    super(repo, delegate, EventType.CHAT.name(), metatable);
  }

  @Override
  protected void addProperties(DelegatingTable.Builder builder) {
    super.addProperties(builder);
    builder.add("sender", delegate.getUsername());
    builder.add("message", delegate.getMessage());
  }
}
