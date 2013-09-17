/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.console.client.administration.role.form;

import static org.jboss.as.console.client.administration.role.form.IncludeExcludeFormItem.Type.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.Role;
import org.jboss.as.console.client.administration.role.model.RoleComparator;
import org.jboss.as.console.client.administration.role.model.Roles;
import org.jboss.as.console.client.widgets.lists.DefaultCellList;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;

/**
 * Form item for the include and exclude roles of an {@link org.jboss.as.console.client.administration.role.model.RoleAssignment}.
 *
 * @author Harald Pehl
 */
public class IncludeExcludeFormItem extends FormItem<Map<IncludeExcludeFormItem.Type, Set<Role>>> {

    private final static Templates TEMPLATES = GWT.create(Templates.class);
    private final Map<Type, Set<Role>> value;
    private final ProvidesKey<Role> keyProvider;
    private final ListDataProvider<Role> availableProvider;
    private final ListDataProvider<Role> includeProvider;
    private final ListDataProvider<Role> excludeProvider;
    private final MultiSelectionModel<Role> availableSelectionModel;
    private final MultiSelectionModel<Role> includeSelectionModel;
    private final MultiSelectionModel<Role> excludeSelectionModel;
    private Button addInclude;
    private Button removeInclude;
    private Button addExclude;
    private Button removeExclude;
    private FormItemPanelWrapper wrapper;
    private Roles roles;

