package org.mule.plugin.scripting.component;

import static org.mule.plugin.scripting.errors.ScriptingErrors.COMPILATION;
import static org.mule.plugin.scripting.errors.ScriptingErrors.EXECUTION;
import static org.mule.plugin.scripting.errors.ScriptingErrors.UNKNOWN_ENGINE;
import static org.mule.runtime.api.el.BindingContextUtils.FLOW;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.BindingContextUtils.VARS;
import static org.mule.runtime.api.el.BindingContextUtils.addEventBindings;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.Binding;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.util.CollectionUtils;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.el.context.EventVariablesMapContext;
import org.mule.runtime.core.el.context.SessionVariableMapContext;
import org.mule.runtime.extension.api.exception.ModuleException;

import java.io.Reader;
import java.io.StringReader;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.Logger;

public class ScriptRunner {

  private static final Logger LOGGER = getLogger(ScriptRunner.class);

  private static final String BINDING_LOG = "log";
  private static final String BINDING_RESULT = "result";
  private static final String BINDING_SESSION_VARS = "sessionVars";

  // TODO MULE-9690 Remove this binding. An object with this key would be available from the registry when the MuleClient is moved
  // to compatibility.
  private static final String BINDING_MULE_CLIENT = "_muleClient";

  private Script scriptConfig;
  private MuleContext muleContext;

  /** A compiled version of the script, if the scripting engine supports it */
  private CompiledScript compiledScript;

  private ScriptEngine scriptEngine;
  private ScriptEngineManager scriptEngineManager;

  public ScriptRunner(Script scriptConfig, MuleContext muleContext) {
    this.scriptConfig = scriptConfig;
    this.muleContext = muleContext;

    initialise();
  }

  public void initialise() {
    scriptEngineManager = new ScriptEngineManager(this.getClass().getClassLoader());

    scriptEngine = createScriptEngineByName(scriptConfig.getEngine());
    if (scriptEngine == null) {
      String message =
          "Scripting engine '" + scriptConfig.getEngine() + "' not found.  Available engines are: " + listAvailableEngines();
      throw new ModuleException(createStaticMessage(message), UNKNOWN_ENGINE);
    }

    Reader script = new StringReader(scriptConfig.getCode());
    try {
      // Pre-compile script if scripting engine supports compilation.
      if (scriptEngine instanceof Compilable) {
        try {
          compiledScript = ((Compilable) scriptEngine).compile(script);
        } catch (ScriptException e) {
          throw new ModuleException(COMPILATION, e);
        }
      }
    } finally {
      IOUtils.closeQuietly(script);
    }
  }

  protected void populatePropertyBindings(Bindings bindings) {
    bindings.putAll(scriptConfig.getParameters());
  }

  public void populateDefaultBindings(Bindings bindings) {
    bindings.put(BINDING_LOG, LOGGER);
    // A place holder for a returned result if the script doesn't return a result.
    // The script can overwrite this binding
    bindings.put(BINDING_RESULT, null);
  }

  public void populateBindings(Bindings bindings, ComponentLocation location, InternalEvent event,
                               InternalEvent.Builder eventBuilder) {
    // TODO MULE-10121 Provide a MessageBuilder API in scripting components to improve usability
    for (Binding binding : addEventBindings(event, NULL_BINDING_CONTEXT).bindings()) {
      bindings.put(binding.identifier(), binding.value().getValue());
    }
    bindings.put(VARS, new EventVariablesMapContext(event, eventBuilder));
    bindings.put(BINDING_SESSION_VARS, new SessionVariableMapContext(event.getSession()));
    bindings.put(FLOW, location.getRootContainerName());

    populatePropertyBindings(bindings);
    populateDefaultBindings(bindings);

    // TODO MULE-9690 Remove this binding. An object with this key would be available from the registry when the MuleClient is
    // moved to compatibility.
    bindings.put(BINDING_MULE_CLIENT, muleContext.getClient());
  }

  public Object runScript(Bindings bindings) {
    Object result;
    try {
      RegistryLookupBindings registryLookupBindings =
          new RegistryLookupBindings(muleContext.getRegistry(), bindings);
      if (compiledScript != null) {
        result = compiledScript.eval(registryLookupBindings);
      } else {
        result = scriptEngine.eval(scriptConfig.getCode(), registryLookupBindings);
      }

      // The result of the script can be returned directly or it can
      // be set as the variable "result".
      if (result == null) {
        result = registryLookupBindings.get(BINDING_RESULT);
      }
    } catch (Exception ex) {
      throw new ModuleException(EXECUTION, ex);
    }
    return result;
  }

  protected ScriptEngine createScriptEngineByName(String name) {
    return scriptEngineManager.getEngineByName(name);
  }

  protected String listAvailableEngines() {
    return CollectionUtils.toString(scriptEngineManager.getEngineFactories(), false);
  }

  public ScriptEngine getScriptEngine() {
    return scriptEngine;
  }
}
