/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting;

import org.mule.plugin.scripting.operation.ScriptingOperationEnricher;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.privileged.DeclarationEnrichers;

@Extension(name = "Scripting")
@Operations({ScriptingOperations.class})
@DeclarationEnrichers(ScriptingOperationEnricher.class)
public class ScriptingExtension implements Initialisable, Disposable {

  @Override
  public void dispose() {

  }

  @Override
  public void initialise() throws InitialisationException {

  }
}
