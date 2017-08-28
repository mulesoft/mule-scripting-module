/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting;

import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Map;

public class ScriptingOperations {

  @OutputResolver(output = ScriptingTypeResolver.class)
  public Result<Object, Object> execute(@Optional @Content(primary = true) String text,
                                        @Optional String engine,
                                        @Optional String file,
                                        @Optional @Content Map<String, Object> parameters) {
    return null;
  }

}
