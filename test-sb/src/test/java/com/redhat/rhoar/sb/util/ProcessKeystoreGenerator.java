package com.redhat.rhoar.sb.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class for generating keystores signed by one common generated ca
 */
public class ProcessKeystoreGenerator {

    private static final Path WORKING_DIRECTORY = Paths.get("tmp").toAbsolutePath().resolve("keystores");

    static {
        WORKING_DIRECTORY.toFile().mkdirs();
    }


    public static Path generateKeystore(String hostname, String keyAlias) {
        String keystore = hostname + ".keystore";

        if (WORKING_DIRECTORY.resolve(keystore).toFile().exists()) {
            return WORKING_DIRECTORY.resolve(keystore);
        }

        processCall(WORKING_DIRECTORY, "keytool", "-genkeypair", "-J-Dkeystore.pkcs12.legacy", "-keyalg", "RSA", "-noprompt", "-alias", keyAlias, "-dname", "CN=" + hostname + ", OU=TF, O=XTF, L=Brno, S=CZ, C=CZ", "-keystore", keystore, "-storepass", "password", "-keypass", "password", "-deststoretype", "pkcs12");

        return WORKING_DIRECTORY.resolve(keystore);
    }

    private static void processCall(Path cwd, String... args) {
        ProcessBuilder pb = new ProcessBuilder(args);

        pb.directory(cwd.toFile());
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        int result;

        try {
            result = pb.start().waitFor();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Failed executing " + String.join(" ", args));
        }

        if (result != 0) {
            throw new IllegalStateException("Failed executing " + String.join(" ", args));
        }
    }
}
