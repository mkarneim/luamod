package net.wizardsoflua.lua.function;

import net.sandius.rembulan.impl.NonsuspendableFunctionException;
import net.sandius.rembulan.runtime.AbstractFunction5;
import net.sandius.rembulan.runtime.ExecutionContext;
import net.sandius.rembulan.runtime.ResolvedControlThrowable;
import net.wizardsoflua.common.Named;

public abstract class NamedFunction5 extends AbstractFunction5 implements Named {
  @Override
  public void resume(ExecutionContext context, Object suspendedState)
      throws ResolvedControlThrowable {
    throw new NonsuspendableFunctionException(getClass());
  }
}
