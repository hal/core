package org.jboss.as.console.client.shared.model;

/**
 * @author Heiko Braun
 * @since 11/06/15
 */
public class SubsystemReference {
    private SubsystemRecord delegate;
    private boolean isInclude;
    private String includedFrom = "";

    public SubsystemReference(SubsystemRecord delegate, boolean isInclude) {
        this.delegate = delegate;
        this.isInclude = isInclude;
    }

    public SubsystemReference(SubsystemRecord delegate, boolean isInclude, String includedFrom) {
            this.delegate = delegate;
            this.isInclude = isInclude;
            this.includedFrom = includedFrom;
        }

    public SubsystemRecord getDelegate() {
        return delegate;
    }

    public boolean isInclude() {
        return isInclude;
    }

    public String getIncludedFrom() {
        return includedFrom;
    }
}
