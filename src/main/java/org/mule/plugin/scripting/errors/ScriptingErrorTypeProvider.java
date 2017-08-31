/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.errors;

import static org.mule.plugin.scripting.errors.ScriptingErrors.COMPILATION;
import static org.mule.plugin.scripting.errors.ScriptingErrors.EXECUTION;
import static org.mule.plugin.scripting.errors.ScriptingErrors.UNKNOWN_ENGINE;

import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.HashSet;
import java.util.Set;

/**
 * Error type provider for scripting.
 *
 * @since 1.0
 */
public class ScriptingErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    HashSet<ErrorTypeDefinition> result = new HashSet<>();

    result.add(COMPILATION);
    result.add(EXECUTION);
    result.add(UNKNOWN_ENGINE);

    return result;
  }
}
