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

import java.util.Map;

import javax.inject.Inject;

import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.PopupView;
import com.gwtplatform.mvp.client.PresenterWidget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.Preferences;

/**
 * Maintains the settings dialogue
 *
 * @author Heiko Braun
 * @date 5/3/11
 */
public class SettingsPresenterWidget
        extends PresenterWidget<SettingsPresenterWidget.MyView> {

    private final BeanFactory factory;
    private final ProductConfig prodConfig;


    public interface MyView extends PopupView {
        void setPresenter(SettingsPresenterWidget presenter);
    }

    @Inject
    public SettingsPresenterWidget(final EventBus eventBus, final MyView view, BeanFactory factory,
            ProductConfig prodConfig) {

        super(eventBus, view);
        this.factory = factory;
        this.prodConfig = prodConfig;
        view.setPresenter(this);
    }

    public void hideView() {
        getView().hide();

    }

    public void onSaveDialogue(CommonSettings settings) {

        // see also App.gwt.xml

        Map<String, Object> properties = AutoBeanUtils.getAllProperties(
                AutoBeanUtils.getAutoBean(settings)
        );

        for(String token : properties.keySet())
        {
            Preferences.Key key = Preferences.Key.match(token);
            assert key !=null : "invalid token "+token;
            Object value = properties.get(token);
            if(null==value || value.equals(""))
                value = key.getDefaultValue();
            Preferences.set(key, String.valueOf(value));
        }

        Console.info(Console.MESSAGES.savedSettings());

    }

    public void onCancelDialogue() {
        getView().hide();
    }

    public CommonSettings getCommonSettings() {
        CommonSettings settings = factory.settings().as();
        settings.setLocale(Preferences.get(Preferences.Key.LOCALE));
        String analyticsDefault = ProductConfig.Profile.PRODUCT.equals(prodConfig.getProfile()) ? "false" : "true";
        settings.setAnalytics(Boolean.valueOf(Preferences.get(Preferences.Key.ANALYTICS, analyticsDefault)));
        settings.setSecurityCache(Boolean.valueOf(Preferences.get(Preferences.Key.SECURITY_CONTEXT, "true")));
        return settings;
    }
}
