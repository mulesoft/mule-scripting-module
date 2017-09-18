/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
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

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.Binding;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.privileged.el.context.EventVariablesMapContext;
import org.mule.runtime.core.privileged.el.context.SessionVariableMapContext;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.util.CollectionUtils;
import org.mule.runtime.extension.api.exception.ModuleException;

import org.slf4j.Logger;

import java.io.Reader;
import java.io.StringReader;

import javax.inject.Inject;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class ScriptRunner {

  private static final Logger LOGGER = getLogger(ScriptRunner.class);

  private static final String BINDING_LOG = "log";
  private static final String BINDING_RESULT = "result";
  private static final String BINDING_SESSION_VARS = "sessionVars";
  private static final String REGISTRY = "registry";

  @Inject
  private Registry registry;

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
    try {
      muleContext.getInjector().inject(this);
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
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

  public void populateBindings(Bindings bindings, ComponentLocation location, CoreEvent event,
                               CoreEvent.Builder eventBuilder) {
    // TODO MULE-10121 Provide a MessageBuilder API in scripting components to improve usability
    for (Binding binding : addEventBindings(event, NULL_BINDING_CONTEXT).bindings()) {
      bindings.put(binding.identifier(), binding.value().getValue());
    }
    bindings.put(VARS, new EventVariablesMapContext(event, eventBuilder));
    bindings.put(BINDING_SESSION_VARS, new SessionVariableMapContext(((PrivilegedEvent) event).getSession()));
    bindings.put(FLOW, location.getRootContainerName());
    bindings.put(REGISTRY, registry);

    populatePropertyBindings(bindings);
    populateDefaultBindings(bindings);

  }

  public Object runScript(Bindings bindings) {
    Object result;
    try {
      if (compiledScript != null) {
        result = compiledScript.eval(bindings);
      } else {
        result = scriptEngine.eval(scriptConfig.getCode(), bindings);
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
    return CollectionUtils.toString(scriptEngineManager.getEngineFactories(), false);
  }

  public ScriptEngine getScriptEngine() {
    return scriptEngine;
  }
}
