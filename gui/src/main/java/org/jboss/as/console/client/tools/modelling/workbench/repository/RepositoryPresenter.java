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
package org.jboss.as.console.client.tools.modelling.workbench.repository;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Document;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.*;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.RequiredResourcesProvider;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.tools.modelling.workbench.repository.vfs.Entry;
import org.jboss.as.console.client.tools.modelling.workbench.repository.vfs.Vfs;
import org.jboss.as.console.mbui.DialogRepository;
import org.jboss.as.console.mbui.Framework;
import org.jboss.as.console.mbui.Kernel;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.marshall.DialogXML;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.useware.kernel.model.Dialog;

import java.util.List;

/**
 * Lists the available interaction units and let the user create new interaction units.
 *
 * Events fired:
 * <ul>
 *     <li>Reify</li>
 * </ul>
 *
 * @author Harald Pehl
 * @date 10/30/2012
 */
public class RepositoryPresenter
        extends Presenter<RepositoryPresenter.MyView, RepositoryPresenter.MyProxy>
        implements EditorResizeEvent.ResizeListener, ModelEditor.Presenter {

    public interface MyView extends View {
        void setPresenter(RepositoryPresenter presenter);
        void setFullScreen(boolean fullscreen);
        void updateDirectory(Entry dir, List<Entry> entries);
        void clearHistory();
        void updateFile(String name, String fileContents);
        String getText();
    }


    @NoGatekeeper
    @ProxyStandard
    @CustomProvider(RequiredResourcesProvider.class)
    @NameToken("mbui-workbench")
    public interface MyProxy extends ProxyPlace<RepositoryPresenter> {}


    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent = new GwtEvent.Type<RevealContentHandler<?>>();

    private Vfs vfs;
    private final Kernel kernel;
    private final DispatchAsync dispatcher;
    private DefaultWindow preview;
    private Entry selectedDialog = null;


    @Inject
    public RepositoryPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            final SampleRepository sampleRepository, final DispatchAsync dispatcher, CoreGUIContext globalContext) {
        super(eventBus, view, proxy);

        this.dispatcher = dispatcher;

        DialogRepository adhocRepo = new DialogRepository() {
            @Override
            public void getDialog(String name, AsyncCallback<Dialog> callback) {
                callback.onSuccess(new DialogXML().unmarshall(getView().getText()));
            }
        };

        // mbui kernel instance
        this.kernel = new Kernel(adhocRepo, new Framework() {
            @Override
            public DispatchAsync getDispatcher() {
                return dispatcher;
            }

            @Override
            public SecurityFramework getSecurityFramework() {
                return Console.MODULES.getSecurityFramework();
            }
        }, globalContext);

        kernel.setCaching(false);

        this.vfs = new Vfs();

    }

    @Override
    protected void onBind()
    {
        super.onBind();
        getView().setPresenter(this);
        getEventBus().addHandler(EditorResizeEvent.TYPE, this);
    }

    @Override
    public void onResizeRequested(boolean fullscreen) {
        getView().setFullScreen(fullscreen);
    }

    @Override
    protected void onReset() {
        loadDir(Entry.ROOT, true);
    }

    public void setDisableCache(boolean disableCache) {
        kernel.setCaching(disableCache);
    }

    public void onVisualize()
    {

        Dialog dialog = new DialogXML().unmarshall(getView().getText());
        DialogVisualization visualization = new DialogVisualization(dialog);
        DefaultWindow window = new DefaultWindow("Dialog: "+dialog.getId());
        window.setWidth(800);
        window.setHeight(600);
        ScrollPanel widgets = new ScrollPanel(visualization.getChart());
        window.setWidget(widgets);
        window.center();
    }

    public void onReify(final String name)
    {

        if(preview!=null)
            preview.hide();

        kernel.reify(name, new AsyncCallback<Widget>() {
            @Override
            public void onFailure(Throwable throwable) {
                Console.error("Reification failed", throwable.getMessage());
            }

            @Override
            public void onSuccess(Widget widget) {

                doPreview(widget, name);
                kernel.activate();
                kernel.reset();
            }
        });
    }

    private void doPreview(Widget widget, String name) {

        preview = new DefaultWindow("Preview: "+ name);
        preview.setWidth(640);
        preview.setHeight(480);

        preview.setWidget(widget);
        preview.center();
    }

    public void onActivate()
    {
        kernel.activate();
    }

    public void onResetDialog() {
        kernel.reset();
    }

    public void onPassivate() {
        kernel.passivate();
    }

    @Override
    public void onSave(String xml) {

        if(null==selectedDialog) return;

        try {
            DialogXML parser = new DialogXML();

            // validation
            Dialog dialog = parser.unmarshall(xml);

            final Document document = parser.marshall(dialog);

            vfs.save(selectedDialog, ModelEditor.formatXml(document.toString()), new SimpleCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    Console.info("Successfully saved "+selectedDialog.getName());
                    getView().updateFile(selectedDialog.getName(), document.toString());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            Console.error("Failed to save dialog", e.getMessage());
        }

    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }

    public void loadDir(final Entry dir, boolean clearHistory) {

        if(clearHistory)
            getView().clearHistory();

        vfs.listEntries(
                dir,
                new SimpleCallback<List<Entry>>() {
                    @Override
                    public void onSuccess(List<Entry> result) {

                        getView().updateDirectory(dir, result);

                    }
                });
    }

    public void loadFile(final Entry selection) {
        this.selectedDialog = selection;
        vfs.load(selection, new SimpleCallback<String>() {
            @Override
            public void onSuccess(String result) {
                getView().updateFile(selection.getName(), result);
            }
        });
    }
}