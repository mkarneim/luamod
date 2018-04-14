package net.wizardsoflua.lua.table;

import static com.google.common.base.Preconditions.checkNotNull;

import net.wizardsoflua.lua.classes.common.Delegator;
import net.wizardsoflua.lua.extension.api.service.Converter;

public class GeneratedLuaTable<D> extends PropertyTable implements Delegator<D> {
  private final D delegate;
  private final Converter converter;

  public GeneratedLuaTable(D delegate, Converter converter, boolean allowAdditionalProperties) {
    super(allowAdditionalProperties);
    this.delegate = checkNotNull(delegate, "delegate == null!");
    this.converter = checkNotNull(converter, "converter == null!");
  }

  @Override
  public D getDelegate() {
    return delegate;
  }

  public Converter getConverter() {
    return converter;
  }
}
