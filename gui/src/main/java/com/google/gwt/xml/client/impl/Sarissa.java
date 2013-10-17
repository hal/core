package com.google.gwt.xml.client.impl;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;

import com.google.gwt.user.client.Element;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.impl.NodeImpl;

public class Sarissa {
    protected static SarissaException IMPL_EXCEPTION = new SarissaException(
            "XML implementations other than " +
                    "com.google.gwt.xml.client.impl are not supported");

    public static Document parse(String xmlString) throws SarissaException {
        try {
            return (Document)NodeImpl.build(_parse(xmlString));
        } catch (JavaScriptException e) {
            throw new SarissaException(e);
        }
    }

    public static String serialize(Node node) throws SarissaException {
        return _serialize(getDomObj(node));
    }

    public static Document createDocument(String namespace, String localName) {
        return (Document)NodeImpl.build(_createDocument(namespace, localName));
    }

    public static NodeList selectNodes(Node from, String Xpath) throws SarissaException {
        try {
            return new NodeListImpl(_selectNodes(getDomObj(from), Xpath));
        } catch (JavaScriptException e) {
            throw new SarissaException(e);
        }
    }

    protected static JavaScriptObject getDomObj(Object o) throws SarissaException {
        if (o instanceof DOMItem) {
            return ((DOMItem)o).getJsObject();
        } else {
            throw IMPL_EXCEPTION;
        }
    }

    private native static void _setXpathNamespaces(JavaScriptObject domDoc, String namespaces) /*-{
        $wnd.Sarissa.setXpathNamespaces(domDoc, namespaces);
    }-*/;

    private native static JavaScriptObject _selectNodes(JavaScriptObject from, String Xpath) /*-{
        return from.selectNodes(Xpath);
    }-*/;

    private native static String _serialize(JavaScriptObject oDomDoc) /*-{
        return new $wnd.XMLSerializer().serializeToString(oDomDoc);
    }-*/;

    private native static JavaScriptObject _parse(String xmlString) /*-{
        var oDomDoc = (new $wnd.DOMParser()).parseFromString(xmlString, "text/xml");
        var err = $wnd.Sarissa.getParseErrorText(oDomDoc);

        if(err == $wnd.Sarissa.PARSED_OK){
            return oDomDoc;
        }else{
            throw new $wnd.Error(err);
        }
    }-*/;

    private native static JavaScriptObject _createDocument(String namespace, String localName) /*-{
        return new $wnd.Sarissa.getDomDocument(namespace, localName);
    }-*/;

    private static native void _updateContentFromNode(JavaScriptObject oNode, Element oTargetElement, JavaScriptObject processor) /*-{
        $wnd.Sarissa.updateContentFromNode(oNode, oTargetElement, processor);
    }-*/;

}
