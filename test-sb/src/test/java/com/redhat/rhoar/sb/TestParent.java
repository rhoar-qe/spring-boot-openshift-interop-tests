package com.redhat.rhoar.sb;

import com.redhat.rhoar.sb.util.deployment.MsaDeploymentBuilder;
import com.redhat.rhoar.sb.util.maven.PomModifier;
import cz.xtf.builder.builders.ApplicationBuilder;
import cz.xtf.client.Http;
import cz.xtf.client.HttpResponseParser;
import cz.xtf.core.openshift.OpenShift;
import cz.xtf.core.openshift.OpenShiftWaiters;
import cz.xtf.core.openshift.OpenShifts;
import cz.xtf.core.waiting.SimpleWaiter;
import cz.xtf.core.waiting.Waiter;
import cz.xtf.junit5.annotations.OpenShiftRecorder;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.DeploymentConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;

@Slf4j
@OpenShiftRecorder
public abstract class TestParent {
    protected static final OpenShift openshift = OpenShifts.master();
    protected static final String SUITE_NAME = "test-sb";
    protected static final Path TMP_SOURCES_DIR = Paths.get("tmp").toAbsolutePath().resolve("sources");
    private static final boolean SEQUENCE_BUILDS = TestConfig.isSequenceBuildForced();

    private static final List<MsaDeploymentBuilder> builds = new ArrayList<>();
    private static final List<Waiter> waiters = new ArrayList<>();

    protected static final OpenShiftWaiters openshiftWaiters = OpenShiftWaiters.get(openshift, () -> false);

    protected static String httpGetResponse(String url) throws IOException {
        return httpGet(url).response();
    }

    protected static HttpResponseParser httpGet(String url) throws IOException {
        return Http.get(url).trustAll().execute();
    }

    public static Path prepareProjectSources(final String appName, final Path projectDir) throws IOException {
        if (projectDir == null) {
            return null;
        }

        Files.createDirectories(TMP_SOURCES_DIR);
        Path sourcesDir = Files.createTempDirectory(TMP_SOURCES_DIR.toAbsolutePath(), appName);
        FileUtils.copyDirectory(projectDir.toFile(), sourcesDir.toFile());

        PomModifier.modify(projectDir, sourcesDir);

        return sourcesDir;
    }

    public static ApplicationBuilder appFromBinaryBuild(final String appName) {
        ApplicationBuilder appBuilder = new ApplicationBuilder(appName);
        appBuilder.buildConfig().setOutput(appName).sti().forcePull(true).fromDockerImage(TestConfig.imageUrl());
        appBuilder.imageStream();
        appBuilder.deploymentConfig().onImageChange().onConfigurationChange().podTemplate().container().fromImage(appName);

        appBuilder.buildConfig().withBinaryBuild();
        if (TestConfig.isMavenProxyEnabled()) {
            appBuilder.buildConfig().sti().addEnvVariable("MAVEN_MIRROR_URL", TestConfig.mavenProxyUrl());
        }

        return appBuilder;
    }

    public static Path findApplicationDirectory(String appName) {
        return findApplicationDirectory(SUITE_NAME, appName, null);
    }

    public static Path findApplicationDirectory(String appName, String appModuleName) {
        return findApplicationDirectory(SUITE_NAME, appName, appModuleName);
    }

