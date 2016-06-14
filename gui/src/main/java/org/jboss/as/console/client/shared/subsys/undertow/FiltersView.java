/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.shared.subsys.undertow;

import java.util.List;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.ballroom.client.widgets.tabs.FakeTabPanel;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 * @since 04/06/2016
 */
public class FiltersView extends SuspendableViewImpl implements FilterPresenter.MyView {

    private static final String CUSTOM_FILTER = "custom-filter";
    private static final String ERROR_PAGE = "error-page";
    private static final String EXPRESSION_FILTER = "expression-filter";
    private static final String GZIP = "gzip";
    private static final String MODCLUSTER = "mod-cluster";
    private static final String REQUEST_LIMIT = "request-limit";
    private static final String RESPONSE_HEADER = "response-header";
    private static final String REWRITE = "rewrite";
    
    private static final AddressTemplate BASE_ADDRESS = AddressTemplate.of("{selected.profile}/subsystem=undertow/configuration=filter");
    private static final AddressTemplate CUSTOM_ADDRESS = BASE_ADDRESS.append(CUSTOM_FILTER + "=*");
    private static final AddressTemplate ERROR_PAGE_ADDRESS = BASE_ADDRESS.append(ERROR_PAGE + "=*");
    private static final AddressTemplate EXPRESSION_ADDRESS = BASE_ADDRESS.append(EXPRESSION_FILTER + "=*");
    private static final AddressTemplate GZIP_ADDRESS = BASE_ADDRESS.append(GZIP + "=*");
    private static final AddressTemplate MODCLUSTER_ADDRESS = BASE_ADDRESS.append(MODCLUSTER + "=*");
    private static final AddressTemplate REQUEST_LIMIT_ADDRESS = BASE_ADDRESS.append(REQUEST_LIMIT + "=*");
    private static final AddressTemplate RESPONSE_HEADER_ADDRESS = BASE_ADDRESS.append(RESPONSE_HEADER + "=*");
    private static final AddressTemplate REWRITE_ADDRESS = BASE_ADDRESS.append(REWRITE + "=*");
    
    private FilterPresenter presenter;

    private PagedView leftPanel;
    private FilterEditor customFilterEditor;
    private FilterEditor errorPageList;
    private FilterEditor expressionList;
    private FilterEditor gzipList;
    private FilterEditor modclusterList;
    private FilterEditor requestLimitList;
    private FilterEditor responseHeaderList;
    private FilterEditor rewriteList;


    @Override
    public void setPresenter(FilterPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        LayoutPanel layout = new LayoutPanel();

        FakeTabPanel titleBar = new FakeTabPanel("Filters");
        layout.add(titleBar);

        leftPanel = new PagedView(true);

        customFilterEditor = new FilterEditor(presenter, CUSTOM_ADDRESS, "Custom Filter");
        errorPageList = new FilterEditor(presenter, ERROR_PAGE_ADDRESS, "Error Page");
        expressionList = new FilterEditor(presenter, EXPRESSION_ADDRESS, "Expression");
        gzipList = new FilterEditor(presenter, GZIP_ADDRESS, "Gzip");
        modclusterList = new FilterEditor(presenter, MODCLUSTER_ADDRESS, "ModCluster");
        requestLimitList = new FilterEditor(presenter, REQUEST_LIMIT_ADDRESS, "Request Limit");
        responseHeaderList = new FilterEditor(presenter, RESPONSE_HEADER_ADDRESS, "Response Header");
        rewriteList = new FilterEditor(presenter, REWRITE_ADDRESS, "Rewrite");

        leftPanel.addPage("Custom Filter", customFilterEditor.asWidget());
        leftPanel.addPage("Error Page", errorPageList.asWidget());
        leftPanel.addPage("Expression", expressionList.asWidget());
        leftPanel.addPage("Gzip", gzipList.asWidget());
        leftPanel.addPage("ModCluster", modclusterList.asWidget());
        leftPanel.addPage("Request Limit", requestLimitList.asWidget());
        leftPanel.addPage("Response Header", responseHeaderList.asWidget());
        leftPanel.addPage("Rewrite", rewriteList.asWidget());

        // default page
        leftPanel.showPage(0);

        Widget panelWidget = leftPanel.asWidget();
        layout.add(panelWidget);
        layout.setWidgetTopHeight(titleBar, 0, Style.Unit.PX, 40, Style.Unit.PX);
        layout.setWidgetTopHeight(panelWidget, 40, Style.Unit.PX, 100, Style.Unit.PCT);
        
        return layout;
    }

    @Override
    public void setFilters(List<ModelNode> filters) {
        for (ModelNode prop: filters) {

            if (prop.has(CUSTOM_FILTER)) {
                
                List<Property> customList = prop.get(CUSTOM_FILTER).asPropertyList();
                customFilterEditor.updateValuesFromModel(customList);
                
            } else if (prop.has(ERROR_PAGE)) {
                
                List<Property> errorPagesList = prop.get(ERROR_PAGE).asPropertyList();
                errorPageList.updateValuesFromModel(errorPagesList);
                
            } else if (prop.has(EXPRESSION_FILTER)) {
                
                List<Property> expressionResList = prop.get(EXPRESSION_FILTER).asPropertyList();
                expressionList.updateValuesFromModel(expressionResList);
                
            } else if (prop.has(GZIP)) {
                
                List<Property> result = prop.get(GZIP).asPropertyList();
                gzipList.updateValuesFromModel(result);
                
            } else if (prop.has(MODCLUSTER)) {
                
                List<Property> result = prop.get(MODCLUSTER).asPropertyList();
                modclusterList.updateValuesFromModel(result);
                
            } else if (prop.has(REQUEST_LIMIT)) {
                
                List<Property> result = prop.get(REQUEST_LIMIT).asPropertyList();
                requestLimitList.updateValuesFromModel(result);
                
            } else if (prop.has(RESPONSE_HEADER)) {
                
                List<Property> result = prop.get(RESPONSE_HEADER).asPropertyList();
                responseHeaderList.updateValuesFromModel(result);
                
            } else if (prop.has(REWRITE)) {
                
                List<Property> result = prop.get(REWRITE).asPropertyList();
                rewriteList.updateValuesFromModel(result);
                
            }
        }
    }

}
