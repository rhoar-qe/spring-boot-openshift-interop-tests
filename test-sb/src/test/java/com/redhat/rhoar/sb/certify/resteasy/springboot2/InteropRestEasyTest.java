package com.redhat.rhoar.sb.certify.resteasy.springboot2;

import com.redhat.rhoar.sb.util.annotation.InteropTesting;
import com.redhat.rhoar.sb.util.build.SpringBoot2Build;
import com.redhat.rhoar.sb.util.deployment.MsaDeploymentBuilder;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;

@InteropTesting
public class InteropRestEasyTest extends AbstractRestEasyTest {

    @BeforeAll
    public static void setupApplication() throws IOException {
        MsaDeploymentBuilder.withApp(APP_NAME, SpringBoot2Build.RESTEASY_INTEROP.getManagedBuild())
                .generateMirrorSettings(true)
                .urlCheck("/")
                .build()
                .deploy()
                .waitFor();
        appUrl = "http://" + openshift.generateHostname(APP_NAME);
    }
}