    public static Path findApplicationDirectory(String moduleName, String appName, String appModuleName) {
        Path path = FileSystems.getDefault().getPath("src/test/resources/apps", appName);

        /* We only return this path if the absolute path contains the moduleName,
            e.g. if both  test-eap and test-common contain "foo", but we explicitly want
            the test-common/src/test/resources/apps/foo
          */
        if (Files.exists(path) && path.toAbsolutePath().toString().contains(moduleName)) {
            return path;
        }
        log.debug("Path {} does not exist", path.toAbsolutePath());
        if (appModuleName != null) {
            path = FileSystems.getDefault().getPath("src/test/resources/apps/" + appModuleName, appName);
            if (Files.exists(path) && path.toAbsolutePath().toString().contains(moduleName)) {
                return path;
            }
            log.info("Path {} does not exist", path.toAbsolutePath());
        }
        path = FileSystems.getDefault().getPath(moduleName + "/src/test/resources/apps", appName);
        if (Files.exists(path)) {
            return path;
        }
        log.info("Path {} does not exist", path.toAbsolutePath());
        if (appModuleName != null) {
            path = FileSystems.getDefault().getPath(moduleName + "/src/test/resources/apps/" + appModuleName, appName);
            if (Files.exists(path) && path.toAbsolutePath().toString().contains(moduleName)) {
                return path;
            }
            log.info("Path {} does not exist", path.toAbsolutePath());
        }
        path = FileSystems.getDefault().getPath("../" + moduleName + "/src/main/resources/apps", appName);
        if (Files.exists(path)) {
            return path;
        }
        log.info("Path {} does not exist", path.toAbsolutePath());
        path = FileSystems.getDefault().getPath("../" + moduleName + "/src/test/resources/apps", appName);
        if (Files.exists(path)) {
            return path;
        }
        log.info("Path {} does not exist", path.toAbsolutePath());
        throw new IllegalArgumentException("Cannot find directory with STI app sources");
    }

