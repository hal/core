package org.jboss.as.console.mbui.marshall;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;

/**
 * @author Heiko Braun
 * @date 10/14/13
 */
public interface ElementAdapter<T> {

    String getElementName();
    T fromXML(Node node);
    Element toXML(Document doc, T unit);
    Class<?> getType();
}
