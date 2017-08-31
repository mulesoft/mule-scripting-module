package org.mule.plugin.scripting.operation;

import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutorFactory;
import org.mule.runtime.module.extension.api.loader.java.property.OperationExecutorModelProperty;

/**
 * Sets a custom {@link OperationExecutorFactory} for the "execute" operation, allowing to use it through privileged access.
 *
 * @since 1.0
 */
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
