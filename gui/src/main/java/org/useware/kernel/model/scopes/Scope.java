package org.useware.kernel.model.scopes;

/**
 * @author Heiko Braun
 * @date 4/17/13
 */
public final class Scope {

    private int scopeId;
    private boolean active;

    public Scope(int scopeId) {
        this.scopeId = scopeId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getId() {
        return scopeId;
    }

    @Override
    public String toString() {
        return "Scope{" +
                "id=" + scopeId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Scope scope = (Scope) o;

        if (scopeId != scope.scopeId) return false;

        return true;
    }
}
