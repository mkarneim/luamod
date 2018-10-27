---
name: Time
subtitle: Accessing the Time
type: module
layout: module
properties:
  - name: autosleep
    type: boolean
    access: r/w
    description: "The autosleep value defines whether the current spell
    should go to sleep automatically when its allowance is exceeded.
    If this is set to false, the spell will never go to sleep automatically,
    but instead will be broken when its allowance reaches zero.
    Default is true normally, but in an [event interceptor](/modules/Events#intercept) 'autosleep' is always false and can't be changed.
    "
  - name: allowance
    type: number
    access: r
    description: "The allowance is the number of lua ticks that are left before
    the spell or event listener is broken or sent to sleep, depending on [autosleep](#autosleep).
    "
  - name: realtime
    type: number
    access: r
    description: "The realtime is the number of milliseconds that have passed since
    January 1st, 1970.
    "
  - name: gametime
    type: number
    access: r
    description: "The gametime is the number of game ticks that have passed since the
    world has been created.
    "
  - name: luatime
    type: number
    access: r
    description: "The luatime is the number of lua ticks that the current spell
    has worked since it has been casted. This includes lua ticks of event listeners.
    "
functions:
  - name: getDate
    parameters: string
    results: string
    description: "Returns a string with the current real date and time.
    If you want you can change the format by providing an optional format string."
    examples:
      - url: Time/getDate.md
  - name: sleep
    parameters: number
    results: nil
    description: |
      Forces the current spell to sleep for the given number of game ticks.
      If the number is 0, the spell won't sleep.
      If the number is negative, this function will issue an error.
      If the number is nil, the spell might go to sleep or not.
      This depends on the number of lua ticks that are already consumed by this spell.
      The rule is as follows: the spell will be sent to sleep if the spell's allowance falls below the half value
      of the spell's initial allowance.
    examples:
      - url: Time/sleep.md
---

The <span class="notranslate">Time</span> module provides access to time related properties of the active Spell's world.
