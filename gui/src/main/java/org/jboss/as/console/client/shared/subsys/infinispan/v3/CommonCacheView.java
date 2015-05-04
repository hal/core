package org.jboss.as.console.client.shared.subsys.infinispan.v3;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.Collections;

/**
 * @author Heiko Braun
 * @since 28/04/15
 */
public class CommonCacheView {

    private final CachesPresenter presenter;
    private final String title;
    private final AddressTemplate address;
    private CommonCacheAttributes forms;

    public CommonCacheView(CachesPresenter presenter, String title, AddressTemplate address) {

        this.presenter = presenter;
        this.title = title;
        this.address = address;
    }

    Widget asWidget(){

        forms = new CommonCacheAttributes(
                presenter,
                title,
                address
        );
        return forms.asWidget();
    }

    public void updateFrom(ModelNode modelNode) {

        if(modelNode.isDefined())
            forms.updateFrom(modelNode.asPropertyList());
        else
            forms.updateFrom(Collections.<Property>emptyList());

    }
}
