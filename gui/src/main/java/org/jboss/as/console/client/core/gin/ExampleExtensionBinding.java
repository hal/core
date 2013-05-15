package org.jboss.as.console.client.core.gin;

import com.gwtplatform.mvp.client.gin.AbstractPresenterModule;
import org.jboss.as.console.client.shared.subsys.tx.TransactionPresenter;
import org.jboss.as.console.client.shared.subsys.tx.TransactionView;
import org.jboss.as.console.client.shared.subsys.undertow.SimpleView;
import org.jboss.as.console.client.shared.subsys.undertow.SimpleViewImpl;
import org.jboss.as.console.client.shared.subsys.undertow.UndertowHTTPPresenter;
import org.jboss.as.console.client.shared.subsys.undertow.UndertowServletPresenter;
import org.jboss.as.console.client.standalone.runtime.VMMetricsPresenter;
import org.jboss.as.console.client.standalone.runtime.VMMetricsView;
import org.jboss.as.console.spi.GinExtensionBinding;

/**
 * @author Heiko Braun
 * @date 3/23/12
 */
@GinExtensionBinding
public class ExampleExtensionBinding extends AbstractPresenterModule {
    @Override
    protected void configure() {

        bindPresenter(TransactionPresenter.class,
                TransactionPresenter.MyView.class,
                TransactionView.class,
                TransactionPresenter.MyProxy.class);

        bindPresenter(UndertowHTTPPresenter.class,
                SimpleView.class,
                SimpleViewImpl.class,
                UndertowHTTPPresenter.MyProxy.class);


      /*  bindPresenter(UndertowServletPresenter.class,
                SimpleView.class,
                SimpleViewImpl.class,
                UndertowServletPresenter.MyProxy.class);*/
    }
}
