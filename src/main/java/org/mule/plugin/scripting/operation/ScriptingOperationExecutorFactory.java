package org.mule.plugin.scripting.operation;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutorFactory;

/**
 * Custom factory that creates our operation executor.
 *
 * @since 1.0
 */
public class ScriptingOperationExecutorFactory implements ComponentExecutorFactory<OperationModel> {

  @Override
  public ComponentExecutor createExecutor(OperationModel operationModel) {
    return new ScriptingOperationExecutor();
  }
}
