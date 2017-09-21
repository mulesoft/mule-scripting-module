/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.plugin.scripting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.message.Message;

import org.junit.Ignore;
import org.junit.Test;

public class GroovyScriptServiceFunctionalTestCase extends AbstractScriptingFunctionalTestCase {

  private TestConnectorQueueHandler queueHandler;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    queueHandler = new TestConnectorQueueHandler(registry);
  }

  @Override
  protected String getConfigFile() {
    return "groovy-component-config.xml";
  }

  @Test
  public void testInlineScript() throws Exception {
    flowRunner("inlineScript").withPayload("Important Message").run();
    Message response = queueHandler.read("inlineScriptTestOut", RECEIVE_TIMEOUT).getMessage();
    assertNotNull(response);
    assertEquals("Important Message Received", getPayloadAsString(response));
  }

  @Ignore("MULE-6926: flaky test")
  @Test
  public void testFileBasedScript() throws Exception {
    flowRunner("fileBasedScript").withPayload("Important Message").run();
    Message response = queueHandler.read("fileBasedScriptTestOut", RECEIVE_TIMEOUT).getMessage();
    assertNotNull(response);
    assertEquals("Important Message Received", getPayloadAsString(response));
  }

  @Ignore("MULE-6926: flaky test")
  @Test
  public void testScriptVariables() throws Exception {
    flowRunner("scriptVariables").withPayload("Important Message").run();
    Message response = queueHandler.read("scriptVariablesTestOut", RECEIVE_TIMEOUT).getMessage();
    assertNotNull(response);
    assertEquals("Important Message Received A-OK", getPayloadAsString(response));
  }
}
