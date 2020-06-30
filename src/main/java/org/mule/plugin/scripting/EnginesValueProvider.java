/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting;

import static org.mule.runtime.extension.api.values.ValueBuilder.getValuesFor;
import static java.lang.Thread.currentThread;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link ValueProvider} implementation which provides the common values for the script engine parameter.
 *
 * @since 1.0
 */
public final class EnginesValueProvider implements ValueProvider {

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    ScriptEngineManager scriptEngineManager = new ScriptEngineManager(currentThread().getContextClassLoader());
    List<ScriptEngineFactory> scriptEngineFactories = scriptEngineManager.getEngineFactories();
    Map<String, String> map = new HashMap<>();

    try {
      scriptEngineFactories.forEach(entry -> map.put(entry.getLanguageName(),
                                                     entry.getEngineName()));
    } catch (Exception e) {
      throw new ValueResolvingException(e.getMessage(), ValueResolvingException.UNKNOWN);
    }

    return getValuesFor(map);
  }
}
