---
title: The Art of Spell Crafting
---
# Welcome to the Home of the Wizards of Lua

The Wizards of Lua are a [community of programmers and their friends](members.md),
who want to spread the knowledge of programming to kids.
The Wizards of Lua are also some decent gamers
who want to give gamers the ability to create their own game contents.

And finally 'The Wizards of Lua' is the name of a Minecraft
modification that simply adds the **/lua** command to the game.

# News
<ul>
  {% for post in site.posts %}
    <li>
      <a href="{{ post.url }}">{{ post.title }}</a>
    </li>
  {% endfor %}
</ul>
