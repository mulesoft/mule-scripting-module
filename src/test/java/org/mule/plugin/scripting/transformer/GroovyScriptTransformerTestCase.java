/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.transformer;

import static java.util.Collections.singletonMap;
import static org.mule.runtime.api.meta.AbstractAnnotatedObject.LOCATION_KEY;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;

import org.mule.plugin.scripting.component.Scriptable;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.tck.core.transformer.AbstractTransformerTestCase;

import java.util.ArrayList;
import java.util.List;

public class GroovyScriptTransformerTestCase extends AbstractTransformerTestCase {

  @Override
  protected boolean mockExprExecutorService() {
    return true;
  }

  @Override
  public Transformer getTransformer() throws Exception {
    Scriptable script = new Scriptable(muleContext);
    script.setScriptEngineName("groovy");
    script.setScriptFile("StringToList.groovy");
    script.initialise();

    ScriptTransformer transformer = new ScriptTransformer();
    transformer.setName("StringToList");
    transformer.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent("flow")));
    transformer.setMuleContext(muleContext);
    transformer.setScript(script);
    transformer.initialise();
    return transformer;
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    Scriptable script = new Scriptable(muleContext);
    script.setScriptFile("ListToString.groovy");
    script.setScriptEngineName("groovy");
    script.initialise();

    ScriptTransformer transformer = new ScriptTransformer();
    transformer.setName("ListToString");
    transformer.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent("flow")));
    transformer.setMuleContext(muleContext);
    transformer.setScript(script);
    transformer.initialise();
    return transformer;
  }

  @Override
  public Object getTestData() {
    return "this is groovy!";
  }

  @Override
  public Object getResultData() {
    List<String> list = new ArrayList<String>(3);
    list.add("this");
    list.add("is");
    list.add("groovy!");
    return list;
  }
}
