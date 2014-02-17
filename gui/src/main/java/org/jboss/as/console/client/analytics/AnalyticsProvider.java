package org.jboss.as.console.client.analytics;

import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import com.google.inject.Provider;
import com.gwtplatform.mvp.client.googleanalytics.GoogleAnalytics;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.shared.Preferences;

/**
 * @author Heiko Braun
 * @date 10/24/12
 */
public class AnalyticsProvider implements Provider<GoogleAnalytics> {

    private static final GoogleAnalytics NOOP = new NoopAnalytics();

    private ProductConfig prodConfig;

    @Inject
    public AnalyticsProvider(ProductConfig prodConfig) {
        this.prodConfig = prodConfig;
    }

    @Override
    public GoogleAnalytics get() {

        GoogleAnalytics analytics;

        boolean isWebMode = GWT.isScript();
        boolean isEAP = ProductConfig.Profile.PRODUCT.equals(prodConfig.getProfile());
        boolean enabledInPreferences = Preferences.has(Preferences.Key.ANALYTICS) && Preferences
                .get(Preferences.Key.ANALYTICS).equals("true");

        if (isWebMode && !enabledInPreferences) {
            // Google Analytics is an opt-in for the product and an opt-out for the community version
            analytics = isEAP ? NOOP : new CustomAnalyticsImpl();
        } else {
            analytics = NOOP;
        }
        System.out.println("Google analytics: Using " + (analytics == NOOP ? "stub" : "real") + " implementation");

        return analytics;
    }


    static class NoopAnalytics implements GoogleAnalytics {

        @Override
        public void init(String userAccount) {}

        @Override
        public void addAccount(String trackerName, String userAccount) {}

        @Override
        public void trackPageview() {}

        @Override
        public void trackPageview(String pageName) {}

        @Override
        public void trackPageview(String trackerName, String pageName) {}

        @Override
        public void trackEvent(String category, String action) {}

        @Override
        public void trackEventWithTracker(String trackerName, String category, String action) {}

        @Override
        public void trackEvent(String category, String action, String optLabel) {}

        @Override
        public void trackEventWithTracker(String trackerName, String category, String action, String optLabel) {}

        @Override
        public void trackEvent(String category, String action, String optLabel, int optValue) {}

        @Override
        public void trackEventWithTracker(String trackerName, String category, String action, String optLabel,
                int optValue) {}

        @Override
        public void trackEvent(String category, String action, String optLabel, int optValue,
                boolean optNonInteraction) {}

        @Override
        public void trackEventWithTracker(String trackerName, String category, String action, String optLabel,
                int optValue, boolean optNonInteraction) {}
    }
}
