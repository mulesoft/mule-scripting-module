/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.config;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.config.ConfigurationException;

import org.junit.Test;

public class ScriptingConfigErrorTestCase extends MuleArtifactFunctionalTestCase {

  @Override protected String getConfigFile() {
    return "config-error.xml";
  }

  @Test(expected = InitialisationException.class)
  public void testMissingEngine() throws InitialisationException, ConfigurationException {
    // TODO MULE-10061 - Review once the MuleContext lifecycle is clearly defined
    //new DefaultMuleContextFactory().createMuleContext("config-error.xml");
  }
}


