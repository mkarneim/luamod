---
title: The Wizards of Lua Configuration File
---
*The Wizards of Lua Mod can be configured by editing its configuration file.*

The Wizards of Lua configuration file is located at

    <minecraft>/conf/wizards-of-lua/wizards-of-lua.luacfg

where &lt;minecraft&gt; is your Minecraft folder.

Please note that you should only edit it manually when Minecraft is not running.
Otherwise your changes will be ignored and might get overwritten when the mod's configuration has been changed by some [/wol command](/wol-command.html).

## Example
Here is an example of the <tt>wizards-of-lua.luacfg</tt>:
```lua
General {
  luaTicksLimit=10000,
  showAboutMessage=true,
  luaLibDirHome="libs",
  sharedLibDir="shared" }
RestApi {
  hostname="example.com",
  port=60080,
  secure=true,
  keystore="ssl-keystore.jks",
  keystorePassword="123456",
  keyPassword="123456",
  webDir="www",
  uuid="fd19a362-04fa-4a92-9481-cd21a85c44d8",
  apiKey="sVu4QB1joXfQCM_DUAcFpw" }
Wizard {
  id="0c77f1a8-943b-4f7b-aa09-f2b1737d4f03",
  libDir="0c77f1a8-943b-4f7b-aa09-f2b1737d4f03",
  apiKey="8lpZ81w7AGWfKCRLHSZtvA" }
Wizard {
  id="d26feccb-baae-4d90-8e3d-50389e8b8ad9",
  libDir="d26feccb-baae-4d90-8e3d-50389e8b8ad9",
  apiKey="9cssfWocUNoWVhA7-m50Cw" }
```

## Format
The contents of <tt>wizards-of-lua.luacfg</tt> is valid Lua code which is loaded and executed by Wol at server startup.
"General", "RestApi", and "Wizard" are internal functions that accept a Lua table as parameter.
All values (with the exception of the wizard id) are optional - in that sense that you can omit the assignment, but Wol will choose a default value and store it into this file.

### General
This function sets the mod's general configuration.
* **luaTicksLimit**: defines the number of Lua ticks a spell can run continuously before it must sleep at least for one game tick.
This value can be modified from within the game by [/wol luaTicksLimit](/wol-command.html#Lua-Ticks-Limit).
* **showAboutMessage**: defines, whether the mod's about message is shown to new users when they log into the server.
Please note, that until the next server restart the about message is only shown once to each user.
* **luaLibDirHome**: defines the relative or absolute location of the home of all Lua library directories.
This directory is parent to the shared lib and all user-specific library directories.
For more information about this value please have a look into the [tutorial about importing Lua files](/tutorials/importing_lua_files).
* **sharedLibDir**: defines the relative or absolute location of the shared library directory.
This directory is parent of all Lua modules that can be used by all spell.
For more information about this value please have a look into the [tutorial about importing Lua files](/tutorials/importing_lua_files).

### RestApi
This function sets the mod's REST server configuration.
The REST server is running inside your Minecraft server and provides HTTP (or HTTPS) access to your Lua files.
All read and write access is protected by a combination of the server's secret *apiKey* and the player's secret *apiKey*.
* **hostname**: defines the name of the REST server. This name will be used in the servers HTTP URL.
For example, if you set the hostname to "wizards.example.com", the resulting URL will start with "http://wizards.example.com", or "https://wizards.example.com" respectively.
To make the REST server only accessible from your local computer, set this to "127.0.0.1". Default is "127.0.0.1".
* **port**: defines the port number of the REST server. This number will be used in the servers HTTP URL.
Default is "60000".
* **secure**: defines whether the REST server should use Transport Layer Security (TLS aka. SSL). If this is set to "true", the REST server is only accessible through HTTPS, and the following properties must also be defined: keystore, keystorePassword, keyPassword.
Default is "false".
* **keystore**: this is the filename of the keystore file that contains the server's SSL certificate. If provided, this file must be placed next to the "server.properties" file.
This is only used if *secure* is set to "true".
Default is "".
* **keystorePassword**: this is the password that must be used to access the keystore.
This is only used if *secure* is set to "true".
Default is "".
* **keyPassword**: this is the password that must be used to decrypt the server's SSL certificate from the keystore.
This is only used if *secure* is set to "true".
Default is "".
* **webDir**: this is the filesystem path to the directory where the REST server caches static files.
Its contents are deleted and recreated when the Minecraft server starts.
This directory will be inside the mod's configuration directory.
Default is "www".
* **uuid**: this is the REST servers UUID. This is currently not used, but may be used in a future version of this mod for
authentication purpose.
* **apiKey**: this is the server's randomly generated key that is used for authenticating a REST client (e.g. the web browser). This is used if you want to edit Lua files with your web browser. You should not edit its value. If you want to change it, please do so by deleting it and restarting the server.

### Wizard
This function adds a player-specific configuration.
New entries are added automatically when a new player uses the [/lua command](/lua-command.html) or the [/wol command](/wol-command.html) the first time.
* **id**: is the UUID of the player this configuration belongs to.
* **libDir**: defines the relative or absolute location of the player-specific Lua library directory.
This directory is parent of all Lua modules that can be used by the player.
If defined as relative, it will be located inside the directory defined by <tt>General.luaLibDirHome</tt>.
For more information about this value please have a look into the [tutorial about importing Lua files](/tutorials/importing_lua_files).
* **apiKey**: is a personal and randomly generated key that is used for authenticating a REST client (e.g. the web browser). This is used if you want to edit Lua files with your web browser. You should not edit its value. If you want to change it, please do so by calling ```/wol browser logout``` - this will generate a new random key.
