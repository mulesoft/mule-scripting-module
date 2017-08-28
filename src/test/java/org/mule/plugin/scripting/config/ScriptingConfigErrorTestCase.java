/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.config;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.test.plugin.scripting.AbstractScriptingFunctionalTestCase;

import org.junit.Test;

public class ScriptingConfigErrorTestCase extends AbstractScriptingFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "config-error.xml";
  }

  @Test(expected = InitialisationException.class)
  public void testMissingEngine() throws Exception {
    runFlow("someFlow");
  }
}


