package org.useware.kernel.model.mapping;

/**
* @author Heiko Braun
* @date 3/19/13
*/
public interface NodePredicate<T> {
    boolean appliesTo(Node<T> node);
}
