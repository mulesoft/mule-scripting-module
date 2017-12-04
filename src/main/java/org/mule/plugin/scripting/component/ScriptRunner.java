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
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.BindingContextUtils.VARS;
import static org.mule.runtime.api.el.BindingContextUtils.addEventBindings;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.Binding;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.privileged.el.context.SessionVariableMapContext;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.util.CollectionUtils;
import org.mule.runtime.extension.api.exception.ModuleException;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.inject.Inject;
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
  private static final String REGISTRY = "registry";

  private String engineName;
  private String scriptBody;
  private ComponentLocation location;
  private ClassLoader classLoader;

  @Inject
  private Registry registry;

  /** A compiled version of the script, if the scripting engine supports it */
  private AtomicReference<CompiledScript> compiledScriptRef = new AtomicReference<>();

  private ScriptEngine scriptEngine;
  private ScriptEngineManager scriptEngineManager;

  public ScriptRunner(String engine, String code, ComponentLocation location, ClassLoader classLoader) {
    this.engineName = engine;
    this.scriptBody = code;
    this.location = location;
    this.classLoader = classLoader;

    initialise();
  }

  public void initialise() {
    scriptEngineManager = new ScriptEngineManager(this.getClass().getClassLoader());

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
        runInClassloader(classLoader, resultRef -> {
          try {
            CompiledScript compiled = ((Compilable) scriptEngine).compile(script);
            resultRef.set(Either.right(compiled));
          } catch (ScriptException e) {
            Exception exception = unwrapScriptingException(e);
            resultRef.set(Either.left(new ModuleException(COMPILATION, exception)));
          }
        });
      }
    } finally {
      IOUtils.closeQuietly(script);
    }
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

    bindings.put(VARS, unmodifiableMap(createResolvedMap(event)));
    bindings.put(BINDING_SESSION_VARS, new SessionVariableMapContext(((PrivilegedEvent) event).getSession()));
    bindings.put(FLOW, location.getRootContainerName());
    bindings.put(REGISTRY, registry);

    bindings.putAll(parameters);
    populateDefaultBindings(bindings);
  }

  public Object runScript(Bindings bindings) {
    Object result = runInClassloader(classLoader, resultRef -> {
      try {
        CompiledScript compiledScript = compiledScriptRef.get();
        if (compiledScript != null) {
          resultRef.set(Either.right(compiledScript.eval(bindings)));
        } else {
          resultRef.set(Either.right(scriptEngine.eval(scriptBody, bindings)));
        }
      } catch (Exception e) {
        Exception exception = unwrapScriptingException(e);
        resultRef.set(Either.left(new ModuleException(EXECUTION, exception)));
      }
    });

    // The result of the script can be returned directly or it can be set as the variable "result".
    if (result == null) {
      result = bindings.get(BINDING_RESULT);
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

  private Exception unwrapScriptingException(Exception e) {
    while (e instanceof ScriptException && e.getCause() instanceof ScriptException) {
      e = (Exception) e.getCause();
    }

    return e;
  }

  private static <T> T runInClassloader(ClassLoader classLoader,
                                        Consumer<AtomicReference<Either<RuntimeException, T>>> callable) {
    AtomicReference<Either<RuntimeException, T>> resultRef = new AtomicReference<>();

    withContextClassLoader(classLoader, () -> callable.accept(resultRef));

    Either<RuntimeException, T> result = resultRef.get();
    if (result == null) {
      return null;
    } else if (result.isRight()) {
      return result.getRight();
    } else if (result.isLeft()) {
      throw result.getLeft();
    } else {
      return null;
    }
  }
}
