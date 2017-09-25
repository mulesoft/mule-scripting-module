/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting;

import org.mule.plugin.scripting.errors.ScriptingErrorTypeProvider;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Text;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Map;

/**
 * Scripting operations.
 *
 * @since 1.0
 */
public class ScriptingOperations {

  /**
   * Run a script with provided code, according to the engine and passing parameters.
   *
   * @param code the script source code to be executed
   * @param engine name of the scripting engine for running ths script
   * @param parameters variables provided to the script as bindings
   * @return the result of script evaluation
   */
  @OutputResolver(output = ScriptingTypeResolver.class)
  @Throws(ScriptingErrorTypeProvider.class)
  public Result<Object, Object> execute(@Text String code,
                                        @OfValues(EnginesValueProvider.class) String engine,
                                        @Optional @Content Map<String, Object> parameters) {

    // the real operation is implemented through a custom executor
    return null;
  }

}
