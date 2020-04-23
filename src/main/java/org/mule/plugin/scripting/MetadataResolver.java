/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;


public class MetadataResolver implements OutputTypeResolver<String> {

  @Override
  public MetadataType getOutputType(MetadataContext context, String s)
      throws MetadataResolvingException, ConnectionException {

    return null;
  }

  @Override
  public String getCategoryName() {
    return "scriptCategory";
  }
}
