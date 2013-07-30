package org.jboss.as.console.client;

/**
 * @author Heiko Braun
 * @date 9/24/12
 */
public interface ProductConfig {

    public enum Profile {JBOSS, EAP}

    String getCoreVersion();
    String getDevHost();
    Profile getProfile();
}
