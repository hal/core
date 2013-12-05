package org.jboss.as.console.client.tools.modelling.workbench.repository;

/**
 * @author Heiko Braun
 * @date 10/6/13
 */
public class DialogRef {
    String name;

    public DialogRef(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
