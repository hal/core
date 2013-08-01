package org.jboss.as.console.client;

/**
 * Instance holding product related information. An instance of this interface is generated using defered binding.
 *
 * @author Heiko Braun
 * @date 9/24/12
 */
public interface ProductConfig {

    /**
     * Whether this is the community or product version.
     *
     * @return
     */
    Profile getProfile();

    /**
     * The core console version
     *
     * @return
     */
    String getCoreVersion();

    /**
     * The version of the HAL release stream
     *
     * @return
     */
    String getConsoleVersion();

    /**
     * The product title from the management model
     *
     * @return
     */
    String getProductName();

    /**
     * The product version from the management model
     *
     * @return
     */
    String getProductVersion();

    /**
     * The hostname / ip address of the dev host (only relevant in dev mode).
     *
     * @return
     */
    String getDevHost();

    public enum Profile {COMMUNITY, PRODUCT}
}
