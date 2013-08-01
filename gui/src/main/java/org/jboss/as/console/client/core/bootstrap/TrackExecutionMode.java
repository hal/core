package org.jboss.as.console.client.core.bootstrap;

import com.google.gwt.core.client.GWT;
import com.gwtplatform.mvp.client.googleanalytics.GoogleAnalytics;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

/**
 * @author Heiko Braun
 * @date 1/17/12
 */
public class TrackExecutionMode implements Function<BootstrapContext> {

    private GoogleAnalytics analytics;
    private final ProductConfig prodConfig;

    public TrackExecutionMode(GoogleAnalytics analytics) {
        this.analytics = analytics;
        this.prodConfig =  GWT.create(ProductConfig.class);
    }

    @Override
    public void execute(Control<BootstrapContext> control) {

        BootstrapContext bootstrap = control.getContext();


        if(bootstrap.hasProperty(BootstrapContext.STANDALONE))
        {
            String value = bootstrap.isStandalone() ? "standalone" : "domain";
            analytics.trackEvent("bootstrap", "exec-mode", value);
            analytics.trackEvent("bootstrap", "console-version", prodConfig.getConsoleVersion());

            control.proceed();
        }
        else
        {
            bootstrap.setlastError(new RuntimeException("Failed to resolve execution mode"));
            control.abort();
        }
    }

}
