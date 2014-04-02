/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.console.client.core.settings;

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.PopupViewImpl;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.shared.Preferences;
import org.jboss.as.console.client.shared.help.StaticHelpPanel;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;


/**
 * @author Heiko Braun
 * @date 5/3/11
 */
public class SettingsView extends PopupViewImpl implements SettingsPresenterWidget.MyView {

    private DefaultWindow window;
    private SettingsPresenterWidget presenter;
    private Form<CommonSettings> form;

    @Inject
    public SettingsView(EventBus eventBus, ProductConfig productConfig) {
        super(eventBus);

        window = new DefaultWindow(Console.CONSTANTS.common_label_settings());
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        form = new Form<CommonSettings>(CommonSettings.class);

        ComboBoxItem localeItem = null;
        List<String> locales = productConfig.getLocales();
        if (locales.size() > 1) {
            localeItem = new ComboBoxItem(Preferences.Key.LOCALE.getToken(),
                    Preferences.Key.LOCALE.getTitle());
            localeItem.setDefaultToFirstOption(true);
            localeItem.setValueMap(locales);
        }

        //CheckBoxItem useCache = new CheckBoxItem(Preferences.Key.USE_CACHE.getToken(), Preferences.Key.USE_CACHE.getTitle());

        CheckBoxItem enableAnalytics = new CheckBoxItem(Preferences.Key.ANALYTICS.getToken(),
                Preferences.Key.ANALYTICS.getTitle());

        if (localeItem != null) {
            form.setFields(localeItem, enableAnalytics);
        } else {
            form.setFields(enableAnalytics);
        }

        CheckBoxItem enableSecurityContextCache = new CheckBoxItem(Preferences.Key.SECURITY_CONTEXT.getToken(),
                Preferences.Key.SECURITY_CONTEXT.getTitle());

        //form.setFields(localeItem, enableAnalytics, enableSecurityContextCache);

        Widget formWidget = form.asWidget();
        formWidget.getElement().setAttribute("style", "margin:15px");

        DialogueOptions options = new DialogueOptions(
                Console.CONSTANTS.common_label_save(),
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        presenter.onSaveDialogue(form.getUpdatedEntity());

                        presenter.hideView();

                        Feedback.confirm(Console.MESSAGES.restartRequired(), Console.MESSAGES.restartRequiredConfirm(),
                                new Feedback.ConfirmationHandler() {
                                    @Override
                                    public void onConfirmation(boolean isConfirmed) {

                                        // Ignore: it crashes the browser..

                                        /*if(isConfirmed){
                                           Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                                               @Override
                                               public void execute() {
                                                   reload();
                                               }
                                           });

                                       } */
                                    }
                                }
                        );
                    }
                },
                Console.CONSTANTS.common_label_cancel(),
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        presenter.onCancelDialogue();
                    }
                }
        );

        options.getElement().setAttribute("style", "padding:10px");

        SafeHtmlBuilder html = new SafeHtmlBuilder();
        html.appendHtmlConstant("<ul>");
        if (localeItem != null) {
            html.appendHtmlConstant("<li>").appendEscaped("Locale: The user interface language.").appendHtmlConstant(
                    "</li>");
        }
        html.appendHtmlConstant("<li>");
        html.appendEscaped(
                "Enable Usage Data Collection: The Admin Console has the capability to collect usage data via ");
        html.appendHtmlConstant("<a href=\"http://www.google.com/analytics/\" target=\"_blank\">Google Analytics</a>");
        html.appendEscaped(
                ". This data will be used exclusively by Red Hat to improve the console in future releases. By default this data collection is ");
        if (productConfig.getProfile() == ProductConfig.Profile.COMMUNITY) {
            html.appendEscaped("enabled, but you can disable collection of this data by unchecking the Enable Usage Data Collection box.");
        } else {
            html.appendEscaped("disabled, but you can enable collection of this data by checking the Enable Usage Data Collection box.");
        }
        html.appendHtmlConstant("</li>");
        //html.appendHtmlConstant("<li>").appendEscaped("Security Cache: If disabled the security context will be re-created everytime you access a dialog (performance hit).");
        html.appendHtmlConstant("</ul>");
        StaticHelpPanel help = new StaticHelpPanel(html.toSafeHtml());
        layout.add(help.asWidget());
        layout.add(form.asWidget());

        window.setWidth(480);
        window.setHeight(360);
        window.trapWidget(new WindowContentBuilder(layout, options).build());
        window.setGlassEnabled(true);
        window.center();
    }

    /*private void onCenter() {
        form.edit(presenter.getCommonSettings());
    } */

    @Override
    public Widget asWidget() {
        form.edit(presenter.getCommonSettings());
        return window;
    }

    @Override
    public void setPresenter(SettingsPresenterWidget presenter) {
        this.presenter = presenter;
    }

    public static native JavaScriptObject reload() /*-{
        window.location.reload();
    }-*/;
}
