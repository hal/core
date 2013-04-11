package org.jboss.as.console.mbui.reification;

import com.google.gwt.user.client.ui.Widget;
import org.useware.kernel.model.structure.InteractionUnit;

/**
 * @author Heiko Braun
 * @date 11/13/12
 */
interface TabPanelContract
{
    void add(InteractionUnit unit, Widget widget);

    //void add(Widget widget, String name);

    Widget as();
}
