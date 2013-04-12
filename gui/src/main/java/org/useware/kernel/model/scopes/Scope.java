package org.useware.kernel.model.scopes;

import org.useware.kernel.model.mapping.Node;

public abstract class Scope {

    Node<Integer> node;
    Integer previousContext = null;

    protected Scope(Node<Integer> container) {
        this.node = container;
    }

    public Integer getPreviousContext() {
        return previousContext;
    }

    protected Scope(Node<Integer> container, Integer previousContext) {
        this.node = container;
        this.previousContext = previousContext;
    }

    public Node<Integer> getNode() {
        return node;
    }

    abstract Integer getContextId();
}
