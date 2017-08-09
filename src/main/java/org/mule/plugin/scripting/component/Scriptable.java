/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
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
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.util.CollectionUtils;
import org.mule.runtime.core.el.context.EventVariablesMapContext;
import org.mule.runtime.core.el.context.SessionVariableMapContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import javax.inject.Inject;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.Logger;

/**
 * A JSR 223 Script service. Allows any JSR 223 compliant script engines such as JavaScript, Groovy or Rhino to be embedded as
 * Mule components.
 * 
 * @since 1.0
 */
public class Scriptable implements Initialisable {

  private static final Logger LOGGER = getLogger(Scriptable.class);

  private static final String BINDING_LOG = "log";
  private static final String BINDING_RESULT = "result";
  private static final String BINDING_SESSION_VARS = "sessionVars";

  // TODO MULE-9690 Remove this binding. An object with this key would be available from the registry when the MuleClient is moved
  // to compatibility.
  private static final String BINDING_MULE_CLIENT = "_muleClient";

  /** The actual body of the script */
  private String scriptText;

  /** A file from which the script will be loaded */
  private String scriptFile;

  /** Parameters to be made available to the script as variables */
  private List<ScriptingProperty> properties;

  /** The name of the JSR 223 scripting engine (e.g., "groovy") */
  private String scriptEngineName;

  // ///////////////////////////////////////////////////////////////////////////
  // Internal variables, not exposed as properties
  // ///////////////////////////////////////////////////////////////////////////

  /** A compiled version of the script, if the scripting engine supports it */
  private CompiledScript compiledScript;

  private ScriptEngine scriptEngine;
  private ScriptEngineManager scriptEngineManager;

  @Inject
  private ExtendedExpressionManager expressionManager;

  @Inject
  private MuleContext muleContext;

  @Override
  public void initialise() throws InitialisationException {
    scriptEngineManager = new ScriptEngineManager(this.getClass().getClassLoader());

    // Create scripting engine
    if (scriptEngineName != null) {
      scriptEngine = createScriptEngineByName(scriptEngineName);
      if (scriptEngine == null) {
        throw new InitialisationException(createStaticMessage("Scripting engine '" + scriptEngineName
            + "' not found.  Available engines are: " + listAvailableEngines()), this);
      }
    }
    // Determine scripting engine to use by file extension
    else if (scriptFile != null) {
      int i = scriptFile.lastIndexOf(".");
      if (i > -1) {
        LOGGER.info("Script Engine name not set. Guessing by file extension.");
        String extension = scriptFile.substring(i + 1);
        scriptEngine = createScriptEngineByExtension(extension);
        if (scriptEngine == null) {
          throw new InitialisationException(createStaticMessage("File extension '" + extension
              + "' does not map to a scripting engine.  Available engines are: " + listAvailableEngines()), this);
        } else {
          setScriptEngineName(extension);
        }
      }
    }

    Reader script = null;
    try {
      // Load script from variable
      if (!isBlank(scriptText)) {
        script = new StringReader(scriptText);
      }
      // Load script from file
      else if (scriptFile != null) {
        InputStream is;
        try {
          is = getResourceAsStream(scriptFile, getClass());
        } catch (IOException e) {
          throw new InitialisationException(cannotLoadFromClasspath(scriptFile), e, this);
        }
        if (is == null) {
          throw new InitialisationException(cannotLoadFromClasspath(scriptFile), this);
        }
        script = new InputStreamReader(is);
      } else {
        throw new InitialisationException(propertiesNotSet("scriptText, scriptFile"), this);
      }

      // Pre-compile script if scripting engine supports compilation.
      if (scriptEngine instanceof Compilable) {
        try {
          compiledScript = ((Compilable) scriptEngine).compile(script);
        } catch (ScriptException e) {
          throw new InitialisationException(e, this);
        }
      }
    } finally {
      if (script != null) {
        try {
          script.close();
        } catch (IOException e) {
          throw new InitialisationException(e, this);
        }
      }
    }
  }

  protected void populatePropertyBindings(Bindings bindings, InternalEvent event, ComponentLocation location) {
    if (properties != null) {
      for (ScriptingProperty property : properties) {
        String value = (String) property.getValue();
        if (expressionManager.isExpression(value)) {
          bindings.put(property.getKey(), expressionManager.evaluate(value, event, location).getValue());
        } else {
          bindings.put(property.getKey(), value);
        }
      }
    }
  }

  public void populateDefaultBindings(Bindings bindings) {
    bindings.put(BINDING_LOG, LOGGER);
    // A place holder for a returned result if the script doesn't return a result.
    // The script can overwrite this binding
    bindings.put(BINDING_RESULT, null);
  }

  public void populateBindings(Bindings bindings, String rootContainerName, ComponentLocation location, InternalEvent event,
                               InternalEvent.Builder eventBuilder) {
    // TODO MULE-10121 Provide a MessageBuilder API in scripting components to improve usability
    for (Binding binding : addEventBindings(event, NULL_BINDING_CONTEXT).bindings()) {
      bindings.put(binding.identifier(), binding.value().getValue());
    }
    bindings.put(VARS, new EventVariablesMapContext(event, eventBuilder));
    bindings.put(BINDING_SESSION_VARS, new SessionVariableMapContext(event.getSession()));
    bindings.put(FLOW, location.getRootContainerName());

    populatePropertyBindings(bindings, event, location);
    populateDefaultBindings(bindings);

    // TODO MULE-9690 Remove this binding. An object with this key would be available from the registry when the MuleClient is
    // moved to compatibility.
    bindings.put(BINDING_MULE_CLIENT, muleContext.getClient());
  }

  public Object runScript(Bindings bindings) throws ScriptException {
    Object result;
    try {
      RegistryLookupBindings registryLookupBindings = new RegistryLookupBindings(muleContext.getRegistry(), bindings);
      if (compiledScript != null) {
        result = compiledScript.eval(registryLookupBindings);
      } else {
        result = scriptEngine.eval(scriptText, registryLookupBindings);
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

  // //////////////////////////////////////////////////////////////////////////////
  // Getters and setters
  // //////////////////////////////////////////////////////////////////////////////

  public String getScriptText() {
    return scriptText;
  }

  public void setScriptText(String scriptText) {
    this.scriptText = scriptText;
  }

  public String getScriptFile() {
    return scriptFile;
  }

  public void setScriptFile(String scriptFile) {
    this.scriptFile = scriptFile;
  }

  public void setScriptEngineName(String scriptEngineName) {
    this.scriptEngineName = scriptEngineName;
  }

  public String getScriptEngineName() {
    return scriptEngineName;
  }

  public List<ScriptingProperty> getProperties() {
    return properties;
  }

  public void setProperties(List<ScriptingProperty> properties) {
    this.properties = properties;
  }

  public ScriptEngine getScriptEngine() {
    return scriptEngine;
  }

  protected void setScriptEngine(ScriptEngine scriptEngine) {
    this.scriptEngine = scriptEngine;
  }

  protected CompiledScript getCompiledScript() {
    return compiledScript;
  }

  protected void setCompiledScript(CompiledScript compiledScript) {
    this.compiledScript = compiledScript;
  }

}
