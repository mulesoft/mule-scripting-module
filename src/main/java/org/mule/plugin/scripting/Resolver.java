package org.mule.plugin.scripting;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

public class Resolver implements InputTypeResolver<String>, OutputTypeResolver<String> {

  @Override
  public String getResolverName() {
    return "sadsa";
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {
    return null;
  }

  @Override
  public String getCategoryName() {
    return "sdasdjkdsajkl";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {
    return null;
  }
}
