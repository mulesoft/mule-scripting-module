/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.plugin.scripting;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.MavenClientProvider;
import org.mule.maven.client.api.model.BundleDependency;
import org.mule.maven.client.api.model.BundleDescriptor;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.plugin.scripting.listeners.ScriptingArtifactLifecycleListener;
import org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import javax.swing.text.html.Option;
import java.io.File;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.Class.forName;
import static java.lang.System.gc;
import static java.lang.System.out;
import static java.lang.Thread.currentThread;
import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.lang3.JavaVersion.JAVA_17;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;
import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;

@RunWith(Parameterized.class)
public class ScriptingArtifactLifecycleListenerTest {

  private static final int PROBER_POLLING_INTERVAL = 150;
  private static final int PROBER_POLLING_TIMEOUT = 6000;
  private static final String GROOVY_ARTIFACT_ID = "groovy";
  private static final String GROOVY_GROUP_ID = "org.codehaus.groovy";
  private static final String GROOVY_SCRIPT_ENGINE = "groovy.util.GroovyScriptEngine";
  private static final String GROOVY_LANG_BINDING = "groovy.lang.Binding";

  private final String groovyVersion;
  private final ClassLoaderLookupPolicy testLookupPolicy;
  private MuleArtifactClassLoader artifactClassLoader = null;
  private final ScriptingArtifactLifecycleListener listener;
  private ArtifactDisposalContext artifactDisposalContext;

  public ScriptingArtifactLifecycleListenerTest(String groovyVersion) {
    this.groovyVersion = groovyVersion;
    this.testLookupPolicy = new ClassLoaderLookupPolicy() {

      @Override
      public LookupStrategy getClassLookupStrategy(String className) {
        return ChildFirstLookupStrategy.CHILD_FIRST;
      }

      @Override
      public LookupStrategy getPackageLookupStrategy(String packageName) {
        return ChildFirstLookupStrategy.CHILD_FIRST;
      }

      @Override
      public ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies) {
        return null;
      }
    };
    listener = new ScriptingArtifactLifecycleListener();
  }

  @Parameterized.Parameters(name = "Testing artifact {0}")
  public static String[] data() throws NoSuchFieldException, IllegalAccessException {
    return new String[] {
        "2.4.21",
        "2.5.22",
        "3.0.19"
    };
  }

  @Before
  public void setup() {
    assumeThat("When running on Java 17, the resource releaser logic from the Mule Runtime will not be used. " +
        "The resource releasing responsibility will be delegated to each connector instead.",
               isJavaVersionAtLeast(JAVA_17), is(false));

    artifactClassLoader =
        new MuleArtifactClassLoader("ScriptingArtifactLifecycleListenerTest",
                                    mock(ArtifactDescriptor.class),
                                    new URL[] {getDependencyFromMaven(GROOVY_GROUP_ID, GROOVY_ARTIFACT_ID, groovyVersion)},
                                    currentThread().getContextClassLoader(),
                                    testLookupPolicy);
    artifactDisposalContext = new ArtifactDisposalContext() {

      @Override
      public ClassLoader getExtensionClassLoader() {
        return currentThread().getContextClassLoader();
      }

      @Override
      public ClassLoader getArtifactClassLoader() {
        return artifactClassLoader;
      }

      @Override
      public boolean isExtensionOwnedClassLoader(ClassLoader classLoader) {
        return false;
      }

      @Override
      public boolean isArtifactOwnedClassLoader(ClassLoader classLoader) {
        return false;
      }

      @Override
      public Stream<Thread> getExtensionOwnedThreads() {
        return null;
      }

      @Override
      public Stream<Thread> getArtifactOwnedThreads() {
        return null;
      }

      @Override
      public boolean isArtifactOwnedThread(Thread thread) {
        return false;
      }

      @Override
      public boolean isExtensionOwnedThread(Thread thread) {
        return false;
      }
    };
  }

  private URL getDependencyFromMaven(String groupId, String artifactId, String version) {
    URL settingsUrl = getClass().getClassLoader().getResource("custom-settings.xml");
    final MavenClientProvider mavenClientProvider = discoverProvider(this.getClass().getClassLoader());

    final Supplier<File> localMavenRepository =
        mavenClientProvider.getLocalRepositorySuppliers().environmentMavenRepositorySupplier();

    final MavenConfiguration.MavenConfigurationBuilder mavenConfigurationBuilder =
        newMavenConfigurationBuilder().globalSettingsLocation(toFile(settingsUrl));

    MavenClient mavenClient = mavenClientProvider
        .createMavenClient(mavenConfigurationBuilder.localMavenRepositoryLocation(localMavenRepository.get()).build());

    try {
      BundleDescriptor bundleDescriptor = new BundleDescriptor.Builder().setGroupId(groupId)
          .setArtifactId(artifactId).setVersion(version).build();

      BundleDependency dependency = mavenClient.resolveBundleDescriptor(bundleDescriptor);

      return dependency.getBundleUri().toURL();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  @Test
  public void runGroovyScriptAndDispose() throws ReflectiveOperationException {
    assertEquals("TEST", runScript());
    listener.onArtifactDisposal(artifactDisposalContext);
    artifactClassLoader.dispose();
    assertClassLoaderIsEnqueued();
  }

  private String runScript() throws ReflectiveOperationException {
    URL[] roots = new URL[] {artifactClassLoader.getResource("groovy/example.groovy")};
    Class<?> groovyScriptEngineClass = forName(GROOVY_SCRIPT_ENGINE, true, artifactClassLoader);
    Object scriptEngine =
        groovyScriptEngineClass.getConstructor(URL[].class, ClassLoader.class).newInstance(roots, artifactClassLoader);
    Class<?> groovyBinding = forName(GROOVY_LANG_BINDING, true, artifactClassLoader);
    Method runMethod = groovyScriptEngineClass.getMethod("run", String.class, groovyBinding);
    String scriptBody = "example.groovy";
    return (String) runMethod.invoke(scriptEngine, scriptBody, groovyBinding.getConstructor().newInstance());
  }

  private void assertClassLoaderIsEnqueued() {
    PhantomReference<ClassLoader> artifactClassLoaderRef = new PhantomReference<>(artifactClassLoader, new ReferenceQueue<>());
    artifactClassLoader = null;
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      gc();
      assertThat(artifactClassLoaderRef.isEnqueued(), is(true));
      return true;
    }));
  }
}
