/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.component;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.processor.AbstractProcessor;

import javax.script.Bindings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Script service backed by a JSR-223 compliant script engine such as Groovy, JavaScript, or Rhino.
 */
public class ScriptProcessor extends AbstractProcessor
    implements Initialisable, Disposable, MuleContextAware {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScriptProcessor.class);

  private MuleContext muleContext;
  private Scriptable script;

  @Override
  public void initialise() throws InitialisationException {
    LifecycleUtils.initialiseIfNeeded(script, muleContext);
  }

  @Override
  public void dispose() {
    LifecycleUtils.disposeIfNeeded(script, LOGGER);
  }

  @Override
  public InternalEvent process(InternalEvent event) throws MuleException {
    InternalEvent.Builder eventBuilder = InternalEvent.builder(event);

    // Set up initial script variables.
    Bindings bindings = script.getScriptEngine().createBindings();
    script.populateBindings(bindings, getRootContainerName(), getLocation(), event, eventBuilder);
    try {
      final Object result = script.runScript(bindings);
      if (result instanceof Message) {
        eventBuilder.message((Message) result);
      } else {
        eventBuilder.message(Message.builder(event.getMessage()).value(result).build());
      }
    } catch (Exception e) {
      // leave this catch block in place to help debug classloading issues
      throw new DefaultMuleException(e);
    } finally {
      bindings.clear();
    }

    return eventBuilder.build();
  }

  public Scriptable getScript() {
    return script;
  }

  public void setScript(Scriptable script) {
    this.script = script;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

}
