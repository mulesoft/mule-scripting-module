/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.plugin.scripting;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.exception.EventProcessingException;

import org.junit.Test;

public class ScriptingErrorsTestCase extends AbstractScriptingFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "scripting-errors-config.xml";
  }

  @Test
  public void testExecutionError() throws Exception {
    EventProcessingException exception = flowRunner("executionError").runExpectingException();

    assertThat(exception.getEvent().getError().isPresent(), is(true));
    assertThat(exception.getEvent().getError().get().getErrorType().getIdentifier(), is("EXECUTION"));
  }

  @Test
  public void testEngineError() throws Exception {
    EventProcessingException exception = flowRunner("engineError").runExpectingException();

    assertThat(exception.getEvent().getError().isPresent(), is(true));
    assertThat(exception.getEvent().getError().get().getErrorType().getIdentifier(), is("UNKNOWN_ENGINE"));
  }
}
