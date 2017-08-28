/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting;

import org.mule.plugin.scripting.component.Script;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Map;

public class ScriptingOperations {

  @OutputResolver(output = Resolver.class)
  public Result<Object, Object> execute(@ParameterGroup(name = "script") Script script,
                                        @Content @TypeResolver(Resolver.class) @Optional(
                                            defaultValue = "#[{}]") Map<String, Object> parameteres) {
    return null;
  }

}
