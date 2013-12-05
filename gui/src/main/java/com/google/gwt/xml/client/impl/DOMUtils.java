package com.google.gwt.xml.client.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;

/**
 * @author Heiko Braun
 * @date 10/17/13
 */
public class DOMUtils {

    public static JavaScriptObject getJSObj(Node node)
    {
        return ((DOMItem)node).getJsObject();
    }

    public static Element createElementNS(Document doc, String ns, String name)
    {
        return (Element)NodeImpl.build(_createElementNS(getJSObj(doc), ns, name));
    }

    static native JavaScriptObject _createElementNS(
            JavaScriptObject jsObject,String ns,
            String tagName) /*-{
        return jsObject.createElementNS(ns, tagName);
    }-*/;

    public static Node getFirstChildElement(Node parent) {
        NodeList children = parent.getChildNodes();

        for(int i=0; i<children.getLength(); i++)
        {
            Node child = children.item(i);
            if(child.getNodeType() == Node.ELEMENT_NODE)
                return child;
        }

        return null;
    }

}
