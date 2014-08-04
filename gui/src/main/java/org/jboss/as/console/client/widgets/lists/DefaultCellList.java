package org.jboss.as.console.client.widgets.lists;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.view.client.ProvidesKey;

/**
 * @author Heiko Braun
 * @date 12/14/11
 */
public class DefaultCellList<T> extends CellList<T> {

    private final static DefaultCellListResources RESOURCES = new DefaultCellListResources();

    public DefaultCellList(Cell<T> cell) {
        super(cell, RESOURCES);
    }

    public DefaultCellList(final Cell<T> cell, final ProvidesKey<T> keyProvider) {
        super(cell, RESOURCES, keyProvider);
    }
}
