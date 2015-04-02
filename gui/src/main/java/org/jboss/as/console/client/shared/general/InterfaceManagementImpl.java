package org.jboss.as.console.client.shared.general;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.general.model.Interface;
import org.jboss.as.console.client.shared.general.validation.CompositeDecision;
import org.jboss.as.console.client.shared.general.validation.DecisionTree;
import org.jboss.as.console.client.shared.general.validation.ValidationResult;
import org.jboss.as.console.client.shared.general.wizard.NewInterfaceWizard;
import org.jboss.as.console.client.widgets.forms.AddressBinding;
import org.jboss.as.console.client.widgets.forms.BeanMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelNodeUtil;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 11/17/11
 */
public class InterfaceManagementImpl implements InterfaceManagement {

    private DispatchAsync dispatcher;
    private DefaultWindow window;
    private EntityAdapter<Interface> entityAdapter;
    private BeanMetaData beanMetaData;
    private InterfaceManagement.Callback callback;

    public InterfaceManagementImpl(
            DispatchAsync dispatcher,
            EntityAdapter<Interface> entityAdapter,
            BeanMetaData beanMetaData) {
        this.dispatcher = dispatcher;
        this.entityAdapter = entityAdapter;
        this.beanMetaData = beanMetaData;
    }

    @Override
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void closeDialoge() {
        window.hide();
    }

    @Override
    public void launchNewInterfaceDialogue() {
        window = new DefaultWindow(Console.MESSAGES.createTitle("Network Interface"));
        window.setWidth(480);
        window.setHeight(360);

        window.trapWidget(
                new NewInterfaceWizard(this).asWidget()
        );

        window.setGlassEnabled(true);
        window.center();
    }

    @Override
    public void createNewInterface(final Interface entity) {
        window.hide();

        ModelNode operation = entityAdapter.fromEntity(entity);
        operation.get(ADDRESS).set(callback.getBaseAddress());
        operation.get(ADDRESS).add("interface", entity.getName());
        operation.get(OP).set(ADD);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.addingFailed("Network Interface"), response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.added("Network Interface"));
                }
                loadInterfaces();
            }
        });
    }

    @Override
    public void onRemoveInterface(final Interface entity) {

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(callback.getBaseAddress());
        operation.get(ADDRESS).add("interface", entity.getName());
        operation.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();

                System.out.println(response);
                if(ModelNodeUtil.indicatesSuccess(response))
                {
                    Console.info(Console.MESSAGES.deleted("Network Interface"));
                }
                else
                {
                    Console.error(Console.MESSAGES.deletionFailed("Network Interface"),
                            response.getFailureDescription());
                }

                loadInterfaces();
            }
        });
    }

    public void loadInterfaces() {
        callback.loadInterfaces();
    }

    @Override
    public ValidationResult validateInterfaceConstraints(final Interface entity, Map<String, Object> changeset) {

        //long s0 = System.currentTimeMillis();

        AutoBean<Interface> autoBean = AutoBeanUtils.getAutoBean(entity);
        Map<String, Object> properties = AutoBeanUtils.getAllProperties(autoBean);

        final List<String> decisions = new LinkedList<String>();

        DecisionTree.DecisionLog log = new DecisionTree.DecisionLog() {
            int index = 0;
            @Override
            public void append(String message) {
                index++;
                decisions.add("["+index+"] " + message);
            }
        };

        CompositeDecision decisionTree = new CompositeDecision();
        decisionTree.setLog(log);

        ValidationResult validation = decisionTree.validate(entity, changeset);

        // dump log
        StringBuilder sb = new StringBuilder();
        for(String s : decisions)
            sb.append(s).append(" \n");
        System.out.println(sb.toString());

        //System.out.println("** Exec time: "+(System.currentTimeMillis()-s0)+" ms **");
        return validation;
    }

    @Override
    public void onSaveInterface(final Interface entity, Map<String, Object> changeset) {

        doPersistChanges(entity, changeset);
    }

    private void doPersistChanges(final Interface entity, Map<String, Object> changeset) {
        AddressBinding addressBinding = beanMetaData.getAddress();
        ModelNode address = addressBinding.asResource(callback.getBaseAddress(), entity.getName());
        ModelNode operation = entityAdapter.fromChangeset(changeset, address);

        //System.out.println(operation);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                //System.out.println(response);

                if (ModelNodeUtil.indicatesSuccess(response)) {
                    Console.info(Console.MESSAGES.modified("Network Interface"));
                } else {
                    Console.error(Console.MESSAGES.modificationFailed("Network Interface"),
                            response.getFailureDescription());
                }
                loadInterfaces();
            }
        });
    }

    public static boolean isSet(String value)
    {
        return value!=null && !value.isEmpty();
    }
}
