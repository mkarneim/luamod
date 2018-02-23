---
name: Player
subtitle: Controlling the Player
type: class
extends: Entity
layout: module
properties:
  - name: team
    type: string
    access: r/w
    description: |
        The 'team' is the name of the team this player belongs to, or nil if he is not a member of any team.

        #### Example
        Adding the wizard to the 'rogues' team.
        ```lua
        spell.owner.team = "rogues"
        ```

        To make this work, don't forget to create the 'rogues' team first:
        ```
        /scoreboard teams add rogues
        ```

        #### Example
        Printing the wizard's team name.
        ```lua
        print( spell.owner.team)
        ```
  - name: mainhand
    type: "[Item](/modules/Item/)"
    access: r/w
    description: "This is the [item](!SITE_URL!/modules/Item/) this player is holding in his main hand.    
    "
  - name: offhand
    type: "[Item](!SITE_URL!/modules/Item/)"
    access: r/w
    description: "This is the [item](!SITE_URL!/modules/Item/) this player is holding in his off hand.    
    "
  - name: gamemode
    type: string
    access: r/w
    description: |
        This is this player's game mode. It can be one of 'survival', 'adventure', 'creative', 'spectator'.
        #### Example
        Setting the gamemode of this spell's owner to 'adventure'.
        ```lua
        spell.owner.gamemode = "adventure"
        ```
functions:
---

An instance of the <span class="notranslate">Player</span> class represents a specific player who is currently logged into the world.
