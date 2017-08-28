/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.component;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.Map;

/**
 * A JSR 223 Script service. Allows any JSR 223 compliant script engines such as JavaScript, Groovy or Rhino to be embedded as
 * Mule components.
 *
 * @since 1.0
 */
@TypeDsl(allowTopLevelDefinition = true)
public class Script {

  /** The actual body of the script */
  @Optional
  @Parameter()
  @Content(primary = true)
  private String text;

  /** A file from which the script will be loaded */
  @Optional
  @Parameter
  private String file;

  /** Parameters to be made available to the script as variables */
  @Optional(defaultValue = "#[{}]")
  @Parameter
  //@TypeResolver(ScriptingTypeResolver.class)
  @Content
  private Map<String, Object> parameters;

  /** The name of the JSR 223 scripting engine (e.g., "groovy") */
  @Optional
  @Parameter
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
