package org.jboss.as.console.client.shared.model;

import org.jboss.gwt.circuit.Action;

/**
 * @author Heiko Braun
 * @since 18/09/14
 */
public class LoadProfile implements Action {

    private String profile;

    public LoadProfile(String name) {
        this.profile = name;
    }

    public String getProfile() {
        return profile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LoadProfile that = (LoadProfile) o;

        if (!profile.equals(that.profile)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return profile.hashCode();
    }
}
