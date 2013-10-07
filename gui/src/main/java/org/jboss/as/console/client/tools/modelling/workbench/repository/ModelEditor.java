package org.jboss.as.console.client.tools.modelling.workbench.repository;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.XMLParser;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditor;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorMode;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorTheme;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;


/**
 * @author Heiko Braun
 * @date 10/6/13
 */
public class ModelEditor {

    private AceEditor editor = new AceEditor();
    private String text;
    private ContentHeaderLabel header;
    private String dialogName;
    private MenuBar menu;

    Widget asWidget() {


        final VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("fill-layout");

        // ---

        MenuBar file = new MenuBar(true);
        file.addItem("save", new Command() {
            @Override
            public void execute() {

            }
        });

        MenuBar view = new MenuBar(true);
        view.addItem("Full Screen", new Command() {
            @Override
            public void execute() {

            }
        });

        MenuBar code = new MenuBar(true);
        code.addItem("Reformat", new Command() {
            @Override
            public void execute() {
                String text = editor.getText();
                Document document = null;
                try {
                    document = XMLParser.parse(text);
                    editor.setText(formatXml(document.toString()));
                } catch (Exception e) {
                    Console.error("Failed to parse document", e.getMessage());
                }

            }
        });

        menu = new MenuBar();
        menu.addItem("File", file);
        menu.addItem("View", view);
        menu.addItem("Code", code);

        panel.add(menu);

        // ---
        panel.add(editor);

        editor.getElement().setAttribute("style", "border:1px solid #cccccc");
        editor.addAttachHandler(new AttachEvent.Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                Scheduler.get().scheduleDeferred(
                        new Scheduler.ScheduledCommand() {
                            @Override
                            public void execute() {

                                updateEditorConstraints(panel);

                                editor.startEditor();
                                editor.setMode(AceEditorMode.XML);
                                editor.setTheme(AceEditorTheme.TWILIGHT);

                            }
                        }
                );
            }
        });




        Window.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent event) {
                updateEditorConstraints(panel);
            }
        });

        // --

        header = new ContentHeaderLabel("Dialog:");

        SimpleLayout editorLayout = new SimpleLayout()
                .setPlain(true)
                .setHeadlineWidget(header)
                .setDescription("")
                .addContent("XML", panel);


        // --

        DefaultTabLayoutPanel tabLayoutpanel = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabLayoutpanel.addStyleName("default-tabpanel");
        tabLayoutpanel.add(editorLayout.build(), "Editor", true);

        tabLayoutpanel.selectTab(0);

        return tabLayoutpanel;

    }

    private void updateEditorConstraints(final VerticalPanel panel) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                int parentWidth = panel.getElement().getOffsetWidth();
                int editorWidth = parentWidth - 35;
                editor.setWidth(editorWidth +"px");
                editor.setHeight("480px");
                menu.setWidth((editorWidth)-7+"px");
            }
        });
    }

    public void setText(String text) {
        String formatted = null;
        try {
            formatted = formatXml(text);
            editor.setText(formatted);
        } catch (Throwable e) {
            Console.error("Failed to format XML", e.getMessage());
        }

    }

    public static native String formatXml(String xml) /*-{


            var formatted = '';
            var reg = /(>)(<)(\/*)/g;
            xml = xml.replace(reg, '$1\r\n$2$3');
            var pad = 0;

            var lines = xml.split('\r\n');
            for (var i = 0; i < lines.length; i++) {
                node = lines[i];

                var indent = 0;
                if (node.match( /.+<\/\w[^>]*>$/ )) {
                    indent = 0;
                } else if (node.match( /^<\/\w/ )) {
                    if (pad != 0) {
                        pad -= 1;
                    }
                } else if (node.match( /^<\w[^>]*[^\/]>.*$/ )) {
                    indent = 1;
                } else {
                    indent = 0;
                }

                var padding = '';
                for (var p = 0; p < pad; p++) {
                    padding += '\t';
                }

                formatted += padding + node + '\r\n';
                pad += indent;

            }
            return formatted;


    }-*/;

    public void setDialogName(String dialogName) {
        header.setText("Dialog: "+dialogName);
    }
}
