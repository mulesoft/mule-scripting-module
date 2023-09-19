/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting;

import org.mule.plugin.scripting.errors.ScriptingErrors;
import org.mule.plugin.scripting.listeners.ScriptingArtifactLifecycleListener;
import org.mule.plugin.scripting.operation.ScriptingOperationEnricher;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.privileged.DeclarationEnrichers;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.sdk.api.annotation.OnArtifactLifecycle;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_8;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_11;

import static org.mule.runtime.api.meta.ExternalLibraryType.JAR;

@Extension(name = "Scripting")
@Operations({ScriptingOperations.class})
@DeclarationEnrichers(ScriptingOperationEnricher.class)
@ExternalLib(name = "JSR-223 Engine",
    description = "A JSR-223 supported engine",
    nameRegexpMatcher = "(.*)\\.jar",
    type = JAR, coordinates = "org.codehaus.groovy:groovy-jsr223:3.0.19",
    optional = true)
@ErrorTypes(ScriptingErrors.class)
@JavaVersionSupport({JAVA_8, JAVA_11})
@OnArtifactLifecycle(ScriptingArtifactLifecycleListener.class)
public class ScriptingExtension {

}
