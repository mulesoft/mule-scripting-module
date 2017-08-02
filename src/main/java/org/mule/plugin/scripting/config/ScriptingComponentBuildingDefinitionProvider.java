/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.config;

import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleReferenceParameter;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromTextContent;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;

import org.mule.plugin.scripting.component.ScriptProcessor;
import org.mule.plugin.scripting.component.Scriptable;
import org.mule.plugin.scripting.component.ScriptingProperty;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;

import java.util.LinkedList;
import java.util.List;

/**
 * Provider of {@link ComponentBuildingDefinition} for Scripting module components.
 *
 * @since 4.0
 */
public class ScriptingComponentBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider {

  public static final String SCRIPTING_NAMESPACE = "scripting";

  private static final String TEXT = "text";
  private static final String SCRIPT = "script";
  private static final String COMPONENT = "component";
  private static final String PROPERTY = "property";
  private static final String TRANSFORMER = "transformer";

  private static ComponentBuildingDefinition.Builder baseDefinition =
      new ComponentBuildingDefinition.Builder().withNamespace(SCRIPTING_NAMESPACE);

  @Override
  public void init() {}

  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
    List<ComponentBuildingDefinition> componentBuildingDefinitions = new LinkedList<>();

    componentBuildingDefinitions.add(baseDefinition.withIdentifier(TEXT)
        .withTypeDefinition(fromType(String.class))
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier(PROPERTY)
        .withTypeDefinition(fromType(ScriptingProperty.class))
        .withConstructorParameterDefinition(fromSimpleParameter("key").build())
        .withSetterParameterDefinition("value", fromSimpleParameter("value").build())
        .withSetterParameterDefinition("value", fromSimpleReferenceParameter("value-ref").build())
        .build());

    // TODO: MULE-11960 - scriptFile and scriptText are mutually exclusive
    componentBuildingDefinitions.add(baseDefinition.withIdentifier(SCRIPT)
        .withTypeDefinition(fromType(Scriptable.class))
        .withIgnoredConfigurationParameter("name")
        .withSetterParameterDefinition("scriptEngineName", fromSimpleParameter("engine").build())
        .withSetterParameterDefinition("scriptFile", fromSimpleParameter("file").build())
        .withSetterParameterDefinition("scriptText", fromTextContent().build())
        .withSetterParameterDefinition("scriptText", fromChildConfiguration(String.class).withIdentifier("text").build())
        .withSetterParameterDefinition("properties", fromChildCollectionConfiguration(ScriptingProperty.class).build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.withIdentifier(COMPONENT)
        .withTypeDefinition(fromType(ScriptProcessor.class))
        .withSetterParameterDefinition(SCRIPT, fromChildConfiguration(Scriptable.class).build())
        .withSetterParameterDefinition(SCRIPT, fromSimpleReferenceParameter("script-ref").build())
        .build());

    return componentBuildingDefinitions;
  }
}
