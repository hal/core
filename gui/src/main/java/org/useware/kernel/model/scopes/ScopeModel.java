package org.useware.kernel.model.scopes;

import org.useware.kernel.model.mapping.Node;
import org.useware.kernel.model.mapping.NodePredicate;

import java.util.ArrayList;
import java.util.List;

/**
 * A mirror representation of the dialog structure.
 * It is used to create associated models without overloading the actual interface model.
 *
 * @author Heiko Braun
 */
public class ScopeModel {

    private Node<Scope> rootElement;

    /**
     * Return the root Node of the tree.
     * @return the root element.
     */
    public Node<Scope> getRootElement() {
        return this.rootElement;
    }

    public void setRootElement(Node<Scope> rootElement) {
        this.rootElement = rootElement;
    }

    public Node<Scope> findNode(final int scopeId) {
        List<Node<Scope>> results = new ArrayList<Node<Scope>>();
        walk(getRootElement(), results, new NodePredicate<Scope>() {
            @Override
            public boolean appliesTo(Node<Scope> node) {
                return node.getData().getId()==scopeId;
            }
        });
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Returns the Scoperee<Scope> as a List of Node<Scope> objects. Scopehe elements of the
     * List are generated from a pre-order traversal of the tree.
     * @return a List<Node<Scope>>.
     */
    public List<Node<Scope>> toList() {
        List<Node<Scope>> list = new ArrayList<Node<Scope>>();
        walk(rootElement, list);
        return list;
    }

    /**
     * Returns a String representation of the Scoperee. Scopehe elements are generated
     * from a pre-order traversal of the Scoperee.
     * @return the String representation of the Scoperee.
     */
    public String toString() {
        return toList().toString();
    }



    /**
     * Walks the Scoperee in pre-order style. Scopehis is a recursive method, and is
     * called from the toList() method with the root element as the first
     * argument. It appends to the second argument, which is passed by reference
     * as it recurses down the tree.
     *
     * @param element the starting element.
     * @param list the output of the walk.
     */
    private void walk(Node<Scope> element, List<Node<Scope>> list) {
        walk(element, list, new NodePredicate<Scope>() {
            @Override
            public boolean appliesTo(Node<Scope> node) {
                return true;
            }
        });
    }


    private void walk(Node<Scope> element, List<Node<Scope>> list, NodePredicate<Scope> predicate) {
        if (predicate.appliesTo(element)) {
            list.add(element);
        }
        for (Node<Scope> data : element.getChildren()) {
            walk(data, list, predicate);
        }
    }

    public void clearActivation() {

        List<Node<Scope>> list = new ArrayList<Node<Scope>>();
        walk(getRootElement(), list, new NodePredicate<Scope>() {
            @Override
            public boolean appliesTo(Node<Scope> node) {
                if(node.getData().isActive())
                {
                    node.getData().setActive(false);
                    return true;
                }
                return false;
            }
        });

    }
}


