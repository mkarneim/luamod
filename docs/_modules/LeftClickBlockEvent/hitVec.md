#### Example
Creating some particle effect at the left-click hit position.
```lua
local queue=Events.connect("LeftClickBlockEvent")
while true do
  local event = queue:pop()
  local v = event.hitVec
  spell:execute([[
    /particle angryVillager %s %s %s 0 0 0 0 1 true
  ]], v.x, v.y, v.z)
end
```
