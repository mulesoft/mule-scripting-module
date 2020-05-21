/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.component;

import static java.util.Collections.unmodifiableMap;
import static org.mule.plugin.scripting.errors.ScriptingErrors.COMPILATION;
import static org.mule.plugin.scripting.errors.ScriptingErrors.EXECUTION;
import static org.mule.plugin.scripting.errors.ScriptingErrors.UNKNOWN_ENGINE;
import static org.mule.runtime.api.el.BindingContextUtils.FLOW;
import static org.mule.runtime.api.el.BindingContextUtils.MESSAGE;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.BindingContextUtils.VARS;
import static org.mule.runtime.api.el.BindingContextUtils.addEventBindings;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.MuleProperties.COMPATIBILITY_PLUGIN_INSTALLED;
import static org.slf4j.LoggerFactory.getLogger;
import static java.lang.Thread.currentThread;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.Binding;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.privileged.el.context.SessionVariableMapContext;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.extension.api.exception.ModuleException;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.slf4j.Logger;

public class ScriptRunner {

  private static final Logger LOGGER = getLogger(ScriptRunner.class);

  private static final String BINDING_LOG = "log";
  private static final String BINDING_RESULT = "result";
  private static final String BINDING_SESSION_VARS = "sessionVars";
  private static final String REGISTRY = "registry";

  private String engineName;
  private String scriptBody;
  private ComponentLocation location;

  @Inject
  private Registry registry;

  /** A compiled version of the script, if the scripting engine supports it */
  private CompiledScript compiledScript;

  private ScriptEngine scriptEngine;
  private ScriptEngineManager scriptEngineManager;
  private boolean compatibilityMode;

  public ScriptRunner(String engine, String code, ComponentLocation location) {
    this.engineName = engine;
    this.scriptBody = code;
    this.location = location;
  }

  public void initialise() {
    scriptEngineManager = new ScriptEngineManager(currentThread().getContextClassLoader());

    scriptEngine = createScriptEngineByName(engineName);
    if (scriptEngine == null) {
      String message =
          "Scripting engine '" + engineName + "' not found.  Available engines are: " + listAvailableEngines();
      throw new ModuleException(createStaticMessage(message), UNKNOWN_ENGINE);
    }

    Reader script = new StringReader(scriptBody);
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

    compatibilityMode = registry.lookupByName(COMPATIBILITY_PLUGIN_INSTALLED).isPresent();
  }

  public void populateDefaultBindings(Bindings bindings) {
    bindings.put(BINDING_LOG, LOGGER);
    // A place holder for a returned result if the script doesn't return a result.
    // The script can overwrite this binding
    bindings.put(BINDING_RESULT, null);
  }

  public void populateBindings(Bindings bindings, CoreEvent event, Map<String, Object> parameters) {
    // TODO MULE-10121 Provide a MessageBuilder API in scripting components to improve usability

    for (Binding binding : addEventBindings(event, NULL_BINDING_CONTEXT).bindings()) {
      Object resolvedValue = resolveCursor(binding.value().getValue());
      bindings.put(binding.identifier(), resolvedValue);
    }

    if (compatibilityMode) {
      // Regular bindings will not include compatibility data so we override it
      bindings.put(MESSAGE, event.getMessage());
      bindings.put(BINDING_SESSION_VARS, new SessionVariableMapContext(((PrivilegedEvent) event).getSession()));
    }

    bindings.put(VARS, unmodifiableMap(createResolvedMap(event)));
    bindings.put(FLOW, location.getRootContainerName());
    bindings.put(REGISTRY, registry);

    bindings.putAll(parameters);
    populateDefaultBindings(bindings);
  }

  public Object runScript(Bindings bindings) {
    Object result;
    try {
      if (compiledScript != null) {
        result = compiledScript.eval(bindings);
      } else {
        result = scriptEngine.eval(scriptBody, bindings);
      }

      // The result of the script can be returned directly or it can
      // be set as the variable "result".
      if (result == null) {
        result = bindings.get(BINDING_RESULT);
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
    return scriptEngineManager.getEngineFactories().stream()
        .map(ScriptEngineFactory::getEngineName)
        .collect(Collectors.joining(", "));
  }

  public ScriptEngine getScriptEngine() {
    return scriptEngine;
  }

  private Object resolveCursor(Object value) {
    if (value instanceof CursorProvider) {
      value = ((CursorProvider) value).openCursor();
    }

    return value;
  }

  private Map<String, Object> createResolvedMap(CoreEvent event) {
    HashMap<String, Object> variables = new HashMap<>();

    event.getVariables().entrySet().forEach(e -> {
      Object value = resolveCursor(e.getValue().getValue());
      variables.put(e.getKey(), value);
    });

    return variables;
  }
}
