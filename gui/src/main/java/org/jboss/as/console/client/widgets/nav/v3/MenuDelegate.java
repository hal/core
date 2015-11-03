package org.jboss.as.console.client.widgets.nav.v3;

/**
 * @author Heiko Braun
 * @since 19/01/15
 */
public class MenuDelegate<T>{

    public enum Role {Navigation, Operation}

    private String title;
    private ContextualCommand<T> command;
    private String resource;
    private String op;
    private Role actualRole = Role.Navigation;

    public MenuDelegate(String title, ContextualCommand<T> command) {
        this.title = title;
        this.command = command;
    }

    public MenuDelegate(String title, ContextualCommand<T> command, Role role) {
        this.title = title;
        this.command = command;
        this.actualRole = role;
    }

    public String render(T data) {
        return this.title;
    }

    public Role getRole() {
        return actualRole;
    }

    public String getTitle() {
        return title;
    }

    public ContextualCommand<T> getCommand() {
        return command;
    }

    public String getResource() {
        return resource;
    }

    public String getOp() {
        return op;
    }

    public MenuDelegate<T> setOperationAddress(String resource, String op) {
        this.resource = resource;
        this.op = op;
        return this;
    }

    public boolean hasOperationAddress() {
        return resource!=null && op!=null;
    }


}
