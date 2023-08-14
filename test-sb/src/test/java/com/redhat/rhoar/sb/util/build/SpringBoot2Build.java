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
public enum SpringBoot2Build implements BuildDefinition {
  RESTEASY_INTEROP(new SpringBoot2BuildDefinition("resteasy","resteasy",Collections.singletonMap("MAVEN_ARGS", "-Pinterop -P !rhoarqe-bom -DskipTests -Dfabric8.skip=true package")));

  @Getter
  private final SpringBoot2BuildDefinition buildDefinition;

  @Override
  public BinaryBuild getManagedBuild() {
    return buildDefinition.getManagedBuild();
  }

  public boolean getForceRebuild() {
    return buildDefinition.getForceRebuild();
  }
}
