package org.jboss.as.console.client.core;

import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.gwtplatform.mvp.client.ViewImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Heiko Braun
 * @since 12/02/15
 */
public abstract class MultiViewImpl extends ViewImpl implements MultiView {

    private DeckLayoutPanel deck;
    private Map<String, Integer> mapping = new HashMap<>();

    @Override
    public Widget asWidget() {

        this.deck = new DeckLayoutPanel();
        createWidget();
        return deck;
    }

    @Override
    public void register(String name, IsWidget widget) {
        deck.add(widget);
        mapping.put(name, deck.getWidgetCount()-1);
    }

    @Override
    public void toggle(String mode) {

        if(mapping.containsKey(mode)) {
            Integer index = mapping.get(mode);
            deck.showWidget(index);
        }
        else
        {
            throw new IllegalArgumentException("Unknown multi-view mapping: "+mode);
        }

    }
}
