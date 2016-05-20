/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.shared.homepage;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.PopupPanel;
import elemental.events.KeyboardEvent;
import org.jboss.as.console.client.core.ApplicationProperties;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.shared.Preferences;

/**
 * Helper class to manage the guided tour iframe and make it closable from within the guided tour iframe.
 *
 * @author Harald Pehl
 */
class GuidedTourHelper {

    static PopupPanel guidedTour;

    static void init(BootstrapContext bootstrapContext) {
        String locale = Preferences.get(Preferences.Key.LOCALE, "en");
        String url = bootstrapContext.getProperty(ApplicationProperties.GUIDED_TOUR) + "/" +
                (bootstrapContext.isStandalone() ? "standalone" : "domain") + "/step1.html?setLng=" + locale;

        Frame tourFrame = new Frame(url);
        tourFrame.setWidth("100%");
        tourFrame.setHeight("100%");

        guidedTour = new PopupPanel(true, true) {
            {
                Window.addResizeHandler(resizeEvent -> {
                    if (isShowing()) {
                        center();
                    }
                });
            }

            @Override
            protected void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                if (Event.ONKEYUP == event.getTypeInt()) {
                    if (event.getNativeEvent().getKeyCode() == KeyboardEvent.KeyCode.ESC) {
                        hide();
                    }
                }
            }
        };
        guidedTour.setGlassEnabled(true);
        guidedTour.setAnimationEnabled(false);
        guidedTour.setWidget(tourFrame);
        guidedTour.setWidth("1120px");
        guidedTour.setHeight("800px");
        guidedTour.setStyleName("default-window");

        exportCloseMethod();
    }

    static void open() {
        if (guidedTour != null) {
            guidedTour.center();
        }
    }

    static void close() {
        if (guidedTour != null) {
            guidedTour.hide();
        }
    }

    static native void exportCloseMethod() /*-{
        $wnd.closeGuidedTour = $entry(@org.jboss.as.console.client.shared.homepage.GuidedTourHelper::close());
    }-*/;
}
