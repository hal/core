package org.jboss.as.console.client.tools.modelling.workbench.repository;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditor;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorMode;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorTheme;


/**
 * @author Heiko Braun
 * @date 10/6/13
 */
public class ModelEditor {

    private AceEditor editor = new AceEditor();
    private String text;

    Widget asWidget() {

        final VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("fill-layout");

        panel.add(editor);

        editor.addAttachHandler(new AttachEvent.Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                Scheduler.get().scheduleDeferred(
                        new Scheduler.ScheduledCommand() {
                            @Override
                            public void execute() {

                                editor.setWidth(panel.getElement().getOffsetWidth()+"px");
                                editor.setHeight("480px");

                                editor.startEditor();
                                editor.setMode(AceEditorMode.XML);
                                editor.setTheme(AceEditorTheme.TWILIGHT);

                            }
                        }
                );
            }
        });

        return panel;

    }

    public void setText(String text) {
        editor.setText(text);
    }
}
