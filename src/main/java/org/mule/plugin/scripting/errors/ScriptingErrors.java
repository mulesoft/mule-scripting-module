/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.errors;

import static java.util.Optional.ofNullable;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;

import java.util.Optional;

/**
 * The extension can throw the following scripting related errors:
 * - Scripting engine not found
 * - During script compilation
 * - During script execution
 *
 * @since 4.0
 */
public enum ScriptingErrors implements ErrorTypeDefinition<ScriptingErrors> {
  EXECUTION(MuleErrors.ANY), COMPILATION(MuleErrors.ANY), UNKNOWN_ENGINE(MuleErrors.ANY);

  private ErrorTypeDefinition<?> parentErrortype;

  ScriptingErrors(ErrorTypeDefinition parentErrorType) {
    this.parentErrortype = parentErrorType;
  }

  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return ofNullable(parentErrortype);
  }
}
