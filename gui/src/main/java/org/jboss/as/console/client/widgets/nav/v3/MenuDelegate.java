package org.jboss.as.console.client.widgets.nav.v3;

/**
 * @author Heiko Braun
 * @since 19/01/15
 */
public class MenuDelegate<T> {
    private String title;
    private ContextualCommand command;
    private String[] operationContext;

    public MenuDelegate(String title, ContextualCommand<T> command) {
        this.title = title;
        this.command = command;
    }

    public String getTitle() {
        return title;
    }

    public ContextualCommand getCommand() {
        return command;
    }

    public MenuDelegate<T> setOperationContext(String resource, String op) {
        this.operationContext = new String[] {resource, op};
        return this;
    }
}
