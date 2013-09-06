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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.Principals;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.InputElementWrapper;

/**
 * @author Harald Pehl
 */
public class PrincipalFormItem extends FormItem<Principal> {

    private final Principal.Type type;
    private final Map<String, Principal> cache;
    private Principal value;
    private TextBox textBox;
    private SuggestBox suggestBox;
    private PrincipalSuggestOracle oracle;
    private InputElementWrapper wrapper;

    public PrincipalFormItem(final Principal.Type type, final String name, final String title) {
        super(name, title);

        this.type = type;
        this.cache = new HashMap<String, Principal>();

        setup();
    }

    private void setup() {
        textBox = new TextBox();
        textBox.setName(name);
        textBox.setTitle(title);
        textBox.setTabIndex(0);

        oracle = new PrincipalSuggestOracle(type);
        suggestBox = new SuggestBox(oracle, textBox);
        suggestBox.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                setModified(true);
                String newValue = event.getValue();
                setUndefined(newValue.equals(""));
                parseValue(newValue);
            }
        });
        suggestBox.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {
            @Override
            public void onSelection(
                    SelectionEvent<SuggestOracle.Suggestion> suggestionSelectionEvent) {
                setModified(true);
                String newValue = suggestBox.getValue();
                setUndefined(newValue.equals(""));
                parseValue(newValue);
            }
        });
        wrapper = new InputElementWrapper(suggestBox, this);
    }

    private void parseValue(final String name) {
        Principal principal = null;
        if (name != null && name.trim().length() != 0) {
            principal = cache.get(name);
            if (principal == null) {
                // create a new principal
                principal = new Principal(type, name);
                cache.put(principal.getName(), principal);
            }
        }
        setValue(principal);
    }

    @Override
    public void setFiltered(boolean filtered) {
        super.setFiltered(filtered);
        super.toggleAccessConstraint(textBox, filtered);
        textBox.setEnabled(!filtered);
    }

    @Override
    public Widget asWidget() {
        return wrapper;
    }

    @Override
    public Principal getValue() {
        return value;
    }

    @Override
    public void setValue(Principal value) {
        this.value = value;
        toggleExpressionInput(textBox, false);
        textBox.setValue(asString());
    }

    @Override
    public void resetMetaData() {
        super.resetMetaData();
        textBox.setValue(null);
    }

    @Override
    public void setExpressionValue(String expr) {
        this.expressionValue = expr;
        if (expressionValue != null) {
            toggleExpressionInput(textBox, true);
            textBox.setValue(expressionValue);
        }
    }

    @Override
    public void setEnabled(boolean b) {
        textBox.setEnabled(b);
    }

    @Override
    public void setErroneous(boolean b) {
        super.setErroneous(b);
        wrapper.setErroneous(b);
    }

    @Override
    public String getErrMessage() {
        return super.getErrMessage() + ": no whitespace, no special chars";
    }

    @Override
    public boolean validate(Principal value) {
        return !(isRequired() && value == null);
    }

    @Override
    public void clearValue() {
        setValue(null);
    }

    @Override
    protected void toggleExpressionInput(Widget target, boolean flag) {
        wrapper.setExpression(flag);
    }

    public String asString() {
        return value != null ? value.getName() : "";
    }

    public void update(Principals principals) {
        oracle.update(principals);
    }

    // ------------------------------------------------------ suggest stuff

    static class PrincipalSuggestion extends MultiWordSuggestOracle.MultiWordSuggestion {

        private final Principal principal;

        public PrincipalSuggestion(final Principal principal) {
            super(principal.getName(), principal.getName());
            this.principal = principal;
        }

        public Principal getPrincipal() {
            return principal;
        }
    }

    static class PrincipalSuggestOracle extends SuggestOracle {

        private final Principal.Type type;
        private final List<PrincipalSuggestion> suggestions;

        PrincipalSuggestOracle(Principal.Type type) {
            this.type = type;
            this.suggestions = new ArrayList<PrincipalSuggestion>();
        }

        @Override
        public void requestSuggestions(final Request request, final Callback callback) {
            Response response = new Response(matchingQuery(request.getQuery(), request.getLimit()));
            callback.onSuggestionsReady(request, response);
        }

        public Collection<PrincipalSuggestion> matchingQuery(final String query, final int limit) {
            List<PrincipalSuggestion> matchingResults = new ArrayList<PrincipalSuggestion>(limit);

            if (query.length() > 1) {
                String prefix = query.toLowerCase();

                int i = 0;
                int s = suggestions.size();
                while (i < s && !suggestions.get(i).getDisplayString().toLowerCase().contains(prefix)) {
                    i++;
                }

                int count = 0;
                while (i < s && suggestions.get(i).getDisplayString().toLowerCase().contains(prefix) && count < limit) {
                    matchingResults.add(suggestions.get(i));
                    i++;
                    count++;
                }
            }
            return matchingResults;
        }

        public void update(Principals principals) {
            List<Principal> byType = principals.get(type);
            if (byType != null) {
                suggestions.clear();
                for (Principal principal : byType) {
                    suggestions.add(new PrincipalSuggestion(principal));
                }
            }
        }
    }
}
