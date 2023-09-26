/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.listeners;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import static org.slf4j.LoggerFactory.getLogger;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;

public class ScriptingArtifactLifecycleListener implements ArtifactLifecycleListener {


  private static final Logger LOGGER = getLogger(ScriptingArtifactLifecycleListener.class);
  private static final String GROOVY_CLASS_INFO = "org.codehaus.groovy.reflection.ClassInfo";
  private static final String GROOVY_INVOKER_HELPER = "org.codehaus.groovy.runtime.InvokerHelper";
  private static final String LOGGER_ABSTRACT_MANAGER = "org.apache.logging.log4j.core.appender.AbstractManager";
  private static final String LOGGER_STREAM_MANAGER = "org.apache.logging.log4j.core.appender.OutputStreamManager";
  private static final String GROOVY_SCRIPT_ENGINE_FACTORY = "org.codehaus.groovy.jsr223.GroovyScriptEngineFactory";

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {
    LOGGER.info("Running onArtifactDisposal method");
    unregisterInvokerHelper(artifactDisposalContext);
    // cleanSpisEngines(artifactDisposalContext);
  }

  private void unregisterInvokerHelper(ArtifactDisposalContext artifactDisposalContext) {
    LOGGER.info("unregisterInvokerHelper start");
    try {
      ClassLoader classLoader = artifactDisposalContext.getExtensionClassLoader();
      Class classInfoClass = classLoader.loadClass(GROOVY_CLASS_INFO);
      Method getAllClassInfoMethod = classInfoClass.getMethod("getAllClassInfo");
      Method getTheClassMethod = classInfoClass.getMethod("getTheClass");
      Class invokerHelperClass = classLoader.loadClass(GROOVY_INVOKER_HELPER);
      Method removeClassMethod = invokerHelperClass.getMethod("removeClass", Class.class);
      Object classes = getAllClassInfoMethod.invoke(null);
      if (classes != null && classes instanceof Collection) {
        for (Object classInfo : ((Collection) classes)) {
          Object clazz = null;
          try {
            clazz = getTheClassMethod.invoke(classInfo);
            removeClassMethod.invoke(null, clazz);
          } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
            String className = clazz instanceof Class ? ((Class) clazz).getName() : "Unknown";
            LOGGER.warn("Could not remove the {} class from the Groovy's InvokerHelper", className, e);
          }
        }
      }
    } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException | NoSuchMethodException e) {
      LOGGER.warn("Error trying to remove the Groovy's InvokerHelper classes", e);
    }
  }

  //    private void cleanSpisEngines(ArtifactDisposalContext artifactDisposalContext) {
  //        try {
  //            ClassLoader classLoader = artifactDisposalContext.getExtensionClassLoader();
  //            Class<?> abstractManager = loadClass(LOGGER_ABSTRACT_MANAGER, classLoader);
  //            HashMap hashMap = getStaticFieldValue(abstractManager, "MAP", true);
  //            Iterator it = hashMap.values().iterator();
  //            Class<?> streamManagerClass = loadClass(LOGGER_STREAM_MANAGER, classLoader);
  //            Object rfmInstance = null;
  //            while (it.hasNext()) {
  //                Object o = it.next();
  //                if (streamManagerClass.isInstance(o)) {
  //                    rfmInstance = o;
  //                    Object layout = getFieldValue(rfmInstance, "layout", true);
  //                    Object configuration = getFieldValue(layout, "configuration", true);
  //
  //                    Class<?> configurationClass = loadClass(LOGGER_CONFIGURATION, classLoader);
  //                    Method getScriptManagerMethod = configurationClass.getMethod("getScriptManager");
  //                    Object scriptManager = getScriptManagerMethod.invoke(configuration);
  //                    if (scriptManager != null) {
  //                        Object manager = getFieldValue(scriptManager, "manager", true);
  //                        HashSet engineSpis = getFieldValue(manager, "engineSpis", true);
  //                        Class groovy = loadClass(GROOVY_SCRIPT_ENGINE_FACTORY, classLoader);
  //                        Iterator engineSpisIterator = engineSpis.iterator();
  //                        while (engineSpisIterator.hasNext()) {
  //                            Object i = engineSpisIterator.next();
  //                            if (groovy.isInstance(i) && i.getClass().getClassLoader().equals(groovy.getClassLoader())) {
  //                                engineSpis.remove(i);
  //                            }
  //                        }
  //                    }
  //                }
  //            }
  //        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException
  //                 | IllegalAccessException e) {
  //            LOGGER.warn("Error trying to unregister the Groovy's Scripting Engine", e);
  //        }
  //    }
}
