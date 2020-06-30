/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.plugin.scripting;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.event.CoreEvent;

import io.qameta.allure.Description;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class StreamingTestCase extends AbstractScriptingFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "streaming-config.xml";
  }

  @Test
  @Description("When there is a cursor provider in vars, only one cursor must be opened before execution" +
      "and closed after")
  public void assertCursorProviderMustOpenCursorAndMustCloseThisCursor() throws Exception {
    CursorStream providedCursorStream = createMockCursor();
    CursorStreamProvider provider = createMockCursorProvider(providedCursorStream);

    CoreEvent response = flowRunner("scripting-test-flow")
        .withVariable("stream", provider)
        .run();

    verify(provider, times(1)).openCursor();
    verify(providedCursorStream, times(1)).close();
  }

  @Test
  @Description("When there is a cursor in vars, it will be closed after execution")
  public void assertMastCloseOpenedCursor() throws Exception {
    CursorStream userCursorStream = createMockCursor();

    CoreEvent response = flowRunner("scripting-test-flow")
        .withVariable("cursor", userCursorStream)
        .run();

    verify(userCursorStream, times(1)).close();
  }


  @Test
  @Description("When user closes a cursor in a script, module tries to close it again")
  public void assertCloseCursorFlow() throws Exception {
    CursorStream providedCursorStream = createMockCursor();
    CursorStreamProvider provider = createMockCursorProvider(providedCursorStream);

    CoreEvent response = flowRunner("scripting-close-cursor-flow")
        .withVariable("var1", provider)
        .run();

    verify(provider, times(1)).openCursor();
    verify(providedCursorStream, times(2)).close();
  }

  @Test
  @Description("When there is a cursor wrapped in a typedValue in vars, it will be closed after execution")
  public void assertWrappedCursorIsClosed() throws Exception {
    TypedValue<CursorStream> wrappedCursor = TypedValue.of(createMockCursor());

    CoreEvent response = flowRunner("scripting-test-flow")
        .withVariable("wrapped", wrappedCursor)
        .run();

    verify(wrappedCursor.getValue(), times(1)).close();
  }

  @Test
  @Description("When a map is provided in vars and it has an inner map booth are"
      + "recursively scanned in order to close cursors even the ones that are wrapped in TypeValues")
  public void assertCursorRecursivelyClosed() throws Exception {
    CursorStream cursor = createMockCursor();
    TypedValue<CursorStream> wrappedCursor = TypedValue.of(createMockCursor());

    Map<String, Object> map = new HashMap<>();
    map.put("cursor", cursor);
    map.put("wrapped", wrappedCursor);

    Map<String, Object> innerMap = new HashMap<>();
    map.put("map", innerMap);
    innerMap.put("cursor", cursor);
    innerMap.put("wrapped", wrappedCursor);

    CoreEvent response = flowRunner("scripting-test-flow")
        .withVariable("map", map)
        .run();

    verify(cursor, times(2)).close();
    verify(wrappedCursor.getValue(), times(2)).close();
  }

  @Test
  @Description("When a wrapped map is provided in vars and it has an inner map booth are"
      + "recursively scanned in order to close cursors even the ones that are wrapped in TypeValues")
  public void assertWrappedCursorRecursivelyClosed() throws Exception {
    CursorStream cursor = createMockCursor();
    TypedValue<CursorStream> wrappedCursor = TypedValue.of(createMockCursor());

    Map<String, Object> map = new HashMap<>();
    map.put("cursor", cursor);
    map.put("wrapped", wrappedCursor);

    Map<String, Object> innerMap = new HashMap<>();
    map.put("map", innerMap);
    innerMap.put("cursor", cursor);
    innerMap.put("wrapped", wrappedCursor);

    TypedValue<Map<String, Object>> wrappedMap = TypedValue.of(map);

    CoreEvent response = flowRunner("scripting-test-flow")
        .withVariable("map", wrappedMap)
        .run();

    verify(cursor, times(2)).close();
    verify(wrappedCursor.getValue(), times(2)).close();
  }

  @Test
  @Description("When a wrapped map is provided in the payload and it has an inner map booth are"
      + "recursively scanned in order to close cursors even the ones that are wrapped in TypeValues")
  public void assertWrappedCursorAreRecursivelyClosedInPayload() throws Exception {
    CursorStream cursor = createMockCursor();
    TypedValue<CursorStream> wrappedCursor = TypedValue.of(createMockCursor());

    Map<String, Object> map = new HashMap<>();
    map.put("cursor", cursor);
    map.put("wrapped", wrappedCursor);

    Map<String, Object> innerMap = new HashMap<>();
    map.put("map", innerMap);
    innerMap.put("cursor", cursor);
    innerMap.put("wrapped", wrappedCursor);

    TypedValue<Map<String, Object>> wrappedMap = TypedValue.of(map);

    CoreEvent response = flowRunner("scripting-test-flow")
        .withPayload(wrappedMap)
        .run();

    verify(cursor, times(2)).close();
    verify(wrappedCursor.getValue(), times(2)).close();
  }

  private CursorStream createMockCursor() {
    CursorStream cursorStream = mock(CursorStream.class);
    return cursorStream;
  }

  private CursorStreamProvider createMockCursorProvider(CursorStream cursor) {
    CursorStreamProvider provider = mock(CursorStreamProvider.class);
    when(provider.openCursor()).thenReturn(cursor);

    return provider;
  }
}
