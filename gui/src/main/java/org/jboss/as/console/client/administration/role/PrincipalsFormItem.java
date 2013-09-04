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
package org.jboss.as.console.client.administration.role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.Principals;
import org.jboss.as.console.client.administration.role.model.PrincipalType;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.InputElementWrapper;

/**
 * Could be replaced by something like http://demo.raibledesigns.com/gwt-autocomplete/
 *
 * @author Harald Pehl
 */
public class PrincipalsFormItem extends FormItem<List<Principal>> {

    private final PrincipalType type;
    private final List<Principal> value;
    private final Map<String, Principal> cache;
    private final BeanFactory beanFactory;
    private TextArea textArea;
    private InputElementWrapper wrapper;

    public PrincipalsFormItem(final PrincipalType type, final String name, final String title,
            final BeanFactory beanFactory) {
        super(name, title);

        this.type = type;
        this.beanFactory = beanFactory;
        this.value = new ArrayList<Principal>();
        this.cache = new HashMap<String, Principal>();

        setup();
    }

    private void setup() {
        textArea = new TextArea();
        textArea.setName(name);
        textArea.setTitle(title);
        textArea.setTabIndex(0);
        textArea.setVisibleLines(3);
        textArea.getElement().setAttribute("placeholder", Console.CONSTANTS.administration_principals_form_item());
        textArea.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                setModified(true);
                String newValue = event.getValue();
                setUndefined(newValue.equals(""));
                parseValue(newValue);
            }
        });
        wrapper = new InputElementWrapper(textArea, this);
    }

    private void parseValue(final String newValue) {
        ArrayList<Principal> list = new ArrayList<Principal>();
        if (newValue != null && newValue.trim().length() != 0) {
            String[] lines = newValue.split("\n");
            for (String line : lines) {
                Principal principal = cache.get(line);
                if (principal != null) {
                    list.add(principal);
                } else {
                    // create a new principal
                    principal = beanFactory.principal().as();
                    principal.setName(line);
                    principal.setType(type);
                    list.add(principal);
                    cache.put(principal.getName(), principal);
                }
            }
        }
        setValue(list);
    }

    @Override
    public Widget asWidget() {
        return wrapper;
    }

    @Override
    public void setFiltered(boolean filtered) {
        super.setFiltered(filtered);
        super.toggleAccessConstraint(textArea, filtered);
        textArea.setEnabled(!filtered);
        wrapper.setConstraintsApply(filtered);
    }

    @Override
    public void setEnabled(final boolean b) {
        textArea.setEnabled(b);
    }

    @Override
    public void setErroneous(boolean b) {
        super.setErroneous(b);
        wrapper.setErroneous(b);
    }

    @Override
    protected void toggleExpressionInput(Widget target, boolean flag) {
        wrapper.setExpression(flag);
    }
    @Override
    public boolean validate(final List<Principal> value) {
        return true;
    }

    @Override
    public void clearValue() {
        value.clear();
        textArea.setText("");
    }

    @Override
    public void setUndefined(final boolean undefined) {
        super.setUndefined(undefined);
        if (undefined) {
            textArea.setText("");
        }
    }

    @Override
    public List<Principal> getValue() {
        return value;
    }

    @Override
    public void setValue(final List<Principal> value) {
        this.value.clear();
        this.value.addAll(value);
        StringBuilder builder = new StringBuilder();
        for (Iterator<Principal> iterator = value.iterator(); iterator.hasNext(); ) {
            Principal principal = iterator.next();
            builder.append(principal.getName());
            if (iterator.hasNext()) {
                builder.append("\n");
            }
        }

        textArea.setText(builder.toString());
    }

    public String asString() {
        StringBuilder builder = new StringBuilder("[");
        for (Iterator<Principal> iterator = value.iterator(); iterator.hasNext(); ) {
            Principal principal = iterator.next();
            builder.append(principal.getName());
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append("]");
        return builder.toString();
    }

    public void update(final Principals principals) {
        cache.clear();
        List<Principal> byType = principals.get(type);
        if (byType != null) {
            for (Principal principal : byType) {
                cache.put(principal.getName(), principal);
            }
        }
    }
}
