/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.connectivity.NoConnectivityTest;

import javax.sql.DataSource;
import static org.mule.runtime.api.meta.ExternalLibraryType.JAR;

/**
 * {@link ConnectionProvider} implementation which creates DB connections from a referenced {@link
 * DataSource}
 *
 * @since 1.0
 */
@DisplayName("Data Source Reference Connection")
@Alias("data-source")
@ExternalLib(name = "JSR-223 Engine", description = "A JSR-223 supported engine",
    nameRegexpMatcher = "(.*)\\.jar", type = JAR, optional = true)
public class GenericEngineProvider implements ConnectionProvider<ScriptingEngine>, NoConnectivityTest {

  @Override
  public ScriptingEngine connect() throws ConnectionException {
    return null;
  }

  @Override
  public void disconnect(ScriptingEngine scriptingEngine) {

  }

  @Override
  public ConnectionValidationResult validate(ScriptingEngine scriptingEngine) {
    return null;
  }
}
