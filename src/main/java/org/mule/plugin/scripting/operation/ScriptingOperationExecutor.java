/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.operation;

import org.mule.plugin.scripting.component.ScriptRunner;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedResult;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import java.util.Map;

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

    try {
      if (scriptRunner == null) {
        String engine = context.getParameter("engine");
        String code = context.getParameter("code");

        scriptRunner = new ScriptRunner(engine, code, context.getComponentLocation());
        context.getMuleContext().getInjector().inject(scriptRunner);
        scriptRunner.initialise();
      }

      Map<String, Object> parameters = context.getParameter("parameters");
      Result<Object, Object> result = process(context.getEvent(), parameters);
      return Mono.justOrEmpty(result);
    } catch (Exception e) {
      return Mono.error(e);
    }
  }

  private Result<Object, Object> process(CoreEvent event, Map<String, Object> parameters)
      throws MuleException {
    Bindings bindings = scriptRunner.getScriptEngine().createBindings();
    scriptRunner.populateBindings(bindings, event, parameters);

    try {
      final Object result = scriptRunner.runScript(bindings);
      if (result instanceof Message) {
        CoreEvent resultEvent = CoreEvent.builder(event).message((Message) result).build();
        return EventedResult.from(resultEvent);
      } else {
        return Result.builder(event.getMessage()).output(result).build();
      }
    } finally {
      bindings.clear();
    }
  }
}
