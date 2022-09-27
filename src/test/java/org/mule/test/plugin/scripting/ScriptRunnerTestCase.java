/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.plugin.scripting;

import org.junit.Test;
import org.mule.plugin.scripting.component.ScriptRunner;
import org.mule.runtime.extension.api.exception.ModuleException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class ScriptRunnerTestCase {

  @Test
  public void listAvailableEnginesTest() {
    try {
      ScriptRunner runner = new ScriptRunner("Oracle Nashorn", "log.debug('hi there')", null);
      runner.initialise();
    } catch (ModuleException ex) {
      assertThat(ex.getMessage(), containsString("ECMAScript"));
    }
  }

  @Test
  public void ECMAScriptShouldWorkForDifferentJavaVersionsTest() {
    ScriptRunner runner = new ScriptRunner("ECMAScript", "tempPayload = \"hello\"; tempPayload", null);
    runner.initialise();
    String result = (String) runner.runScript(null);
    assertThat(result, containsString("hello"));
  }

}


