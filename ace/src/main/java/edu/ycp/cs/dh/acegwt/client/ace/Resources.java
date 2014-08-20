package edu.ycp.cs.dh.acegwt.client.ace;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

public interface Resources extends ClientBundle {

    Resources INSTANCE =  GWT.create(Resources.class);

    @Source("edu/ycp/cs/dh/acegwt/public/ace/ace.js")
    TextResource aceJs();

    @Source("edu/ycp/cs/dh/acegwt/public/ace/ext-searchbox.js")
    TextResource extSearchBoxJs();

    @Source("edu/ycp/cs/dh/acegwt/public/ace/mode-xml.js")
    TextResource modeXmlJs();

    @Source("edu/ycp/cs/dh/acegwt/public/ace/mode-json.js")
    TextResource modeJsonJs();

    @Source("edu/ycp/cs/dh/acegwt/public/ace/mode-logfile.js")
    TextResource modeLogfileJs();

    @Source("edu/ycp/cs/dh/acegwt/public/ace/theme-chrome.js")
    TextResource themeChromeJs();

    @Source("edu/ycp/cs/dh/acegwt/public/ace/theme-logfile.js")
    TextResource themeLogFileJs();
}
