/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.operation;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.AnyType;
import org.mule.runtime.api.meta.model.declaration.fluent.OutputDeclaration;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutorFactory;
import org.mule.runtime.module.extension.api.loader.java.property.ComponentExecutorModelProperty;

/**
 * Sets a custom {@link ComponentExecutorFactory} for the "execute" operation, allowing to use it through privileged access.
 *
 * @since 1.0
 */
public class ScriptingOperationEnricher implements DeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    extensionLoadingContext.getExtensionDeclarer().getDeclaration().getOperations().forEach(operation -> {
      if (operation.getName().equals("execute")) {
        AnyType anyType = BaseTypeBuilder.create(JAVA).anyType().build();
        OutputDeclaration outputDeclaration = new OutputDeclaration();
        outputDeclaration.setType(anyType, false);

        operation.setOutput(outputDeclaration);
        operation.addModelProperty(new ComponentExecutorModelProperty(new ScriptingOperationExecutorFactory()));
      }
    });
  }
}
