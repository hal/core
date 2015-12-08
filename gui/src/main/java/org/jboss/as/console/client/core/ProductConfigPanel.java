package org.jboss.as.console.client.core;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.PlainFormView;
import org.jboss.ballroom.client.widgets.forms.RenderMetaData;
import org.jboss.ballroom.client.widgets.forms.TextItem;

import java.util.ArrayList;
import java.util.Collections;

import static java.util.Arrays.asList;
import static org.jboss.as.console.client.core.ApplicationProperties.DOMAIN_API;

/**
 * @author Harald Pehl
 * @date 08/04/2013
 */
public class ProductConfigPanel implements IsWidget {

    private final BootstrapContext context;
    private final ProductConfig productConfig;

    public ProductConfigPanel(BootstrapContext context, ProductConfig productConfig) {

        this.context = context;
        this.productConfig = productConfig;
    }

    @Override
    public Widget asWidget() {
        TextItem productName = new TextItem("product_name", "Product name");
        productName.setValue(productConfig.getProductName());
        TextItem productVersion = new TextItem("product_version", "Product version");
        productVersion.setValue(productConfig.getProductVersion());
        TextItem profile = new TextItem("profile", "Profile");
        TextItem consoleVersion = new TextItem("console_version", "HAL version");
        consoleVersion.setValue(productConfig.getConsoleVersion() == null ? "n/a" : productConfig.getConsoleVersion());
        TextItem coreVersion = new TextItem("core_version", "Core version");
        coreVersion.setValue(productConfig.getCoreVersion());
        profile.setValue(productConfig.getProfile().name());
        TextItem connectedTo = new TextItem("connectedTo", Console.CONSTANTS.connectedTo());
        connectedTo.setValue(context.getProperty(DOMAIN_API));

        ArrayList<FormItem> items = new ArrayList<FormItem>(
                asList(productName, productVersion, profile, consoleVersion, coreVersion));
        if (!context.isSameOrigin()) {
            items.add(connectedTo);
        }
        for (FormItem item : items) {
            item.setUndefined(false);
        }

        PlainFormView view = new PlainFormView(items);
        view.setNumColumns(1);
        RenderMetaData metaData = new RenderMetaData();
        metaData.setFilteredFields(Collections.<String>emptySet());
        Widget content = view.asWidget(metaData);
        view.refresh(true); // to fill the CellTable

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");
        layout.add(content);
        return layout;
    }
}
