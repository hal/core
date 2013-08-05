package org.jboss.as.console.client.core;

import java.util.ArrayList;
import java.util.Arrays;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.PlainFormView;
import org.jboss.ballroom.client.widgets.forms.TextItem;

/**
 * @author Harald Pehl
 * @date 08/04/2013
 */
public class ProductConfigPanel implements IsWidget {

    @Override
    public Widget asWidget() {
        ProductConfig productConfig = GWT.create(ProductConfig.class);

        TextItem consoleVersion = new TextItem("console_version", "HAL version");
        consoleVersion.setValue(productConfig.getConsoleVersion() == null ? "n/a" : productConfig.getConsoleVersion());
        consoleVersion.setUndefined(false);
        TextItem coreVersion = new TextItem("core_version", "Core version");
        coreVersion.setValue(productConfig.getCoreVersion());
        coreVersion.setUndefined(false);
        TextItem productName = new TextItem("product_name", "Product name");
        productName.setValue(productConfig.getProductName());
        productName.setUndefined(false);
        TextItem productVersion = new TextItem("product_version", "Product version");
        productVersion.setValue(productConfig.getProductVersion());
        productVersion.setUndefined(false);
        TextItem profile = new TextItem("profile", "Profile");
        profile.setValue(productConfig.getProfile().name());
        profile.setUndefined(false);

        PlainFormView view = new PlainFormView(new ArrayList<FormItem>(
                Arrays.asList(consoleVersion, coreVersion, productName, productVersion, profile)));
        view.setNumColumns(1);
        Widget content = view.asWidget(null);
        view.refresh(true); // to fill the CellTable

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");
        layout.add(content);
        return layout;
    }
}