    public IncludeExcludeFormItem(final String name, final String title) {
        super(name, title);

        value = new HashMap<Type, Set<Role>>();
        value.put(AVAILABLE, new HashSet<Role>());
        value.put(INCLUDE, new HashSet<Role>());
        value.put(EXCLUDE, new HashSet<Role>());
        keyProvider = new Role.Key();

        // available roles
        availableProvider = new ListDataProvider<Role>(keyProvider);
        availableSelectionModel = new MultiSelectionModel<Role>(keyProvider);

        // included (assigned) roles
        includeProvider = new ListDataProvider<Role>(keyProvider);
        includeSelectionModel = new MultiSelectionModel<Role>(keyProvider);

        // excluded roles
        excludeProvider = new ListDataProvider<Role>(keyProvider);
        excludeSelectionModel = new MultiSelectionModel<Role>(keyProvider);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        // available roles and pager
        CellList<Role> availableList = new DefaultCellList<Role>(new RoleCell(), keyProvider);
        availableList.setPageSize(12);
        availableList.addStyleName("roles-list");
        availableList.addStyleName("roles-list-available");
        availableList.setSelectionModel(availableSelectionModel);
        availableProvider.addDataDisplay(availableList);
        availableSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                onSelect(AVAILABLE);
            }
        });
        DefaultPager pager = new DefaultPager();
        pager.setWidth("auto");
        pager.setDisplay(availableList);

        // included (assigned) roles
        CellList<Role> includeList = new DefaultCellList<Role>(new RoleCell(), keyProvider);
        includeList.setSelectionModel(includeSelectionModel);
        includeProvider.addDataDisplay(includeList);
        includeSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                onSelect(INCLUDE);
            }
        });
        ScrollPanel includeScroller = new ScrollPanel(includeList);
        includeScroller.addStyleName("roles-list");
        includeScroller.addStyleName("roles-list-assigned");

        // excluded roles
        CellList<Role> excludeList = new DefaultCellList<Role>(new RoleCell(), keyProvider);
        excludeList.setSelectionModel(excludeSelectionModel);
        excludeProvider.addDataDisplay(excludeList);
        excludeSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                onSelect(EXCLUDE);
            }
        });
        ScrollPanel excludeScroller = new ScrollPanel(excludeList);
        excludeScroller.addStyleName("roles-list");
        excludeScroller.addStyleName("roles-list-excluded");

        // add / remove buttons
        addInclude = new Button(TEMPLATES.arrow("right"));
        addInclude.getElement().setId("role_assignment_add_include");
        addInclude.setEnabled(false);
        addInclude.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(final ClickEvent event) {
                onAdd(INCLUDE);
            }
        });
        removeInclude = new Button(TEMPLATES.arrow("left"));
        removeInclude.getElement().setId("role_assignment_remove_include");
        removeInclude.setEnabled(false);
        removeInclude.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(final ClickEvent event) {
                onRemove(INCLUDE);
            }
        });
        addExclude = new Button(TEMPLATES.arrow("right"));
        addExclude.getElement().setId("role_assignment_add_exclude");
        addExclude.setEnabled(false);
        addExclude.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(final ClickEvent event) {
                onAdd(EXCLUDE);
            }
        });
        removeExclude = new Button(TEMPLATES.arrow("left"));
        removeExclude.getElement().setId("role_assignment_remove_exclude");
        removeExclude.setEnabled(false);
        removeExclude.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(final ClickEvent event) {
                onRemove(EXCLUDE);
            }
        });

        // put everything into panels
        HorizontalPanel content = new HorizontalPanel();
        content.setWidth("95%");

        // left: available
        Label label = new Label(Console.CONSTANTS.administration_available_roles());
        label.addStyleName("roles-list-label");
        VerticalPanel availableTitleAndTable = vert(label, availableList, pager);
        DOM.setStyleAttribute(availableTitleAndTable.getElement(), "marginBottom", "20px");
        content.add(availableTitleAndTable);
        availableTitleAndTable.getElement().getParentElement().setAttribute("width", "45%");

        // right - top: buttons and includes
        HorizontalPanel rightTop = new HorizontalPanel();
        label = new Label(Console.CONSTANTS.administration_assigned_roles());
        label.addStyleName("roles-list-label");
        VerticalPanel includeTitleAndTable = vert(label, includeScroller);
        DOM.setStyleAttribute(includeTitleAndTable.getElement(), "marginBottom", "20px");
        VerticalPanel includeButtons = vert(addInclude, removeInclude);
        DOM.setStyleAttribute(includeButtons.getElement(), "margin", "5px");
        rightTop.add(includeButtons);
        includeButtons.getElement().getParentElement().setAttribute("width", "10%");
        includeButtons.getElement().getParentElement().setAttribute("style", "vertical-align: middle");
        rightTop.add(includeTitleAndTable);

        // right - bottom: buttons and excludes
        HorizontalPanel rightBottom = new HorizontalPanel();
        label = new Label(Console.CONSTANTS.administration_excluded_roles());
        label.addStyleName("roles-list-label");
        VerticalPanel excludeTitleAndTable = vert(label, excludeScroller);
        DOM.setStyleAttribute(excludeTitleAndTable.getElement(), "marginBottom", "20px");
        VerticalPanel excludeButtons = vert(addExclude, removeExclude);
        DOM.setStyleAttribute(excludeButtons.getElement(), "margin", "5px");
        rightBottom.add(excludeButtons);
        excludeButtons.getElement().getParentElement().setAttribute("width", "10%");
        excludeButtons.getElement().getParentElement().setAttribute("style", "vertical-align: middle");
        rightBottom.add(excludeTitleAndTable);

        // right: include and exclude
        VerticalPanel right = new VerticalPanel();
        right.add(rightTop);
        right.add(rightBottom);
        content.add(right);
        right.getElement().getParentElement().setAttribute("width", "50%");

        wrapper = new FormItemPanelWrapper(content, this);
        return wrapper;
    }

    private VerticalPanel vert(Widget... widgets) {
        VerticalPanel panel = new VerticalPanel();
        if (widgets != null) {
            for (Widget widget : widgets) {
                panel.add(widget);
            }
        }
        return panel;
    }

    private void onSelect(final Type type) {
        switch (type) {
            case AVAILABLE:
                boolean availableSelected = !availableSelectionModel.getSelectedSet().isEmpty();
                addInclude.setEnabled(availableSelected);
                addExclude.setEnabled(availableSelected);
                break;
            case INCLUDE:
                removeInclude.setEnabled(!includeSelectionModel.getSelectedSet().isEmpty());
                break;
            case EXCLUDE:
                removeExclude.setEnabled(!excludeSelectionModel.getSelectedSet().isEmpty());
                break;
        }
    }

    private void onAdd(final Type type) {
        switch (type) {
            case INCLUDE: {
                Set<Role> selectedSet = availableSelectionModel.getSelectedSet();
                if (!selectedSet.isEmpty()) {
                    value.get(AVAILABLE).removeAll(selectedSet);
                    value.get(INCLUDE).addAll(selectedSet);
                    availableSelectionModel.clear();
                    updateDataProvider();
                }
                break;
            }
            case EXCLUDE: {
                Set<Role> selectedSet = availableSelectionModel.getSelectedSet();
                if (!selectedSet.isEmpty()) {
                    value.get(AVAILABLE).removeAll(selectedSet);
                    value.get(EXCLUDE).addAll(selectedSet);
                    availableSelectionModel.clear();
                    updateDataProvider();
                }
                break;
            }
        }
    }

    private void onRemove(final Type type) {
        switch (type) {
            case INCLUDE: {
                Set<Role> selectedSet = includeSelectionModel.getSelectedSet();
                if (!selectedSet.isEmpty()) {
                    value.get(INCLUDE).removeAll(selectedSet);
                    value.get(AVAILABLE).addAll(selectedSet);
                    includeSelectionModel.clear();
                    updateDataProvider();
                }
                break;
            }
            case EXCLUDE: {
                Set<Role> selectedSet = excludeSelectionModel.getSelectedSet();
                if (!selectedSet.isEmpty()) {
                    value.get(EXCLUDE).removeAll(selectedSet);
                    value.get(AVAILABLE).addAll(selectedSet);
                    excludeSelectionModel.clear();
                    updateDataProvider();
                }
                break;
            }
        }
    }

    @Override
    public void setEnabled(final boolean b) {
    }

    @Override
    public boolean validate(final Map<Type, Set<Role>> value) {
        //noinspection SimplifiableIfStatement
        if (isRequired) {
            return value != null && (!value.get(INCLUDE).isEmpty() || !value.get(EXCLUDE).isEmpty());
        }
        return true;
    }

    @Override
    public void clearValue() {
        clearSelection();
        updateDataProvider();
    }

    @Override
    public Map<Type, Set<Role>> getValue() {
        return this.value;
    }

    @Override
    public void setValue(final Map<Type, Set<Role>> value) {
        Set<Role> includes = value.get(INCLUDE);
        if (includes != null) {
            this.value.get(INCLUDE).clear();
            this.value.get(INCLUDE).addAll(includes);
            this.value.get(AVAILABLE).removeAll(this.value.get(INCLUDE));
        }
        Set<Role> excludes = value.get(EXCLUDE);
        if (excludes != null) {
            this.value.get(EXCLUDE).clear();
            this.value.get(EXCLUDE).addAll(excludes);
            this.value.get(AVAILABLE).removeAll(this.value.get(EXCLUDE));
        }
        if (roles != null) {
            Set<Role> available = new HashSet<Role>(roles.getRoles());
            if (includes != null) {available.removeAll(includes);}
            if (excludes != null) {available.removeAll(excludes);}
            this.value.put(AVAILABLE, available);
        }
        clearSelection();
        updateDataProvider();
    }

    @Override
    public String asString() {
        RoleComparator comperator = new RoleComparator();
        List<Role> includes = new ArrayList<Role>(value.get(INCLUDE));
        Collections.sort(includes, comperator);
        StringBuilder builder = new StringBuilder().append(rolesCsv(includes, ""));
        if (builder.length() > 0 && !value.get(EXCLUDE).isEmpty()) {
            List<Role> excludes = new ArrayList<Role>(value.get(EXCLUDE));
            Collections.sort(excludes, comperator);
            builder.append(", ").append(rolesCsv(excludes, "-"));
        }
        return builder.toString();
    }

    private String rolesCsv(Collection<Role> roles, String prefix) {
        StringBuilder builder = new StringBuilder();
        for (Iterator<Role> iterator = roles.iterator(); iterator.hasNext(); ) {
            Role role = iterator.next();
            builder.append(prefix).append(role.getName());
            if (role.isScoped()) {
                builder.append(" ").append(role.getScope());
            }
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    @Override
    public void setErroneous(boolean b) {
        super.setErroneous(b);
        wrapper.setErroneous(b);
    }

    @Override
    public String getErrMessage() {
        return super.getErrMessage() + ": Assign or exclude at least one role.";
    }

    public void update(final Roles roles) {
        this.roles = roles;
        value.get(AVAILABLE).clear();
        value.get(AVAILABLE).addAll(roles.getRoles());
        updateDataProvider();
    }

    private void clearSelection() {
        availableSelectionModel.clear();
        includeSelectionModel.clear();
        excludeSelectionModel.clear();
    }

    private void updateDataProvider() {
        ArrayList<Role> availableList = new ArrayList<Role>(value.get(AVAILABLE));
        ArrayList<Role> includeList = new ArrayList<Role>(value.get(INCLUDE));
        ArrayList<Role> excludeList = new ArrayList<Role>(value.get(EXCLUDE));

        RoleComparator comparator = new RoleComparator();
        Collections.sort(availableList, comparator);
        Collections.sort(includeList, comparator);
        Collections.sort(excludeList, comparator);

        availableProvider.setList(availableList);
        includeProvider.setList(includeList);
        excludeProvider.setList(excludeList);

        setModified(true);
    }

    public static enum Type {
        AVAILABLE, INCLUDE, EXCLUDE
    }

    interface Templates extends SafeHtmlTemplates {

        @SafeHtmlTemplates.Template("<i class=\"icon-arrow-{0}\"></i>")
        SafeHtml arrow(String direction);
    }

    static class RoleCell extends AbstractCell<Role> {

        @Override
        public void render(final Context context, final Role value, final SafeHtmlBuilder sb) {
            sb.appendEscaped(value.getName());
        }
    }

}
