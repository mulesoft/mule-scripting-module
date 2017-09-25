/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.operation;

import org.mule.plugin.scripting.component.Script;
import org.mule.plugin.scripting.component.ScriptRunner;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import javax.script.Bindings;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Custom executor to allow scripts to modify event variables.
 *
 * @since 1.0
 */
public class ScriptingOperationExecutor implements ComponentExecutor<OperationModel> {

  private ScriptRunner scriptRunner;

  @Override
  public Publisher<Object> execute(ExecutionContext<OperationModel> executionContext) {
    ExecutionContextAdapter<OperationModel> context = (ExecutionContextAdapter<OperationModel>) executionContext;

    Script scriptConfig = new Script(context);

    try {
      CoreEvent result = process(context.getEvent(), scriptConfig, context.getComponentLocation(), context.getMuleContext());
      return Mono.justOrEmpty(result);
    } catch (Exception e) {
      return Mono.error(e);
    }
  }

  private CoreEvent process(CoreEvent event, Script script, ComponentLocation componentLocation, MuleContext muleContext)
      throws MuleException {
    CoreEvent.Builder eventBuilder = CoreEvent.builder(event);
    if (scriptRunner == null) {
      scriptRunner = new ScriptRunner(script, muleContext);
    }
    Bindings bindings = scriptRunner.getScriptEngine().createBindings();
    scriptRunner.populateBindings(bindings, componentLocation, event, eventBuilder);

    try {
      final Object result = scriptRunner.runScript(bindings);
      if (result instanceof Message) {
        eventBuilder.message((Message) result);
      } else {
        eventBuilder.message(Message.builder(event.getMessage()).value(result).build());
      }
    } finally {
      bindings.clear();
    }

    return eventBuilder.build();
  }
}
