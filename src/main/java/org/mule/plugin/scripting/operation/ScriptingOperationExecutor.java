package org.mule.plugin.scripting.operation;

import org.mule.plugin.scripting.ScriptingExtension;
import org.mule.plugin.scripting.component.Script;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;

import java.util.ArrayList;
import java.util.Optional;

import javax.script.Bindings;

import org.reactivestreams.Publisher;

public class ScriptingOperationExecutor implements OperationExecutor {

  @Override
  public Publisher<Object> execute(ExecutionContext<OperationModel> executionContext) {
    ScriptingExtension config = (ScriptingExtension) executionContext.getConfiguration().get().getValue();

    ExecutionContextAdapter<OperationModel> context = (ExecutionContextAdapter<OperationModel>) executionContext;


    context.getEvent();
    return null;
  }

  public InternalEvent process(InternalEvent event, Script script) throws MuleException {
    InternalEvent.Builder eventBuilder = InternalEvent.builder(event);

    // Set up initial script variables.
    Bindings bindings = script.getScriptEngine().createBindings();
    //script.populateBindings(bindings, getLocation(), event, eventBuilder);
    script.populateBindings(bindings, new DefaultComponentLocation(Optional.of("test"), new ArrayList<>()), event, eventBuilder);
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
}
