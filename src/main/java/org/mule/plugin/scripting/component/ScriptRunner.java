package org.mule.plugin.scripting.component;

import static org.mule.runtime.api.el.BindingContextUtils.FLOW;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.BindingContextUtils.VARS;
import static org.mule.runtime.api.el.BindingContextUtils.addEventBindings;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.cannotLoadFromClasspath;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.propertiesNotSet;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsStream;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.Binding;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.util.CollectionUtils;
import org.mule.runtime.core.el.context.EventVariablesMapContext;
import org.mule.runtime.core.el.context.SessionVariableMapContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

  private static final Logger LOGGER = getLogger(Script.class);

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

    // Create scripting engine
    if (scriptConfig.getEngine() != null) {
      scriptEngine = createScriptEngineByName(scriptConfig.getEngine());
      if (scriptEngine == null) {
        throw new MuleRuntimeException(createStaticMessage("Scripting engine '" + scriptConfig.getEngine()
            + "' not found.  Available engines are: " + listAvailableEngines()));
      }
    }
    // Determine scripting engine to use by file extension
    else if (scriptConfig.getFile() != null) {
      int i = scriptConfig.getFile().lastIndexOf(".");
      if (i > -1) {
        LOGGER.info("Script Engine name not set. Guessing by file extension.");
        String extension = scriptConfig.getFile().substring(i + 1);
        scriptEngine = createScriptEngineByExtension(extension);
        if (scriptEngine == null) {
          throw new MuleRuntimeException(createStaticMessage("File extension '" + extension
              + "' does not map to a scripting engine.  Available engines are: " + listAvailableEngines()));
        } else {
          //setEngine(extension);
        }
      }
    } else {
      throw new MuleRuntimeException(createStaticMessage("Scripting engine not specified and file extension is not available for guessing. Available engines are: "
          + listAvailableEngines()));
    }

    Reader script = null;
    try {
      // Load script from variable
      if (!isBlank(scriptConfig.getText())) {
        script = new StringReader(scriptConfig.getText());
      }
      // Load script from file
      else if (scriptConfig.getFile() != null) {
        InputStream is;
        try {
          is = getResourceAsStream(scriptConfig.getFile(), getClass());
        } catch (IOException e) {
          throw new MuleRuntimeException(cannotLoadFromClasspath(scriptConfig.getFile()), e);
        }
        if (is == null) {
          throw new MuleRuntimeException(cannotLoadFromClasspath(scriptConfig.getFile()));
        }
        script = new InputStreamReader(is);
      } else {
        throw new MuleRuntimeException(propertiesNotSet("text, file"));
      }

      // Pre-compile script if scripting engine supports compilation.
      if (scriptEngine instanceof Compilable) {
        try {
          compiledScript = ((Compilable) scriptEngine).compile(script);
        } catch (ScriptException e) {
          throw new MuleRuntimeException(e);
        }
      }
    } finally {
      if (script != null) {
        try {
          script.close();
        } catch (IOException e) {
          throw new MuleRuntimeException(e);
        }
      }
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

  public Object runScript(Bindings bindings) throws ScriptException {
    Object result;
    try {
      RegistryLookupBindings registryLookupBindings =
          new RegistryLookupBindings(muleContext.getRegistry(), bindings);
      if (compiledScript != null) {
        result = compiledScript.eval(registryLookupBindings);
      } else {
        result = scriptEngine.eval(scriptConfig.getText(), registryLookupBindings);
      }

      // The result of the script can be returned directly or it can
      // be set as the variable "result".
      if (result == null) {
        result = registryLookupBindings.get(BINDING_RESULT);
      }
    } catch (ScriptException e) {
      // re-throw
      throw e;
    } catch (Exception ex) {
      throw new ScriptException(ex);
    }
    return result;
  }

  protected ScriptEngine createScriptEngineByName(String name) {
    return scriptEngineManager.getEngineByName(name);
  }

  protected ScriptEngine createScriptEngineByExtension(String ext) {
    return scriptEngineManager.getEngineByExtension(ext);
  }

  protected String listAvailableEngines() {
    return CollectionUtils.toString(scriptEngineManager.getEngineFactories(), false);
  }

  public ScriptEngine getScriptEngine() {
    return scriptEngine;
  }
}
