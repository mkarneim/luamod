package net.wizardsoflua.lua.converter.nbt;

import com.google.auto.service.AutoService;
import net.minecraft.nbt.NBTTagInt;
import net.wizardsoflua.extension.spell.spi.JavaToLuaConverter;
import net.wizardsoflua.lua.converter.AnnotatedJavaToLuaConverter;
import net.wizardsoflua.lua.converter.LuaConverterAttributes;
import net.wizardsoflua.lua.nbt.NbtConverter;

@AutoService(JavaToLuaConverter.class)
@LuaConverterAttributes(name = "table")
public class IntNbtConverter extends AnnotatedJavaToLuaConverter<NBTTagInt> {
  @Override
  public Object getLuaInstance(NBTTagInt javaInstance) {
    return NbtConverter.toLua(javaInstance);
  }
}
