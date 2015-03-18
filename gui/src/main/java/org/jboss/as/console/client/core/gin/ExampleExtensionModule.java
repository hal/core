package org.jboss.as.console.client.core.gin;

import com.gwtplatform.mvp.client.gin.AbstractPresenterModule;
import org.jboss.as.console.client.shared.subsys.tx.TransactionPresenter;
import org.jboss.as.console.client.shared.subsys.tx.TransactionView;
import org.jboss.as.console.spi.GinExtensionBinding;

/**
 * @author Heiko Braun
 * @date 3/23/12
 */
@GinExtensionBinding
public class ExampleExtensionModule extends AbstractPresenterModule {

    @Override
    protected void configure() {
        bindPresenter(TransactionPresenter.class,
                TransactionPresenter.MyView.class,
                TransactionView.class,
                TransactionPresenter.MyProxy.class);
    }
}
