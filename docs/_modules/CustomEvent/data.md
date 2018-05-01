#### Example
Firing a custom event with some complex data.
```lua
local data = {pos=spell.pos, time=Time.gametime}
Events.fire("my-event", data)
```

#### Example
Accessing the data of a custom event.
```lua
local q = Events.collect("my-event")
local event = q:next()
print("event.data", str(event.data))
```
