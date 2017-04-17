#### Example
Register for CHAT events and echo the received messages:
```lua
local queue = events.register( "CHAT")
for e in queue.next do
  say( e.message)
end
```
#### Example
Register for LEFT_CLICK and RIGHT_CLICK events and tell
the world what happend:
```lua
local queue = events.register( "LEFT_CLICK", "RIGHT_CLICK")
for e in queue.next do
  say( e.type.." at "..e.pos);
end
```
#### Example
Register for some custom event type and tell
the world what happend:
```lua
local queue = events.register( "my event type name")
for e in queue.next do
  say( inspect( e) )
end
```
