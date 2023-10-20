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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import static java.lang.reflect.Modifier.isStatic;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_11;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_8;
import static org.slf4j.LoggerFactory.getLogger;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;

public class ScriptingArtifactLifecycleListener implements ArtifactLifecycleListener {


  private static final Logger LOGGER = getLogger(ScriptingArtifactLifecycleListener.class);
  private static final String GROOVY_CLASS_INFO = "org.codehaus.groovy.reflection.ClassInfo";
  private static final String GROOVY_INVOKER_HELPER = "org.codehaus.groovy.runtime.InvokerHelper";
  private static final String LOGGER_ABSTRACT_MANAGER = "org.apache.logging.log4j.core.appender.AbstractManager";
  private static final String LOGGER_STREAM_MANAGER = "org.apache.logging.log4j.core.appender.OutputStreamManager";
  private static final String GROOVY_SCRIPT_ENGINE_FACTORY = "org.codehaus.groovy.jsr223.GroovyScriptEngineFactory";
  private static final String LOGGER_CONFIGURATION = "org.apache.logging.log4j.core.config.Configuration";

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext artifactDisposalContext) {
    LOGGER.info("Running onArtifactDisposal method on " + getClass().getName());
    ClassLoader classLoader = artifactDisposalContext.getArtifactClassLoader();
    unregisterInvokerHelper(classLoader);
    final String jvmVersion = System.getProperty("java.version");
    LOGGER.info(jvmVersion);
    if (jvmVersion.startsWith(JAVA_8.version())
        || jvmVersion.startsWith(JAVA_11.version())) {
      cleanSpisEngines(classLoader);
    }
  }

  private void unregisterInvokerHelper(ClassLoader classLoader) {
    LOGGER.info("unregisterInvokerHelper method");
    try {
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

  private void cleanSpisEngines(ClassLoader classLoader) {
    LOGGER.info("cleanSpisEngines method");
    try {
      Class<?> abstractManager = loadClass(LOGGER_ABSTRACT_MANAGER, classLoader);
      HashMap hashMap = getStaticFieldValue(abstractManager, "MAP", true);
      Iterator it = hashMap.values().iterator();
      Class<?> streamManagerClass = loadClass(LOGGER_STREAM_MANAGER, classLoader);
      Object rfmInstance = null;
      while (it.hasNext()) {
        Object o = it.next();
        if (streamManagerClass.isInstance(o)) {
          rfmInstance = o;
          Object layout = getFieldValue(rfmInstance, "layout", true);
          Object configuration = getFieldValue(layout, "configuration", true);

          Class<?> configurationClass = loadClass(LOGGER_CONFIGURATION, classLoader);
          Method getScriptManagerMethod = configurationClass.getMethod("getScriptManager");
          Object scriptManager = getScriptManagerMethod.invoke(configuration);
          if (scriptManager != null) {
            Object manager = getFieldValue(scriptManager, "manager", true);
            Iterable engineSpis = getFieldValue(manager, "engineSpis", true);
            Class groovy = loadClass(GROOVY_SCRIPT_ENGINE_FACTORY, classLoader);
            Iterator engineSpisIterator = engineSpis.iterator();
            while (engineSpisIterator.hasNext()) {
              Object i = engineSpisIterator.next();
              if (groovy.isInstance(i) && i.getClass().getClassLoader().equals(groovy.getClassLoader())) {
                LOGGER.info("Removing Groovy factory from ScriptEngineManager SPIs set");
                engineSpisIterator.remove();
              }
            }
          }
        }
      }
    } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException
        | IllegalAccessException e) {
      LOGGER.warn("Error trying to unregister the Groovy's Scripting Engine", e);
    }
  }

  public static <T> T getStaticFieldValue(Class<?> targetClass, String fieldName, boolean recursive)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = getField(targetClass, fieldName, recursive);
    boolean isAccessible = field.isAccessible();
    if (!isStatic(field.getModifiers())) {
      throw new IllegalAccessException(String.format("The %s field of %s class is not static", fieldName, targetClass.getName()));
    }
    try {
      field.setAccessible(true);
      return (T) field.get(null);
    } finally {
      field.setAccessible(isAccessible);
    }
  }

  public static Field getField(Class<?> targetClass, String fieldName, boolean recursive)
      throws NoSuchFieldException {
    Class<?> clazz = targetClass;
    Field field;
    while (!Object.class.equals(clazz)) {
      try {
        field = clazz.getDeclaredField(fieldName);
        return field;
      } catch (NoSuchFieldException e) {
        // ignore and look in superclass
        if (recursive) {
          clazz = clazz.getSuperclass();
        } else {
          break;
        }
      }
    }
    throw new NoSuchFieldException(String.format("Could not find field '%s' in class %s", fieldName,
                                                 targetClass.getName()));
  }

  public static <T> T getFieldValue(Object target, String fieldName, boolean recursive)

      throws IllegalAccessException, NoSuchFieldException {

    Field f = getField(target.getClass(), fieldName, recursive);
    boolean isAccessible = f.isAccessible();
    try {
      f.setAccessible(true);
      return (T) f.get(target);
    } finally {
      f.setAccessible(isAccessible);
    }
  }


}
