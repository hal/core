package org.jboss.as.console.client.widgets.nav.v3;

/**
 * @author Heiko Braun
 * @since 19/01/15
 */
public class MenuDelegate<T> {

    private String title;
    private ContextualCommand<T> command;
    private String[] operationContext;
    private String resource;
    private String op;

    public MenuDelegate(String title, ContextualCommand<T> command) {
        this.title = title;
        this.command = command;
    }

    public String getTitle() {
        return title;
    }

    public ContextualCommand<T> getCommand() {
        return command;
    }

    public MenuDelegate<T> setOperationContext(String resource, String op) {
        this.operationContext = new String[] {resource, op};
        return this;
    }


    public void setOperationAddress(String resource, String op) {
        this.resource = resource;
        this.op = op;
    }

    public String[] getOperationAddress() {
        return new String[] {resource, op};
    }

    public boolean hasOperationAddress() {
        return resource!=null && op!=null;
    }

}
