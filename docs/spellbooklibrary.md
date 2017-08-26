# The Lua Spell Book Library
The Wizards of Lua use magic words as ingredients to craft powerful spells.
The Lua Spell Book Library contains a collection of books that list all basic
magic words that have been discovered, or invented, by the very first members,
called archmages, so far.
There are a lot of different categories of magic words:
keywords, modules, classes, properties, and functions.
The books listed below cover all those categories, but not all words completely.
There are, almost certainly, more words out there, but to begin with, this library
is a good starting point.

![Library of Lua](images/library-of-lua.jpg)

Below you find a list of books that are organized by modules and classes.
Every type and module is documented within its own book.
Inside each book you will find sections about properties and functions.
In each of those there is a brief description about their nature, their effects,
and, if available, about the options you have to influence these effects.

To make it easy for the reader, the authors have added some small examples to
each section, that can be used as recipes for crafting spells.

This list is sorted alphabetically.

{% assign modules = site.modules | where_exp:"m", "m.title != 'TODO'" %}
{% assign modulesAvail = modules | sort: 'name' %}
{% for module in modulesAvail %}
* <a href="{{ module.url }}">{{ module.name }}: {{ module.title }}</a>
{% endfor %}
