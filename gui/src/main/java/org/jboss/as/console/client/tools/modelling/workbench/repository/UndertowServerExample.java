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
import org.useware.kernel.model.structure.Trigger;
import org.useware.kernel.model.structure.builder.Builder;

import static org.jboss.as.console.mbui.model.StereoTypes.Form;
import static org.jboss.as.console.mbui.model.StereoTypes.Pages;
import static org.jboss.as.console.mbui.model.StereoTypes.Toolstrip;
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
        DMRMapping httpListenerCollection = new DMRMapping()
                .setAddress("/{selected.profile}/subsystem=undertow/server=default-server/http-listener=*")
                .addAttributes("entity.key", "enabled");

        DMRMapping singleHttpListener = new DMRMapping()
                        .setAddress("/{selected.profile}/subsystem=undertow/server=default-server/http-listener={selected.entity}");

        DMRMapping httpListenerTable = new DMRMapping()
                        .addAttributes("entity.key", "enabled");



        DMRMapping ajpListenerCollection = new DMRMapping()
                       .setAddress("/{selected.profile}/subsystem=undertow/server=default-server/ajp-listener=*")
                       .addAttributes("entity.key", "enabled");

        DMRMapping singleAjpListener = new DMRMapping()
                .setAddress("/{selected.profile}/subsystem=undertow/server=default-server/ajp-listener={selected.entity}");

        DMRMapping ajpListenerTable = new DMRMapping()
                .addAttributes("entity.key", "enabled");



        DMRMapping httpsListenerCollection = new DMRMapping()
                .setAddress("/{selected.profile}/subsystem=undertow/server=default-server/https-listener=*")
                .addAttributes("entity.key", "enabled");

        DMRMapping singleHTTPSListener = new DMRMapping()
                .setAddress("/{selected.profile}/subsystem=undertow/server=default-server/https-listener={selected.entity}");

        DMRMapping httpsListenerTable = new DMRMapping()
                .addAttributes("entity.key", "enabled", "security-realm");


        InteractionUnit root = new Builder()
                .start(overview)

                    // handler section
                    .start(listener)

                       // ------------------- HTTP --------------------
                        .start(http)
                            .mappedBy(httpListenerCollection)

                                .start(new Container<StereoTypes>(ns, "httptools", "Tools", Toolstrip))
                                           .mappedBy(singleHttpListener)
                                           .add(new Trigger(
                                                   QName.valueOf("org.jboss.httpListener:add"),
                                                   QName.valueOf("org.jboss.as:resource-operation#add"),
                                                   "Add"))
                                                   .mappedBy(httpListenerCollection)

                                           .add(new Trigger(
                                                   QName.valueOf("org.jboss.httpListener:remove"),
                                                   QName.valueOf("org.jboss.as:resource-operation#remove"),
                                                   "Remove"))
                                .end()

                            .add(new Select(ns, "httpListener", "HTTPListenerSelection"))
                                .mappedBy(httpListenerTable)

                                .start(new Container(ns, "undertow#httpListenerConfig", "httpConfig", Choice))
                                    .mappedBy(singleHttpListener)
                                    .add(new Container(ns, "undertow#httpListenerAttributes", "Attributes", Form))
                                        .mappedBy(new DMRMapping()
                                            .addAttributes(
                                                    "worker", "enabled",
                                                    "socket-binding", "buffer-pool"
                                            )
                                       )
                                .end()
                        .end()

                    // ------------------- AJP --------------------
                        .start(ajp)
                            .mappedBy(ajpListenerCollection)


                                .start(new Container<StereoTypes>(ns, "ajptools", "Tools", Toolstrip))
                                    .mappedBy(singleAjpListener)
                                        .add(new Trigger(
                                            QName.valueOf("org.jboss.ajpListener:add"),
                                            QName.valueOf("org.jboss.as:resource-operation#add"),
                                            "Add"))
                                            .mappedBy(ajpListenerCollection)

                                        .add(new Trigger(
                                            QName.valueOf("org.jboss.ajpListener:remove"),
                                            QName.valueOf("org.jboss.as:resource-operation#remove"),
                                            "Remove"))
                                .end()

                            .add(new Select(ns, "ajpListener", "AJPListenerSelection"))
                                .mappedBy(ajpListenerTable)

                             .start(new Container(ns, "undertow#ajpListenerConfig", "ajpConfig", Choice))
                                .mappedBy(singleAjpListener)
                                .add(new Container(ns, "undertow#ajpListenerAttributes", "Attributes", Form))
                                    .mappedBy(new DMRMapping()
                                        .addAttributes(
                                                "worker", "enabled",
                                                "socket-binding", "buffer-pool"
                                        )
                                    )
                            .end()
                        .end()

                    // ------------------- HTTPS--------------------
                        .start(https)
                            .mappedBy(httpsListenerCollection)


                                    .start(new Container<StereoTypes>(ns, "httpstools", "Tools", Toolstrip))
                                        .mappedBy(singleHTTPSListener)
                                            .add(new Trigger(
                                                QName.valueOf("org.jboss.httpsListener:add"),
                                                QName.valueOf("org.jboss.as:resource-operation#add"),
                                                "Add"))
                                                .mappedBy(httpsListenerCollection)

                                            .add(new Trigger(
                                                QName.valueOf("org.jboss.httpsListener:remove"),
                                                QName.valueOf("org.jboss.as:resource-operation#remove"),
                                                "Remove"))
                                    .end()

                            .add(new Select(ns, "httpsListener", "HTTPSListenerSelection"))
                                .mappedBy(httpsListenerTable)

                                .start(new Container(ns, "undertow#httpsListenerConfig", "httpsConfig", Choice))
                                          .mappedBy(singleHTTPSListener)
                                          .add(new Container(ns, "undertow#httpsListenerAttributes", "Attributes", Form))
                                            .mappedBy(new DMRMapping()
                                            .addAttributes(
                                            "worker", "enabled",
                                             "socket-binding", "buffer-pool", "security-realm"
                                             )
                                             )
                                .end()
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
