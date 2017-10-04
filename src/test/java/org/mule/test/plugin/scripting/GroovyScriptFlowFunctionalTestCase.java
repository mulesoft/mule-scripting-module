/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.plugin.scripting;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.testmodels.fruit.Apple;

import org.junit.Test;

public class GroovyScriptFlowFunctionalTestCase extends GroovyScriptServiceFunctionalTestCase {

  @Test
  public void inlineScriptMutateProperty() throws Exception {
    flowRunner("inlineScriptMutateProperty").withPayload("").withOutboundProperty("foo", "bar").run();
  }

  @Test
  public void inlineScriptAddProperty() throws Exception {
    flowRunner("inlineScriptAddProperty").withPayload("").run();
  }

  @Test
  public void inlineScriptClearProperties() throws Exception {
    flowRunner("inlineScriptClearProperties").withPayload("").withOutboundProperty("foo", "bar").run();
  }

  @Test
  public void inlineScriptCannotMutateVariable() throws Exception {
    Exception exception =
        flowRunner("inlineScriptMutateVariable").withPayload("").withVariable("foo", "bar").runExpectingException();
    assertThat(exception.getCause().getCause(), instanceOf(UnsupportedOperationException.class));
  }

  @Test
  public void inlineScriptCannotAddVariable() throws Exception {
    Exception exception = flowRunner("inlineScriptAddVariable").withPayload("").runExpectingException();
    assertThat(exception.getCause().getCause(), instanceOf(UnsupportedOperationException.class));
  }

  @Test
  public void inlineScriptCannotMutateVariablesMap() throws Exception {
    flowRunner("inlineScriptMutateVariablesMap").withPayload("").withVariable("foo", "bar").run();
  }

  @Test
  public void inlineScriptCannotMutatePayload() throws Exception {
    flowRunner("inlineScriptMutatePayload").withPayload("").run();
  }

  @Test
  public void inlineScriptRemovesAttributes() throws Exception {
    CoreEvent event = flowRunner("inlineScriptMutatePayload").withPayload("").withAttributes("Test").run();
    assertThat(event.getMessage().getAttributes().getValue(), is(nullValue()));
  }

  @Test
  public void scriptExpressionVariables() throws Exception {
    flowRunner("scriptExpressionVariables").withPayload("").withVariable("prop1", "Received")
        .withVariable("prop2", "A-OK").run();
  }

  @Test
  public void scriptReferencesAppClass() throws Exception {
    final Object value = flowRunner("scriptReferencesAppClass").withPayload("").run().getMessage().getPayload().getValue();

    assertThat(value, instanceOf(Apple.class));
    assertThat(value.getClass().getClassLoader().getClass().getName(),
               is("org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoader"));
  }

  @Test
  public void testInlineTransformer() throws Exception {
    TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(registry);
    flowRunner("inlineScriptFlow").withPayload("hello").run();
    Message response = queueHandler.read("inlineScriptTestOut", RECEIVE_TIMEOUT).getMessage();
    assertThat(response, not(nullValue()));
    assertThat(response.getPayload().getValue(), is("hexxo"));
  }

  @Test
  public void testInlineScriptWithParameters() throws Exception {
    TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(registry);
    flowRunner("inlineScriptWithParametersFlow").withPayload("hello").run();
    Message response =
        queueHandler.read("inlineScriptWithParametersTestOut", RECEIVE_TIMEOUT).getMessage();
    assertThat(response, not(nullValue()));
    assertThat(response.getPayload().getValue(), is("hexxo"));
  }

  @Test
  public void inlineScriptWithResolvedParameters() throws Exception {
    CoreEvent event;
    event = flowRunner("inlineScriptWithResolvedParameters")
        .withPayload("{\"element\": 1}")
        .withMediaType(MediaType.APPLICATION_JSON)
        .run();
    assertThat(event.getMessage().getPayload().getValue(), is(1));

    event = flowRunner("inlineScriptWithResolvedParameters")
        .withPayload("{\"element\": 2}")
        .withMediaType(MediaType.APPLICATION_JSON)
        .run();
    assertThat(event.getMessage().getPayload().getValue(), is(2));
  }

  @Test
  public void inlineScriptTargetValue() throws Exception {
    CoreEvent event = flowRunner("inlineScriptTargetValue").withPayload("original").run();
    assertThat(event.getVariables().get("myVar").getValue(), is("hello"));
    assertThat(event.getMessage().getPayload().getValue(), is("original"));
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"groovy-component-config-flow.xml", "groovy-component-config.xml"};
  }

  @Override
  protected String getConfigFile() {
    return null;
  }
}
