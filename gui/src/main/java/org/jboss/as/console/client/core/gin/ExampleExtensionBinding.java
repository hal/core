package org.jboss.as.console.client.core.gin;

import com.gwtplatform.mvp.client.gin.AbstractPresenterModule;
import org.jboss.as.console.client.shared.subsys.tx.TransactionPresenter;
import org.jboss.as.console.client.shared.subsys.tx.TransactionView;
import org.jboss.as.console.client.shared.subsys.undertow.HttpView;
import org.jboss.as.console.client.shared.subsys.undertow.HttpViewImpl;
import org.jboss.as.console.client.shared.subsys.undertow.ServletView;
import org.jboss.as.console.client.shared.subsys.undertow.ServletViewImpl;
import org.jboss.as.console.client.shared.subsys.undertow.SimpleView;
import org.jboss.as.console.client.shared.subsys.undertow.UndertowHTTPPresenter;
import org.jboss.as.console.client.shared.subsys.undertow.UndertowServletPresenter;
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
                HttpView.class,
                HttpViewImpl.class,
                UndertowHTTPPresenter.MyProxy.class);

        bindPresenter(UndertowServletPresenter.class,
                ServletView.class,
                ServletViewImpl.class,
                UndertowServletPresenter.MyProxy.class);
    }
}
