/*
 * Copyright © MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.plugin.scripting;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

@ArtifactClassLoaderRunnerConfig(applicationSharedRuntimeLibs = {"org.mule.tests:mule-tests-model",
    "org.codehaus.groovy:groovy-all"},
    testRunnerExportedRuntimeLibs = {"org.mule.tests:mule-tests-functional"})
public abstract class AbstractScriptingFunctionalTestCase extends MuleArtifactFunctionalTestCase {

}
