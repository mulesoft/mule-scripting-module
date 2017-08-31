/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.errors;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

/**
 * The extension can throw the following scripting related errors:
 * - Scripting engine not found
 * - During script compilation
 * - During script execution
 *
 * @since 1.0
 */
public enum ScriptingErrors implements ErrorTypeDefinition<ScriptingErrors> {
  EXECUTION, COMPILATION, UNKNOWN_ENGINE
}
