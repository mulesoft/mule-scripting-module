package org.mule.plugin.scripting.operation;

import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.OperationExecutorModelProperty;

public class ScriptingOperationEnricher implements DeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    extensionLoadingContext.getExtensionDeclarer().getDeclaration().getOperations().forEach(operation -> {
      if (operation.getName().equals("execute")) {
        operation.addModelProperty(new OperationExecutorModelProperty(new ScriptingOperationExecutorFactory()));
      }
    });
  }
}
