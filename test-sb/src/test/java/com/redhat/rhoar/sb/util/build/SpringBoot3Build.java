package com.redhat.rhoar.sb.util.build;

import cz.xtf.core.bm.BinaryBuild;
import cz.xtf.junit5.interfaces.BuildDefinition;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;

/**
 * @author Radek Koubsky (rkoubsky@redhat.com)
 */
@AllArgsConstructor
public enum SpringBoot3Build implements BuildDefinition {
  REST_INTEROP(new SpringBoot3BuildDefinition("rest", "rest", Collections.singletonMap("MAVEN_ARGS", "-Pinterop package")));

  @Getter
  private final SpringBoot3BuildDefinition buildDefinition;

  @Override
  public BinaryBuild getManagedBuild() {
    return buildDefinition.getManagedBuild();
  }

  public boolean getForceRebuild() {
    return buildDefinition.getForceRebuild();
  }
}
