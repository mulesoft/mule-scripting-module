package org.mule.plugin.scripting.operation;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutorFactory;

/**
 * Custom factory that creates our operation executor.
 *
 * @since 1.0
 */
public class ScriptingOperationExecutorFactory implements OperationExecutorFactory {

  @Override
  public OperationExecutor createExecutor(OperationModel operationModel) {
    return new ScriptingOperationExecutor();
  }
}
