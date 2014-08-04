package org.jboss.as.console.client.widgets.tabs;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.Map;

/**
 * A tab panel which is limited to show a maximum of {@link #PAGE_LIMIT} pages at once. Addiing more pages will
 * display a selector icon. Clicking the selector icon brings up a menu where you can select the "off-page"
 * elements. Selected off-page elements are displayed in the last tab.
 * <p/>
 * Please note that this tab panel only keeps an index over its visible tabs. Off-page elements cannot be selected or
 * removed by index, but only by name.
 *
 * @author Heiko Braun, Harald Pehl
 */
public class DefaultTabLayoutPanel extends TabLayoutPanel implements OffPageTabPanel {

    /**
     * The number of simultaneously visible pages. Adding more tabs will show a selector icon.
     */
    public final static int PAGE_LIMIT = 4;

    public final static String STYLE_NAME = "hal-TabLayout";

    /**
     * Whether the tabs will be closable.
     * Please note that the tab which is added first will <b>never</b> be closable.
     */
    private final boolean closeable;

    /**
     * The off-page elements
     */
    private final OffPageContainer offPageContainer;

    /**
     * The tabs in this tab layout. The collection contains {@link #PAGE_LIMIT} tabs maximum.
     */
    private final Tabs tabs;

    private int prevSelectedIndex;


    public DefaultTabLayoutPanel(double barHeight, Style.Unit barUnit) {
        this(barHeight, barUnit, false);
    }

    public DefaultTabLayoutPanel(double barHeight, Style.Unit barUnit, boolean closeable) {
        super(barHeight, barUnit);

        this.closeable = closeable;
        this.tabs = new Tabs(this);
        this.offPageContainer = new OffPageContainer();
        this.prevSelectedIndex = -1;

        addStyleName(STYLE_NAME);
        getElement().setAttribute("role", "tablist");

    }

