package com.redhat.rhoar.sb.util.build;

import com.redhat.rhoar.sb.TestConfig;
import com.redhat.rhoar.sb.TestParent;
import cz.xtf.core.bm.BinaryBuild;
import cz.xtf.core.bm.BinarySourceBuild;
import cz.xtf.junit5.interfaces.BuildDefinition;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Radek Koubsky (rkoubsky@redhat.com)
 */
public class SpringBoot2BuildDefinition implements BuildDefinition {
  private final static String MODULE_NAME = "springboot2";

  private String buildName;
  private Path path;
  private Map<String, String> envProperties;

  private BinarySourceBuild managedBuild = null;
  private boolean forceRebuild = false;

  public SpringBoot2BuildDefinition(String appName) {
    init(appName, TestParent.findApplicationDirectory(appName, MODULE_NAME), null);
  }

  public SpringBoot2BuildDefinition(String appName, boolean forceRebuild) {
    this.forceRebuild = forceRebuild;
    init(appName, TestParent.findApplicationDirectory(appName, MODULE_NAME), null);
  }

  public SpringBoot2BuildDefinition(String appName, String buildName, Map<String, String> envProperties) {
    init(buildName, TestParent.findApplicationDirectory(appName, MODULE_NAME), envProperties);
  }

  private void init(String buildName, Path path, Map<String, String> envProperties) {
    this.buildName = buildName;
    this.path = path;
    this.envProperties = envProperties;
  }

  public Map<String, String> getEnvProperties() {
    return envProperties;
  }

  @Override
  public BinaryBuild getManagedBuild() {
    if (forceRebuild || managedBuild == null) {
      try {
        Path preparedSources = TestParent.prepareProjectSources(buildName, path);

        Map<String, String> properties = new HashMap<>();

        if (envProperties != null) {
          properties.putAll(envProperties);
        }

        if (TestConfig.isMavenProxyEnabled()) {
          properties.put("MAVEN_MIRROR_URL", TestConfig.mavenProxyUrl());
        }

        managedBuild = new BinarySourceBuild(TestConfig.imageUrl(), preparedSources, properties, buildName);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return managedBuild;
  }

  public boolean getForceRebuild() {
    return this.forceRebuild;
  }
}
