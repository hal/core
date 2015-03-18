package org.jboss.as.console.client.core.bootstrap.hal;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

interface TextResources extends ClientBundle {

    TextResources INSTANCE = GWT.create(TextResources.class);

    @ClientBundle.Source("compat.nocache.js")
    TextResource compat();
}
