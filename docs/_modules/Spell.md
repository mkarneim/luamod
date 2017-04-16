---
title: The Spell's Essence - How to Controll the Aspects of an Active Spell
name: Spell
properties:
  - name: block
    type: BlockState
    access: r/w
    description: "The 'block' denotes the *block's state* at the spell's position. Use
    it to find out about what material the block is contsructed of, or in which
    direction it is facing.
    "
    examples:
      - url: Spell/block.md
  - name: orientation
    type: string
    access: r/w
    description: "The 'orientation' denotes the direction the spell is oriented at.
    This is one of 'NORTH', 'EAST', 'SOUTH', or 'WEST'. It defines for example
    in which direction the spell will move when sent 'FORWARD'."
    examples:
      - url: Spell/orientation.md
  - name: origin
    type: Vec3
    access: r
    description: The spell's initial position where it was casted.
    examples:
      - url: Spell/origin.md
  - name: owner
    type: Entity
    access: r
    description: The entity that has casted this spell. Normally this is a Player, or nil if the spell has been casted by a command block.
    examples:
      - url: Spell/owner.md
  - name: pos
    type: Vec3
    access: r/w
    description: The spell's current position.
    examples:
      - url: Spell/pos.md
  - name: rotation
    type: number
    access: r/w
    description: The spell's rotation in degrees. It can have any value between -180 to 180, where 0 means "SOUTH".
    examples:
      - url: Spell/rotation.md
  - name: surface
    type: string
    access: r
    description: The orientation of the surface the spell has been casted on. This is one of "NORTH", "EAST", "SOUTH, "WEST", "UP", "DOWN", or nil if the spell has been casted mid-air.
    examples:
      - url: Spell/surface.md
functions:
  - name: move
    parameters: direction, distance
    results: nil
    description: "The 'move' function teleports the spell instantly to a
    location into the specified direction at a specified distance. If no
    distance is specified, 1 Meter is taken as default."
  - name: moveBy
    parameters: dx, dy, dz
    results: nil
    description: TODO
  - name: rotate
    parameters: angle
    results: nil
    description: TODO
  - name: say
    parameters: message, arguments...
    results: nil
    description: TODO
  - name: msg
    parameters: recipient, message
    results: nil
    description: TODO
  - name: execute
    parameters: command
    results: nil
    description: TODO  
  - name: reset
    parameters:
    results:
    description: TODO
  - name: resetRotation
    parameters:
    results: nil
    description: TODO
  - name: resetPosition
    parameters:
    results: nil
    description: TODO
  - name: pushLocation
    parameters:
    results: nil
    description: TODO
  - name: popLocation
    parameters:
    results: nil
    description: TODO
  - name: cut
    parameters: selection
    results: snapshot
    description: TODO  
  - name: copy
    parameters: selection
    results: snapshot
    description: TODO
  - name: paste
    parameters: snapshot
    results: selection
    description: TODO
---
{% include module-head.md %}

"Spell" is one of the main magic types used in most known spells. It is used to
control the properties and the behaviour of the executed spell itself.
For example, you specify the exact location in the world where the spell will
be acting apon.

{% include module-body.md %}
