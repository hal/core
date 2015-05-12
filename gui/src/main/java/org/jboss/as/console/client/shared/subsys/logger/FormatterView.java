package org.jboss.as.console.client.shared.subsys.logger;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 */
public class FormatterView {

    private LoggerPresenter presenter;

    private PagedView panel;

    private static final AddressTemplate CUSTOM =
            AddressTemplate.of("{selected.profile}/subsystem=logging/custom-formatter=*");


    private static final AddressTemplate PATTERN =
            AddressTemplate.of("{selected.profile}/subsystem=logging/pattern-formatter=*");


    private MasterDetailTemplate patternFormatter;
    private MasterDetailTemplate customFormatter;

    public FormatterView(LoggerPresenter presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {

        panel = new PagedView(true);

        patternFormatter = new MasterDetailTemplate(presenter, PATTERN, "Pattern Formatter");
        customFormatter = new MasterDetailTemplate(presenter, CUSTOM, "Custom Formatter");

        panel.addPage("Pattern", patternFormatter.asWidget());
        panel.addPage("Custom", customFormatter.asWidget());

        // default page
        panel.showPage(0);

        return panel.asWidget();
    }



    public void updatePatternFormatter(List<Property> items) {
        patternFormatter.setData(items);
    }

    public void updateCustomFormatter(List<Property> items) {
        customFormatter.setData(items);
    }


}
