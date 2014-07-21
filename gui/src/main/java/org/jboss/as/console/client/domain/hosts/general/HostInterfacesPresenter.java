/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

package org.jboss.as.console.client.domain.hosts.general;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.hosts.HostMgmtPresenter;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.PlaceRequestSecurityFramework;
import org.jboss.as.console.client.shared.general.InterfaceManagement;
import org.jboss.as.console.client.shared.general.InterfaceManagementImpl;
import org.jboss.as.console.client.shared.general.model.Interface;
import org.jboss.as.console.client.shared.general.model.LoadInterfacesCmd;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.OperationMode;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.circuit.PropagatesChange;

import java.util.List;

import static org.jboss.as.console.spi.OperationMode.Mode.DOMAIN;

/**
 * @author Heiko Braun
 * @date 5/18/11
 */
public class HostInterfacesPresenter extends Presenter<HostInterfacesPresenter.MyView, HostInterfacesPresenter.MyProxy>
        implements InterfaceManagement.Callback {

    @ProxyCodeSplit
    @NameToken(NameTokens.HostInterfacesPresenter)
    @OperationMode(DOMAIN)
    @AccessControl(resources = {"/{selected.host}/interface=*",})
    public interface MyProxy extends Proxy<HostInterfacesPresenter>, Place {}


    public interface MyView extends View {

        void setPresenter(HostInterfacesPresenter presenter);

        void setInterfaces(List<Interface> interfaces);

        void setDelegate(InterfaceManagement delegate);
    }


    private final DispatchAsync dispatcher;
    private final ApplicationMetaData metaData;
    private final PlaceRequestSecurityFramework placeRequestSecurityFramework;
    private final InterfaceManagement delegate;
    private final HostStore hostStore;


    @Inject
    public HostInterfacesPresenter(EventBus eventBus, MyView view, MyProxy proxy, HostStore hostStore,
            DispatchAsync dispatcher, ApplicationMetaData metaData,
            PlaceRequestSecurityFramework placeRequestSecurityFramework) {

        super(eventBus, view, proxy);

        this.hostStore = hostStore;
        this.dispatcher = dispatcher;
        this.metaData = metaData;
        this.placeRequestSecurityFramework = placeRequestSecurityFramework;
        this.delegate = new InterfaceManagementImpl(dispatcher, new EntityAdapter<Interface>(Interface.class, metaData),
                metaData.getBeanMetaData(Interface.class));
        this.delegate.setCallback(this);

    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        getView().setDelegate(this.delegate);

        hostStore.addChangeHandler(new PropagatesChange.Handler() {
            @Override
            public void onChange(Class<?> source) {
                if (isVisible()) {
                    placeRequestSecurityFramework.update(HostInterfacesPresenter.this, hostPlaceRequest());
                    loadInterfaces();
                }
            }
        });
        placeRequestSecurityFramework.addCurrentContext(hostPlaceRequest());
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, HostMgmtPresenter.TYPE_MainContent, this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadInterfaces();
    }

    private PlaceRequest hostPlaceRequest() {
        return new PlaceRequest.Builder().nameToken(getProxy().getNameToken())
                .with("host", hostStore.getSelectedHost()).build();
    }

    @Override
    public ModelNode getBaseAddress() {
        ModelNode address = new ModelNode();
        address.setEmptyList();
        address.add("host", hostStore.getSelectedHost());
        return address;
    }

    public void loadInterfaces() {
        ModelNode address = new ModelNode();
        address.add("host", hostStore.getSelectedHost());

        LoadInterfacesCmd loadInterfacesCmd = new LoadInterfacesCmd(dispatcher, address, metaData);
        loadInterfacesCmd.execute(new SimpleCallback<List<Interface>>() {
            @Override
            public void onSuccess(List<Interface> result) {
                getView().setInterfaces(result);
            }
        });
    }
}
