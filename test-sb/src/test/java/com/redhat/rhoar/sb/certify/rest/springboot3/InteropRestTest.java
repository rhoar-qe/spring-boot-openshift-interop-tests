package com.redhat.rhoar.sb.certify.resteasy.springboot3;

import com.redhat.rhoar.sb.TestParent;
import com.redhat.rhoar.sb.util.annotation.InteropTesting;
import com.redhat.rhoar.sb.util.build.SpringBoot3Build;
import com.redhat.rhoar.sb.util.deployment.MsaDeploymentBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@InteropTesting
public class InteropRestTest extends TestParent {
  protected static final String APP_NAME = "rest-sample-app";
  protected static String appUrl;

  @BeforeAll
  public static void setupApplication() throws IOException {
      MsaDeploymentBuilder.withApp(APP_NAME, SpringBoot3Build.REST_INTEROP.getManagedBuild())
              .generateMirrorSettings(true)
              .urlCheck("/")
              .build()
              .deploy()
              .waitFor();
      appUrl = "http://" + openshift.generateHostname(APP_NAME);
  }

  @Test
  public void helloWorldTest() throws Exception {
    String response = httpGetResponse(appUrl);
    Assertions.assertThat(response).isEqualTo("Hello world!");
  }
}