    @Override
    protected void onAttach() {
        super.onAttach();
        if (Window.Navigator.getUserAgent().contains("MSIE")) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    forceLayout();
                }
            });
        }
    }


    // ------------------------------------------------------ insert & remove

    public void insert(final Widget page, final Widget tab, final int beforeIndex) {
        final String text = (tab instanceof HasText) ? ((HasText) tab).getText() : "Tab #" + beforeIndex;

        if (beforeIndex < PAGE_LIMIT - 1) {
            // visible pages
            Tab t = tabs.add(text, beforeIndex);
            super.insert(page, t, beforeIndex);

        } else if (beforeIndex == PAGE_LIMIT - 1) {
            // last visible page
            Tab t = tabs.add(text, beforeIndex);
            offPageContainer.add(new OffPageText(0, text), page);
            super.insert(offPageContainer.asWidget(), t, beforeIndex);

        } else {
            // off page - don't use beforeIndex as it'll always be PAGE_LIMIT
            int last = offPageContainer.size();
            offPageContainer.add(new OffPageText(last, text), page);
            tabs.showSelector();
        }
    }

    @Override
    public boolean remove(int index) {
        // only visible tabs can be removed!
        if (index < 0 || index > getWidgetCount() - 1) {
            return false;
        }

        boolean removed = false;
        int count = getWidgetCount();
        if (count < PAGE_LIMIT) {
            // no need to take care of any off-page handling
            tabs.remove(index);
            removed = super.remove(index);

        } else {
            if (index < PAGE_LIMIT - 1) {
                // 'regular' visible tab is about to be removed; there's at least one element in offPageContainer
                // first remove the specified tab
                tabs.remove(index);
                removed = super.remove(index);

                // then remove the last tab (offPageContainer)
                int last = getWidgetCount() - 1;
                OffPageContainer backup = offPageContainer.copy();
                offPageContainer.clear();
                tabs.remove(last);
                super.remove(last);

                // and finally re-add the off page elements
                for (Map.Entry<OffPageText, Widget> entry : backup) {
                    add(entry.getValue(), entry.getKey().getText());
                }
                backup.clear();

            } else if (index == PAGE_LIMIT - 1) {
                // last tab which contains the offPageContainer is about to be removed
                offPageContainer.remove(0);
                if (offPageContainer.isEmpty()) {
                    // no other off-page element is left, completely remove the last tab
                    selectTab(0, false);
                    tabs.remove(index);
                    removed = super.remove(index);
                }

                else if (getSelectedIndex() == PAGE_LIMIT - 1) {
                    // the last tab is visible while we're removing the page, select the first off page element
                    selectTab(offPageContainer.getText(0).getText());
                    removed = true;
                }
            }
        }

        if (offPageContainer.size() > 1) {
            tabs.showSelector();
        } else {
            tabs.hideSelector();
        }
        return removed;
    }


    // ------------------------------------------------------ select

    @Override
    public void selectTab(final int index) {
        if (index < PAGE_LIMIT) {
            // visible pages
            flagSelected(prevSelectedIndex, false);
            super.selectTab(index);
            flagSelected(index, true);
            prevSelectedIndex = index;

            if (index == PAGE_LIMIT - 1 && offPageContainer.getSelectedDeck() == -1) {
                offPageContainer.selectDeck(0);
            }

        } else {
            // off page
            offPageContainer.selectDeck(offPageIndex(index));
            if (getSelectedIndex() == PAGE_LIMIT - 1) {
                SelectionEvent.fire(this, PAGE_LIMIT - 1);
            }
            tabs.lastTab().setText(offPageContainer.getTexts().get(offPageIndex(index)).getText());
            selectTab(PAGE_LIMIT - 1);
        }
    }

    /**
     * Selects the tab with the specified text. If there's no tab with the specified text, the selected tab remains
     * unchanged. If there's more than one tab with the specified text, the first one will be selected.
     *
     * @param text
     */
    @Override
    public void selectTab(final String text) {
        for (int i = 0; i < tabs.size(); i++) {
            if (text.equals(tabs.get(i).getText())) {
                selectTab(i);
                return;
            }
        }

        // not found in visible tabs, should be in off-page
        for (OffPageText opt : offPageContainer.getTexts()) {
            if (text.equals(opt.getText())) {
                if (opt.getIndex() == 0) {
                    // the first off-page needs special treatment
                    offPageContainer.selectDeck(opt.getIndex());
                    if (getSelectedIndex() == PAGE_LIMIT - 1) {
                        SelectionEvent.fire(this, PAGE_LIMIT - 1);
                    }
                    tabs.lastTab().setText(opt.getText());
                    selectTab(PAGE_LIMIT - 1);
                } else {
                    selectTab(PAGE_LIMIT - 1 + opt.getIndex());
                }
            }
        }
    }


    // ------------------------------------------------------ custom methods

    /**
     * Returns {@code true} if there's a tab with the specified text, {@code false} otherwise
     */
    @Override
    public boolean contains(final String text) {
        for (Tab tab : tabs) {
            if (text.equals(tab.getText())) {
                return true;
            }
        }

        // not found in visible tabs, look in off-page
        return offPageContainer.contains(text);
    }

    private int offPageIndex(int index) {
        if (index <= PAGE_LIMIT - 1) {
            return 0;
        } else {
            return index - PAGE_LIMIT + 1;
        }
    }

    private void flagSelected(int index, boolean isSelected) {
        if (index < 0 || index > getWidgetCount() - 1) return;
        if (isSelected) {
            getTabWidget(index).getElement().setAttribute("aria-selected", "true");
        } else {
            getTabWidget(index).getElement().removeAttribute("aria-selected");
        }
    }


    // ------------------------------------------------------ properties


    @Override
    public Widget getWidget(int index) {
        if (index == PAGE_LIMIT - 1) {
            if (offPageContainer.isEmpty()) {
                return offPageContainer.asWidget();
            }

            int deckIndex = offPageContainer.getSelectedDeck() == -1 ? 0 : offPageContainer.getSelectedDeck();
            return offPageContainer.getDeck(deckIndex);
        }
        return super.getWidget(index);
    }

    OffPageContainer getOffPageContainer() {
        return offPageContainer;
    }

    boolean isCloseable() {
        return closeable;
    }
}
