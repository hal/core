package org.jboss.as.console.client.plugins;

/**
 * @author Heiko Braun
 * @date 3/26/12
 */
public class SubsystemExtensionMetaData {
    private String token;
    private String name;
    private String group;
    private String key;

    private int major = 0;
    private int minor = 0;
    private int micro = 0;

    public SubsystemExtensionMetaData(String name, String token, String group, String key) {
        this.name = name;
        this.token = token;
        this.group = group;
        this.key = key;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public int getMicro() {
        return micro;
    }

    public void setMicro(int micro) {
        this.micro = micro;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public String getToken() {
        return token;
    }

    public String getKey() {
        return key;
    }
}
