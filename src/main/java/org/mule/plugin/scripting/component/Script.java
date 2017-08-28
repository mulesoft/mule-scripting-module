/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.component;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

public class Script {

  public Script(ExecutionContext<OperationModel> context) {
    if (context.hasParameter("text")) {
      setText(context.getParameter("text"));
    }
    if (context.hasParameter("file")) {
      setFile(context.getParameter("file"));
    }
    if (context.hasParameter("engine")) {
      setEngine(context.getParameter("engine"));
    }
    if (context.hasParameter("parameters")) {
      setParameters(context.getParameter("parameters"));
    } else {
      setParameters(new HashMap<>());
    }
  }

  /** The actual body of the script */
  private String text;

  /** A file from which the script will be loaded */
  private String file;

  /** Parameters to be made available to the script as variables */
  private Map<String, Object> parameters;

  /** The name of the JSR 223 scripting engine (e.g., "groovy") */
  private String engine;

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public void setEngine(String engine) {
    this.engine = engine;
  }

  public String getEngine() {
    return engine;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }
}
