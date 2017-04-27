#### Example
Registering for chat events and echoing the received messages:
```lua
local queue = Events.register( "ChatEvent")
for e in queue.next do
  spell:say( e.message)
end
```
#### Example
Registering for left and right click events and telling
the world what happend:
```lua
local queue = Events.register( "LeftClickBlockEvent", "RightClickBlockEvent")
for e in queue.next do
  spell:say( e.type.." at "..e.pos);
end
```
#### Example
Registering for some custom event type and telling
the world what happend:
```lua
local queue = Events.register( "my event type name")
for e in queue.next do
  spell:say( inspect( e) )
end
```
