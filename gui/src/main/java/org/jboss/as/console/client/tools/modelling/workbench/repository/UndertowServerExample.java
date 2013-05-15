package org.jboss.as.console.client.tools.modelling.workbench.repository;

import org.jboss.as.console.mbui.model.StereoTypes;
import org.jboss.as.console.mbui.model.mapping.DMRMapping;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.mapping.Mapping;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.Select;
import org.useware.kernel.model.structure.TemporalOperator;
import org.useware.kernel.model.structure.builder.Builder;

import static org.jboss.as.console.mbui.model.StereoTypes.Form;
import static org.jboss.as.console.mbui.model.StereoTypes.Pages;
import static org.useware.kernel.model.structure.TemporalOperator.Choice;
import static org.useware.kernel.model.structure.TemporalOperator.Concurrency;

/**
 * @author Heiko Braun
 * @date 5/14/13
 */
public class UndertowServerExample implements Sample {


    private final Dialog dialog;

    public UndertowServerExample() {
        this.dialog = build();
    }

    public Dialog build()
    {
        String ns = "org.jboss.undertow.server";

        // entities
        Mapping global = new DMRMapping()
                .setAddress("/{selected.profile}/subsystem=undertow/server=default-server");


        Container overview = new Container(ns, "httpServer", "Server", TemporalOperator.Choice, StereoTypes.EditorPanel);


        Container attributes = new Container(ns, "server#attributes", "Attributes", Form);
        Mapping attributesMapping = new DMRMapping()
                .addAttributes("default-host", "servlet-container");

        Container listener = new Container(ns, "listener", "Listener", Choice, Pages);

        Container http = new Container(ns, "listener#http", "HTTP", Concurrency);
        Container ajp = new Container(ns, "listener#ajp", "AJP", Concurrency);
        Container https = new Container(ns, "listener#https", "HTTPS", Concurrency);


        // structure & mapping
        InteractionUnit root = new Builder()
                .start(overview)
                    .mappedBy(global)
                        //.add(attributes).mappedBy(attributesMapping)

                        // handler section
                    .start(listener)
                        .start(http)
                            .add(new Select(ns, "httpListener", "HTTPListenerSelection"))
                                .mappedBy(
                                    new DMRMapping()
                                        .setAddress("/{selected.profile}/subsystem=undertow/server=default-server/http-listener=*")
                                        .addAttributes("entity.key", "enabled")
                                )

                                .add(new Container(ns, "undertow#httpListenerAttributes", "Attributes", Form))
                                    .mappedBy(new DMRMapping()
                                        .setAddress("/{selected.profile}/subsystem=undertow/server=default-server/http-listener={selected.entity}")
                                        .addAttributes(
                                                "worker", "enabled",
                                                "socket-binding", "buffer-pool"
                                        )
                                    )
                        .end()

                        .start(ajp)
                            .add(new Select(ns, "ajpListener", "AJPListenerSelection"))
                                .mappedBy(
                                    new DMRMapping()
                                        .setAddress("/{selected.profile}/subsystem=undertow/server=default-server/ajp-listener=*")
                                        .addAttributes("entity.key", "enabled")
                                    )

                             .add(new Container(ns, "undertow#ajpListenerAttributes", "Attributes", Form))
                                .mappedBy(new DMRMapping()
                                    .setAddress("/{selected.profile}/subsystem=undertow/server=default-server/ajp-listener={selected.entity}")
                                    .addAttributes(
                                        "worker", "enabled",
                                        "socket-binding", "buffer-pool"
                                        )
                                    )
                        .end()
                        .start(https)
                            .add(new Select(ns, "httpsListener", "HTTPSListenerSelection"))
                                .mappedBy(
                                    new DMRMapping()
                                        .setAddress("/{selected.profile}/subsystem=undertow/server=default-server/https-listener=*")
                                        .addAttributes("entity.key", "enabled")
                                        )

                            .add(new Container(ns, "undertow#httpsListenerAttributes", "Attributes", Form))
                                .mappedBy(
                                        new DMRMapping()
                                        .setAddress("/{selected.profile}/subsystem=undertow/server=default-server/https-listener={selected.entity}")
                                        .addAttributes(
                                        "worker", "enabled",
                                        "socket-binding", "buffer-pool",
                                        "security-realm"
                                        )
                                )
                        .end()
                    .end()

                .end()
        .build();

        Dialog dialog = new Dialog(QName.valueOf("org.jboss.as:http-server"), root);
        return dialog;
    }

    @Override
    public String getName() {
        return "HTTP Server";
    }

    @Override
    public Dialog getDialog() {
        return dialog;
    }
}
