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
        Container filter = new Container(ns, "filter", "Filter",  Choice, Pages);
        Container errorHandler = new Container(ns, "errorHandler", "Error Handler", Choice, Pages);
        Container fileHandler = new Container(ns, "undertow#fileHandler", "File Handler", Concurrency);


        // structure & mapping
        InteractionUnit root = new Builder()
                .start(overview)
                    .mappedBy(global)

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
                                   )
                        .end()
                    .end()


                    /*.start(errorHandler)
                        .start(new Container(ns, "undertow#error", "Error Pages", Concurrency))
                            .add(new Select(ns, "errorHandler", "ErrorHandlerSelection"))
                                .mappedBy(
                                    new DMRMapping()
                                        .setAddress("/{selected.profile}/subsystem=undertow/configuration=error-handler/error-page=*")
                                        .addAttributes("entity.key", "path")
                                )
                            .add(new Container(ns, "undertow#errorHandlerAttributes", "Attributes",Form))
                                .mappedBy(new DMRMapping()
                                    .setAddress("/{selected.profile}/subsystem=undertow/configuration=error-handler/error-page={selected.entity}")
                                )
                        .end()
                    .end()  */


                    .start(filter)
                        .start(new Container(ns, "undertow#basicAuth", "Basic Auth", Concurrency))
                            .add(new Select(ns, "undertow#basicAuthSelection", "BasicAuthSelection"))
                               .mappedBy(
                                   new DMRMapping()
                                    .setAddress("/{selected.profile}/subsystem=undertow/configuration=filter/basic-auth=*")
                                    .addAttributes("entity.key")
                                    )
                            .add(new Container(ns, "undertow#filterAuthAttributes", "Attributes",Form))
                                .mappedBy(new DMRMapping()
                                    .setAddress("/{selected.profile}/subsystem=undertow/configuration=filter/basic-auth={selected.entity}")
                                   )
                        .end()

                        .start(new Container(ns, "undertow#connectionLimit", "Connection Limit", Concurrency))
                            .add(new Select(ns, "undertow#connectionLimitSelection", "connectionLimitSelection"))
                               .mappedBy(
                                   new DMRMapping()
                                        .setAddress("/{selected.profile}/subsystem=undertow/configuration=filter/connection-limit=*")
                                        .addAttributes("entity.key")
                                    )
                            .add(new Container(ns, "undertow#connectionLimitAttributes", "Attributes",Form))
                                .mappedBy(new DMRMapping()
                                  .setAddress("/{selected.profile}/subsystem=undertow/configuration=filter/connection-limit={selected.entity}")
                               )
                        .end()

                    .end()


                .end()
        .build();

        Dialog dialog = new Dialog(QName.valueOf("org.jboss.as:undertow-subsystem"), root);
        return dialog;
    }

    @Override
    public String getName() {
        return "undertow";
    }

    @Override
    public Dialog getDialog() {
        return dialog;
    }
}
