/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting;

import static org.mule.runtime.extension.api.values.ValueBuilder.getValuesFor;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * {@link ValueProvider} implementation which provides the common values for the script engine parameter.
 *
 * @since 1.0
 */
public final class EnginesValueProvider implements ValueProvider {

  private static final Map<String, String> engines =
      Collections.unmodifiableMap(new HashMap<String, String>() {

        {
          put("groovy", "Groovy");
          put("jython", "Jython (Python)");
          put("ruby", "JRuby (Ruby)");
          put("nashorn", "Nashorn (JavaScript)");
        }
      });

  private static final Set<Value> values = getValuesFor(engines);

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return values;
  }
}
