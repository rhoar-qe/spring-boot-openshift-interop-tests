package com.redhat.rhoar.sb;

import cz.xtf.core.config.XTFConfig;
import cz.xtf.core.image.Image;
import cz.xtf.core.image.Product;
import cz.xtf.core.image.Products;
import org.apache.commons.lang3.StringUtils;

public class TestConfig {
    public static Product product() {
        return Products.resolve("msa");
    }

    public static Image image() {
        return product().image();
    }

    public static String imageUrl() {
        return image().getUrl();
    }

    public static boolean isOpenJDK11() {
        return image().getRepo().contains("openjdk-11");
    }

    public static boolean isMavenProxyEnabled() {
        return mavenProxyUrl() != null;
    }

    /**
     * Check if tests should run without admin priviledge available
     * @return true is there is no admin priviledge available
     */
    public static boolean noAdminModeActive(){
        return Boolean.parseBoolean(System.getProperty("noAdmin"));
    }

    public static boolean isSequenceBuildForced(){
        return Boolean.parseBoolean(XTFConfig.get("rhoar.build_sequentially"));
    }

    public static String mavenProxyUrl() {
        final String url = XTFConfig.get("rhoar.maven.proxy.url");
        if (!StringUtils.isBlank(url)) {
            return url.trim();
        }
        return null;
    }
}
