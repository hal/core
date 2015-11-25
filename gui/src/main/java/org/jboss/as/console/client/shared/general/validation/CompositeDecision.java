package org.jboss.as.console.client.shared.general.validation;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.general.model.Interface;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The decision tree to run interface constraint validations.
 *
 * @author Heiko Braun
 * @date 11/16/11
 */
public class CompositeDecision extends AbstractValidationStep<Interface>{

    private AddressValidation addressValidation = new AddressValidation();
    private NicValidation nicValidation = new NicValidation();
    private LoopbackValidation loopbackValidation = new LoopbackValidation ();
    private OtherConstraintsValidation otherValidation = new OtherConstraintsValidation();

    private List<String> detailMessages = new LinkedList<String>();

    public List<String> getDetailMessages() {
        return detailMessages;
    }

    @Override
    public void setLog(DecisionTree.DecisionLog log) {
        super.setLog(log);
        addressValidation.setLog(log);
        nicValidation.setLog(log);
        loopbackValidation.setLog(log);
        otherValidation.setLog(log);
    }

    @Override
    public ValidationResult validate(Interface entity, Map<String, Object> changedValues) {
        ValidationResult result = super.validate(entity, changedValues);
        result.getMessages().addAll(detailMessages);
        return result;
    }

    @Override
    public boolean doesApplyTo(Interface entity, Map<String, Object> changedValues) {
        return true;
    }

    @Override
    protected DecisionTree<Interface> buildDecisionTree(final Interface entity, final Map<String, Object> changedValues) {


        //System.out.println(">> "+changedValues);

        DecisionTree<Interface> tree = new DecisionTree<Interface>(entity);
        tree.createRoot(1, Console.CONSTANTS.anyChanges(), new Decision<Interface>() {
            @Override
            public boolean evaluate(Interface entity) {
                return !changedValues.isEmpty();
            }
        });

        tree.no(1, 2, Console.CONSTANTS.noChanges(), SUCCESS);
        tree.yes(1, 3, Console.CONSTANTS.anyAddressModified(), new Decision<Interface>() {
            @Override
            public boolean evaluate(Interface entity) {
                return addressValidation.doesApplyTo(entity, changedValues);
            }
        });

        // --------------------------------

        tree.yes(3, 4, Console.CONSTANTS.validAddressCriteria(), new Decision<Interface>() {
            @Override
            public boolean evaluate(Interface entity) {
                ValidationResult result = addressValidation.validate(entity, changedValues);
                detailMessages.addAll(result.getMessages());
                return result.isValid();
            }
        });
        tree.no(3, 5, Console.CONSTANTS.anyNicModified(), new Decision<Interface>() {
            @Override
            public boolean evaluate(Interface entity) {
                return nicValidation.doesApplyTo(entity, changedValues);
            }
        });

        tree.yes(4, 6, Console.CONSTANTS.addressCriteriaIsValid(), SUCCESS);
        tree.no(4, 7, Console.CONSTANTS.invalidAddressCriteria(), FAILURE);

        // --------------------------------

        tree.yes(5, 8, Console.CONSTANTS.validNicConstraints(), new Decision<Interface>() {
            @Override
            public boolean evaluate(Interface entity) {
                ValidationResult result = nicValidation.validate(entity, changedValues);
                detailMessages.addAll(result.getMessages());
                return result.isValid();
            }
        });
        tree.no(5, 9, Console.CONSTANTS.anyLoopbackModified(), new Decision<Interface>() {
            @Override
            public boolean evaluate(Interface entity) {
                return loopbackValidation.doesApplyTo(entity, changedValues);
            }
        });


        tree.yes(8, 10, Console.CONSTANTS.nicCriteriaIsValid(), SUCCESS);
        tree.no(8, 11, Console.CONSTANTS.invalidNicCriteria(), FAILURE);

        // --------------------------------

        tree.yes(9, 12, Console.CONSTANTS.validLoopbackCriteria(), new Decision<Interface>() {
            @Override
            public boolean evaluate(Interface entity) {
                ValidationResult result = loopbackValidation.validate(entity, changedValues);
                detailMessages.addAll(result.getMessages());
                return result.isValid();
            }
        });
        tree.no(9, 13, Console.CONSTANTS.otherConstraintsModified(), new Decision<Interface>() {
            @Override
            public boolean evaluate(Interface entity) {
                return otherValidation.doesApplyTo(entity, changedValues);
            }
        });


        tree.yes(12, 14, Console.CONSTANTS.loopbackCriteriaIsValid(), SUCCESS);
        tree.no(12, 15, Console.CONSTANTS.invalidLoopbackCriteria(), FAILURE);

        // --------------------------------

        tree.yes(13, 16, Console.CONSTANTS.validOtherConstraints(), new Decision<Interface>() {
            @Override
            public boolean evaluate(Interface entity) {
                ValidationResult result = otherValidation.validate(entity, changedValues);
                detailMessages.addAll(result.getMessages());
                return result.isValid();
            }
        });
        tree.no(13, 17, Console.CONSTANTS.interfaces_err_not_set(), FAILURE);

        tree.yes(16, 18, Console.CONSTANTS.otherCriteriaIsValid(), SUCCESS);
        tree.no(16, 19, Console.CONSTANTS.invalidOtherCriteria(), FAILURE);

        return tree;
    }

}
