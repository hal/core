/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.v3.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.dmr.client.ModelNode;

import static org.jboss.dmr.client.ModelDescriptionConstants.PATH;

/**
 * @author Claudio Miranda
 */
public class DeploymentBrowseContentView extends SuspendableViewImpl implements DeploymentBrowseContentPresenter.MyView {

    private VerticalPanel panel = new VerticalPanel();
    private DeploymentBrowseContentPresenter presenter;

    static java.util.logging.Logger _log = java.util.logging.Logger.getLogger("org.jboss");

    @Inject
    public DeploymentBrowseContentView() { }

    @Override
    public Widget createWidget() {

        ScrollPanel scroller = new ScrollPanel();
        panel.setStyleName("browse-content");
        scroller.add(panel);
        return scroller.asWidget();
    }

    @Override
    public void browseContent(List<ModelNode> contentItems) {

        panel.clear();
        List<ModelNode> contents = new ArrayList<>(contentItems);
        Collections.sort(contents, new Comparator<ModelNode>() {
            @Override
            public int compare(final ModelNode o1, final ModelNode o2) {
                return o1.get(PATH).asString().compareTo(o2.get(PATH).asString());
            }
        });
        
        for (ModelNode file: contents) {
            String contentFile = file.get(PATH).asString();
            boolean isDirectory = file.get("directory").asBoolean();
            long fileLength = 0;
            if (isDirectory) 
                continue;
            else
                fileLength = file.get("file-size").asLong();
            
            String fileLengthDisplay = "";
            if (fileLength > 0) 
                fileLengthDisplay = formatFileUnits(fileLength);
            
            Anchor anchor = new Anchor(contentFile + " | " + fileLengthDisplay);
            anchor.setEnabled(false);
            anchor.setTitle("Download not implemented yet.");
            //anchor.addClickHandler(clickEvent -> {
            //    Anchor sourceAnchor = (Anchor) clickEvent.getSource();
            //    String filepath = sourceAnchor.getText();
            //    // retrieve only the path portion
            //    filepath = filepath.substring(0, filepath.lastIndexOf('|') - 1);
            //    //_log.info(" click file filepath: '" + filepath +  "'");
            //    presenter.downloadFile(filepath);
            //});
                    
            panel.add(anchor);
        }
    }

    public String formatFileUnits(long length) {
        if (length <= 0) 
            return "0";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(length) / Math.log10(1024));
        return NumberFormat.getFormat("#,##0.#").format(length/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    @Override
    public void setPresenter(DeploymentBrowseContentPresenter presenter) {
        this.presenter = presenter;
    }
}
