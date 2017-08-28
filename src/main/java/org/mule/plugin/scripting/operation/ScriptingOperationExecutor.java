package org.mule.plugin.scripting.operation;

import org.mule.plugin.scripting.component.Script;
import org.mule.plugin.scripting.component.ScriptRunner;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;

import java.util.HashMap;

import javax.script.Bindings;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public class ScriptingOperationExecutor implements OperationExecutor {

  @Override
  public Publisher<Object> execute(ExecutionContext<OperationModel> executionContext) {
    //ScriptingExtension config = (ScriptingExtension) executionContext.getConfiguration().get().getValue();

    ExecutionContextAdapter<OperationModel> context = (ExecutionContextAdapter<OperationModel>) executionContext;
    Script scriptConfig = createScriptConfig(context);

    try {
      InternalEvent result = process(context.getEvent(), scriptConfig, context.getComponentLocation(), context.getMuleContext());
      return Mono.justOrEmpty(result);
    } catch (MuleException e) {
      return Mono.error(e);
      //e.printStackTrace();
    }
  }

  public InternalEvent process(InternalEvent event, Script script, ComponentLocation componentLocation, MuleContext muleContext)
      throws MuleException {
    InternalEvent.Builder eventBuilder = InternalEvent.builder(event);

    ScriptRunner scriptRunner = new ScriptRunner(script, muleContext);
    // Set up initial script variables.
    Bindings bindings = scriptRunner.getScriptEngine().createBindings();
    scriptRunner.populateBindings(bindings, componentLocation, event, eventBuilder);
    try {
      final Object result = scriptRunner.runScript(bindings);
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

  private Script createScriptConfig(ExecutionContext<OperationModel> context) {
    Script result = new Script();
    if (context.hasParameter("text")) {
      result.setText(context.getParameter("text"));
    }
    if (context.hasParameter("file")) {
      result.setFile(context.getParameter("file"));
    }
    if (context.hasParameter("engine")) {
      result.setEngine(context.getParameter("engine"));
    }
    if (context.hasParameter("parameters")) {
      result.setParameters(context.getParameter("parameters"));
    } else {
      result.setParameters(new HashMap<>());
    }
    return result;
  }
}
