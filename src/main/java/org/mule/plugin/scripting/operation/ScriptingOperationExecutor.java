/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.operation;

import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA;

import org.mule.plugin.scripting.component.ScriptRunner;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.func.Once;
import org.mule.runtime.core.api.util.func.Once.ConsumeOnce;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedResult;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.api.runtime.privileged.StreamingHelperFactory;

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
  private final ConsumeOnce<ExecutionContextAdapter<OperationModel>> initScriptRunner = Once.of(this::initScriptRunner);
  private volatile StreamingHelper streamingHelper;

  @Override
  public Publisher<Object> execute(ExecutionContext<OperationModel> executionContext) {
    ExecutionContextAdapter<OperationModel> context = (ExecutionContextAdapter<OperationModel>) executionContext;

    try {
      Map<String, Object> parameters = context.getParameter("parameters");
      Result<Object, Object> result = process(context.getEvent(), parameters, context);
      return Mono.justOrEmpty(result);
    } catch (Exception e) {
      return Mono.error(e);
    }
  }

  private void initScriptRunner(ExecutionContextAdapter<OperationModel> context) throws MuleException {
    String engine = context.getParameter("engine");
    String code = context.getParameter("code");

    scriptRunner = new ScriptRunner(engine, code, context.getComponent().getLocation());
    context.getMuleContext().getInjector().inject(scriptRunner);
    scriptRunner.initialise();
  }

  private Result<Object, Object> process(CoreEvent event, Map<String, Object> parameters,
                                         ExecutionContextAdapter<OperationModel> context) {
    initScriptRunner.consumeOnce(context);
    Bindings bindings = scriptRunner.getScriptEngine().createBindings();
    if (streamingHelper == null) {
      synchronized (this) {
        if (streamingHelper == null) {
          streamingHelper = new StreamingHelperFactory().resolve(context);
        }
      }
    }
    scriptRunner.populateBindings(bindings, event, parameters, streamingHelper);

    try {
      final Object result = scriptRunner.runScript(bindings);
      if (result instanceof Message) {
        CoreEvent resultEvent = CoreEvent.builder(event).message((Message) result).build();
        return EventedResult.from(resultEvent);
      } else {
        return Result.builder(event.getMessage())
            .attributes(null)
            .output(result)
            .mediaType(APPLICATION_JAVA)
            .build();
      }
    } finally {
      scriptRunner.closeCursors(bindings);
      bindings.clear();
    }
  }
}
