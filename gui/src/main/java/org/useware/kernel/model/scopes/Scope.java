package org.useware.kernel.model.scopes;

/**
 * @author Heiko Braun
 * @date 4/17/13
 */
public final class Scope {

    private int scopeId;
    private boolean demarcationType;

    public Scope(int scopeId, boolean demarcationType) {
        this.scopeId = scopeId;
        this.demarcationType = demarcationType;
    }

    public int getScopeId() {
        return scopeId;
    }

    public boolean isDemarcationType() {
        return demarcationType;
    }

    @Override
    public String toString() {
        return "Scope{" +
                "id=" + scopeId +
                ", demarcation=" + demarcationType +
                '}';
    }
}
