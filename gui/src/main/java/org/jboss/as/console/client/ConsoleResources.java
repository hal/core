package org.jboss.as.console.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.TextResource;

/**
 * @author Heiko Braun
 * @date 3/7/12
 */
public interface ConsoleResources extends ClientBundle {

    ConsoleResources INSTANCE =  GWT.create(ConsoleResources.class);

    @CssResource.NotStrict
    @Source("org/jboss/as/console/public/console.css")
    CssResource css();

    @CssResource.NotStrict
    @Source("org/jboss/as/console/public/community.css")
    CssResource communityStyles();

    @CssResource.NotStrict
    @Source("org/jboss/as/console/public/product.css")
    CssResource productStyles();

    @CssResource.NotStrict
    @Source("org/jboss/as/console/public/prettyprint/prettify.css")
    CssResource prettifyCss();

    @Source("org/jboss/as/console/public/prettyprint/prettify.js")
    TextResource prettifyJs();

    @Source("org/jboss/as/console/public/lunr.js")
    TextResource lunrJs();

    @Source("org/jboss/as/console/public/mousetrap.js")
    TextResource mousetrapJs();
}

