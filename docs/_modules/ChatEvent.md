---
name: ChatEvent
title: ChatEvent
subtitle: Knowing what Players are Talking About
type: event
extends: Event
layout: module
properties:
  - name: message
    type: 'string'
    access: r
    description: |
        The chat message that has been posted.
  - name: player
    type: '[Player](/modules/Player)'
    access: r
    description: |
        The player that sent the chat message.
functions:
---

The <span class="notranslate">ChatEvent</span> class contains information about a chat message
that has been posted by some [player](/modules/Player/).

### Example

Echoing all chat messages.

```lua
local queue=Events.collect("ChatEvent")
while true do
  local event=queue:next()
  spell:execute("say %s", event.message)
end
```
