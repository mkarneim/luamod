{% if page.extends %}
{% assign modules = site.modules | where_exp:"m", "m.name == page.extends" %}
The {{ page.name }} class is a subtype of the <a href="{{ modules[0].url }}">{{ modules[0].name }}</a> class and inherits all its properties and functions.
{% endif %}
{% if page.properties %}
Here is an overview of the {{ page.name }} *properties*:

| Property             | Type          | read / write |
| ---------------------|---------------| :-----------:|
{% for prop in properties %}| [<span class="notranslate">{{ prop.name }}</span>](#{{ prop.name }}) | <span class="notranslate">{{ prop.type | replace: '!SITE_URL!', site.url }}</span> | {{ prop.access }} |
{% endfor %}
{% endif %}
{% if page.functions %}
Here is an overview of the {{ page.name }} *functions*:

| Function             | Parameters    | Results      |
| ---------------------|---------------| :-----------:|
{% for func in functions %}| [<span class="notranslate">{{ func.name }}</span>](#{{ func.name }}) | <span class="notranslate">{{ func.parameters | replace: '!SITE_URL!', site.url }}</span> | <span class="notranslate">{{ func.results | replace: '!SITE_URL!', site.url }}</span> |
{% endfor %}
{% endif %}

{% if page.properties %}
## Properties

Below you find short descriptions about each of the properties
and some examples about how to use them in your spells.

---
{% for prop in properties %}
<a style="position:relative; top:-70px; display:block;" name="{{ prop.name }}"></a>
### <span class="notranslate">{{ prop.name }}</span> : {{ prop.type | replace: '!SITE_URL!', site.url }}

{{ prop.description | replace: '!SITE_URL!', site.url}}
{% for ex in prop.examples %}
{% include_relative {{ ex.url }} %}
{% endfor %}
---
{% endfor %}
{% endif %}

{% if page.functions %}
## Functions

Below you find short descriptions about each of the functions
and some examples about how to use them in your spells.

---
{% for func in functions %}
<a style="position:relative; top:-70px; display:block;" name="{{ func.name }}"></a>
### <span class="notranslate">{{ func.name }}</span> ({{ func.parameters | replace: '!SITE_URL!', site.url }}) -> <span class="notranslate">{{ func.results | replace: '!SITE_URL!', site.url }}</span>

{{ func.description | replace: '!SITE_URL!', site.url}}
{% for ex in func.examples %}
{% include_relative {{ ex.url }} %}
{% endfor %}
---
{% endfor %}
{% endif %}
