package org.jboss.as.console.client.widgets;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Harald Pehl
 * @date 08/13/2013
 */
public class Code extends Widget {

    private final PreElement element;

    /**
     * Autodetect language w/o linenumbers
     */
    public Code() {
        this(null, false);
    }

    public Code(final boolean lineNumbers) {
        this(null, lineNumbers);
    }

    public Code(final Language language, boolean lineNumbers) {
        element = Document.get().createPreElement();
        setElement(element);
        // Add styles *after* setElement()
        addStyleName("prettyprint");
        if (language != null) {
            addStyleName(language.code);
        }
        if (lineNumbers) {
            addStyleName("linenums");
        }
    }

    public void clear() {
        element.removeClassName("prettyprinted");
        element.setInnerText("");
    }

    public void setValue(final SafeHtml value) {
        element.removeClassName("prettyprinted");
        element.setInnerSafeHtml(value);
        prettyPrint();
    }

    private native void prettyPrint() /*-{
        return $wnd.prettyPrint();
    }-*/;

    public enum Language {
        JAVASCRIPT("lang-js");
        final String code;

        Language(final String code) {
            this.code = code;
        }
    }
}
