package org.jboss.as.console.client.tools.modelling.workbench.repository;

import org.jboss.as.console.mbui.model.StereoTypes;
import org.jboss.as.console.mbui.model.mapping.DMRMapping;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.mapping.Mapping;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.TemporalOperator;
import org.useware.kernel.model.structure.builder.Builder;

import static org.jboss.as.console.mbui.model.StereoTypes.Form;
import static org.useware.kernel.model.structure.TemporalOperator.Concurrency;

/**
 * @author Heiko Braun
 * @date 5/14/13
 */
public class ServletContainerExample implements Sample {


    private final Dialog dialog;

    public ServletContainerExample() {
        this.dialog = build();
    }

    public Dialog build()
    {
        String ns = "org.jboss.undertow.servlet";

        // entities
        Mapping global = new DMRMapping()
                .setAddress("/{selected.profile}/subsystem=undertow/servlet-container=default/setting=jsp");


        Container overview = new Container(ns, "servletContainer", "Servlet Container", TemporalOperator.Choice, StereoTypes.EditorPanel);


        Container attributes = new Container(ns, "servletContainer#attributes", "JSP Settings", Form);
        Mapping attributesMapping = new DMRMapping()
                .addAttributes(
                        "trim-spaces", "smap",
                        "development", "keep-generated",
                        "recompile-on-fail","check-interval",
                        "scratch-dir","modification-test-interval",
                        "display-source-fragment","error-on-use-bean-invalid-class-attribute",
                        "java-encoding","tag-pooling",
                        "generate-strings-as-char-arrays","target-vm",
                        "x-powered-by","dump-smap",
                        "mapped-file","disabled",
                        "source-vm"

                );

        // structure & mapping
        InteractionUnit root = new Builder()
                .start(overview)
                    .mappedBy(global)
                    .add(attributes).mappedBy(attributesMapping)
                .end()
        .build();

        Dialog dialog = new Dialog(QName.valueOf("org.jboss.as:servlet-container"), root);
        return dialog;
    }

    @Override
    public String getName() {
        return "Servlet Container";
    }

    @Override
    public Dialog getDialog() {
        return dialog;
    }
}
