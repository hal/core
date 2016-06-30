package org.jboss.as.console.client.shared.runtime.naming;

/**
 * @author <a href="mailto:stefankomartin6@gmail.com">Martin Štefanko</a>
 */
public class AttributeEntry extends JndiEntry {

    private JndiEntry parent;

    AttributeEntry(JndiEntry parent) {
        this("", "", "");
        this.parent = parent;
    }

    private AttributeEntry(String name, String uri, String dataType) {
        super(name, uri, dataType);
    }

    String getParentUri() {
        return parent.getURI();
    }

    String getParentDataType() {
        return parent.getDataType();
    }

    String getParentValue() {
        return parent.getValue();
    }
}
