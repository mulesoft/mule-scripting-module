/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.plugin.scripting;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.is;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;

import org.junit.Test;

public class ScriptingErrorsTestCase extends AbstractScriptingFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "scripting-errors-config.xml";
  }

  @Test
  public void testExecutionError() throws Exception {
    flowRunner("executionError").runExpectingException(errorType(any(String.class), is("EXECUTION")));
  }

  @Test
  public void testEngineError() throws Exception {
    flowRunner("engineError").runExpectingException(errorType(any(String.class), is("UNKNOWN_ENGINE")));
  }
}
