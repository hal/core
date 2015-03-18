package org.jboss.as.console.client.core.bootstrap.hal;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.googleanalytics.GoogleAnalytics;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.gwt.flow.client.Control;

/**
 * @author Heiko Braun
 * @date 1/17/12
 */
public class TrackExecutionMode implements BootstrapStep {

    private final ProductConfig prodConfig;
    private final GoogleAnalytics analytics;

    @Inject
    public TrackExecutionMode(GoogleAnalytics analytics) {
        this.analytics = analytics;
        this.prodConfig = GWT.create(ProductConfig.class);
    }

    @Override
    public void execute(Control<BootstrapContext> control) {

        BootstrapContext bootstrap = control.getContext();
        if (bootstrap.hasProperty(BootstrapContext.STANDALONE)) {
            String value = bootstrap.isStandalone() ? "standalone" : "domain";
            analytics.trackEvent("bootstrap", "exec-mode", value);
            analytics.trackEvent("bootstrap", "console-version", prodConfig.getCoreVersion());

            control.proceed();
        } else {
            bootstrap.setlastError(new RuntimeException("Failed to resolve execution mode"));
            control.abort();
        }
    }
}
