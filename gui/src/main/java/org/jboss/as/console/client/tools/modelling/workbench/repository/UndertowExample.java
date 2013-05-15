package org.jboss.as.console.client.tools.modelling.workbench.repository;

import org.jboss.as.console.mbui.model.StereoTypes;
import org.jboss.as.console.mbui.model.mapping.DMRMapping;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.mapping.Mapping;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.Select;
import org.useware.kernel.model.structure.builder.Builder;

import static org.jboss.as.console.mbui.model.StereoTypes.Form;
import static org.jboss.as.console.mbui.model.StereoTypes.Pages;
import static org.useware.kernel.model.structure.TemporalOperator.Choice;
import static org.useware.kernel.model.structure.TemporalOperator.Concurrency;

/**
 * @author Heiko Braun
 * @date 5/14/13
 */
public class UndertowExample implements Sample {


    private final Dialog dialog;

    public UndertowExample() {
        this.dialog = build();
    }

    public Dialog build()
    {
        String ns = "org.jboss.undertow";

        // entities
        Mapping global = new DMRMapping()
                .setAddress("/{selected.profile}/subsystem=undertow");


        Container overview = new Container(ns, "undertow", "Undertow Subsytem", Choice, StereoTypes.EditorPanel);

        Container handler = new Container(ns, "handler", "Handler", Choice, Pages);
        Container filter = new Container(ns, "filter", "Filter", Concurrency);
        Container errorHandler = new Container(ns, "errorHandler", "Error Handler", Concurrency);

        Container fileHandler = new Container(ns, "undertow#fileHandler", "File Handler", Concurrency);

        /*Container attributes = new Container(ns, "undertow#basicAttributes", "Attributes",Form);
        Mapping basicAttributesMapping = new DMRMapping()
                .addAttributes(
                        "default-server", "instance-id",
                        "default-virtual-host", "default-servlet-container"
                );*/

        // structure & mapping
        InteractionUnit root = new Builder()
                .start(overview)
                    .mappedBy(global)

                    // handler section
                    .start(handler)
                        .start(fileHandler)
                            .add(new Select(ns, "fileHandler", "FileHandlerSelection"))
                               .mappedBy(
                                       new DMRMapping()
                                        .setAddress("/{selected.profile}/subsystem=undertow/configuration=handler/file=*")
                                        .addAttributes("entity.key", "path")
                                        )
                            .add(new Container(ns, "undertow#fileAttributes", "Attributes",Form))
                                .mappedBy(new DMRMapping()
                                    .setAddress("/{selected.profile}/subsystem=undertow/configuration=handler/file={selected.entity}")
                                    .addAttributes("path", "directory-listing", "cache-buffer-size", "cache-buffers")
                                   )
                        .end()
                    .end()
                    .start(filter).end()
                    .start(errorHandler).end()
                .end()
        .build();

        Dialog dialog = new Dialog(QName.valueOf("org.jboss.as:undertow-subsystem"), root);
        return dialog;
    }

    @Override
    public String getName() {
        return "Undertow";
    }

    @Override
    public Dialog getDialog() {
        return dialog;
    }
}