    public static Build startBinaryBuild(final String bcName, Path sources) throws IOException {
        try {
            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos);

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            final Future<?> future = executorService.submit(() -> {
                Collection<File> filesToArchive = FileUtils.listFiles(sources.toFile(), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
                try (ArchiveOutputStream o = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.TAR, pos)) {
                    for (File f : filesToArchive) {
                        String tarPath = sources.relativize(f.toPath()).toString();
                        log.trace("adding file to tar: {}", tarPath);
                        ArchiveEntry entry = o.createArchiveEntry(f, tarPath);

                        // we force the modTime in the tar, so that the resulting tars are binary equal if their contents are
                        TarArchiveEntry tarArchiveEntry = (TarArchiveEntry)entry;
                        tarArchiveEntry.setModTime(Date.from(Instant.EPOCH));

                        o.putArchiveEntry(tarArchiveEntry);
                        if (f.isFile()) {
                            try (InputStream i = Files.newInputStream(f.toPath())) {
                                IOUtils.copy(i, o);
                            }
                        }
                        o.closeArchiveEntry();
                    }

                    o.finish();
                } catch (ArchiveException | IOException e) {
                    throw new RuntimeException(e);
                }
            });

            Build ret = OpenShifts.master().buildConfigs().withName(bcName).instantiateBinary().fromInputStream(pis);
            future.get();

            return ret;
        } catch (InterruptedException | ExecutionException e) {
            log.error("IOException building {}", bcName, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Remove all resources for specific deployment from openshift project.
     * @param appName Deployment to remove
     */
    protected void removeDeployment(String appName){
        openshift.getRoutes().stream()
                .filter((route) -> route.getMetadata().getName().contains(appName))
                .forEach(openshift::deleteRoute);

        openshift.getServices().stream()
                .filter(service -> service.getMetadata().getName().contains(appName))
                .forEach(openshift::deleteService);

        // delete deployment config
        // no cascade - issue in fabric8
        openshift.getDeploymentConfigs().stream()
                .filter(deploymentConfig1 -> deploymentConfig1.getMetadata().getName().contains(appName))
                .forEach(deploymentConfig -> openshift.deleteDeploymentConfig(deploymentConfig,false));

        //delete replication controller
        openshift.replicationControllers().list().getItems().stream()
                .filter(replicationController1 -> replicationController1.getMetadata().getName().contains(appName))
                .forEach(replicationController -> openshift.replicationControllers()
                    .withName(replicationController.getMetadata().getName())
                    .withPropagationPolicy(DeletionPropagation.FOREGROUND)
                    .delete());

        //delete pod
        openshift.getPods().stream()
                .filter(pod1 -> pod1.getMetadata().getName().contains(appName))
                .forEach(openshift::deletePod);

        //delete build
        openshift.deleteBuild(openshift.getBuild(appName));
        openshift.deleteBuildConfig(openshift.getBuildConfig(appName));
    }

    /**
     * Create a clone of a deployment in openshift.
     * Will generate new deployment config, service and route.
     * @param appName Name of the deployment config to clone.
     * @param newName Name of the newly created app
     * @param checkUrlSuffix Application URL suffix, where to check if the new app is ready
     * @return Waiter - When pods of the new app are ready
     */
    protected static Waiter cloneDeployment(String appName, String newName, String checkUrlSuffix){
        DeploymentConfig deploymentConfig = openshift.getDeploymentConfig(appName);

        String image = deploymentConfig.getSpec().getTemplate().getSpec().getContainers().get(0).getImage();
        Integer replicas = deploymentConfig.getSpec().getReplicas();
        ApplicationBuilder appBuilder = ApplicationBuilder.fromImage(newName, image);

        appBuilder.deploymentConfig(newName)
                .setReplicas(replicas)
                .podTemplate()
                .container();
        appBuilder.service().port("http", 8080, 80);
        appBuilder.route();
        appBuilder.buildApplication(OpenShifts.master()).deploy();

        BooleanSupplier bs = () -> {
            openshiftWaiters.areExactlyNPodsReady(replicas,newName).waitFor();
            try {
                Http.get("http://" + openshift.generateHostname(newName) + "/" + checkUrlSuffix).trustAll().waiters().ok().waitFor();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            return true;
        };
        return new SimpleWaiter(bs, "Wait for application " + newName + " to deploy");
    }

    /**
     * Scale the app down to 0 and then to 1.
     * @param deploymentName Application to reset
     * @param deploymentUrl If not null then after reset if will wait for this URL + /actuator/health to be OK.
     */
    protected void resetDeployment(String deploymentName, String deploymentUrl) throws MalformedURLException {
        openshift.deploymentConfigs().withName(deploymentName).scale(0);
        openshiftWaiters.areNoPodsPresent(deploymentName).waitFor();

        openshift.deploymentConfigs().withName(deploymentName).scale(1);
        openshiftWaiters.areExactlyNPodsReady(1, deploymentName).waitFor();

        if (deploymentUrl != null){
            Http.get(deploymentUrl + "/actuator/health").trustAll().waiters().ok().waitFor();
        }
    }

    private static void addWaiter(Waiter waiter){
        waiters.add(waiter);
    }

    private static void waitForAll(){
        waiters.forEach(Waiter::waitFor);
    }

    /**
     * Add application to be built
     *
     * If sequential build is forced, application will immediately be built and deployed
     * In parallel builds it will start build, deployment must be handled by {@link TestParent#deployAllBuilds}
     *
     * @param builder MsaDeploymentBuild representing running build
     */
    protected static void addBuild(MsaDeploymentBuilder builder) throws IOException {
        if (SEQUENCE_BUILDS) {
            builder.build().waitForBuild().deploy().waitFor();
        } else{
            builds.add(builder.build());
        }
    }

    /**
     * Wait for all builds, build in parallel and deploy them all
     * If build sequentially do nothing
     */
    protected static void deployAllBuilds(){
        if (SEQUENCE_BUILDS){
            return;
        }

        // wait for all service etc. to be deployed
        waitForAll();

        List<Waiter> waiters = new ArrayList<>();
        // initiate deployment for all builds
        for (MsaDeploymentBuilder build : builds){
            build.waitForBuild();
            waiters.add(build.deploy());
        }
        builds.clear();
        // wait for all deployments to be finished
        waiters.forEach(Waiter::waitFor);
    }

    @BeforeAll
    public static void cleanProjectBeforeClass() {
        openshift.clean().waitFor();
        waiters.clear();
        builds.clear();
    }

    @AfterAll
    public static void cleanupAfterClass() {
        openshift.clean().waitFor();
    }
}
