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
package org.jboss.as.console.client.shared.runtime.logviewer;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import org.jboss.as.console.client.shared.runtime.logviewer.actions.ChangePageSize;
import org.jboss.gwt.circuit.Action;

import static com.google.gwt.dom.client.Style.Unit.PX;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * A visual indicator for the navigation inside a {@link org.jboss.as.console.client.shared.runtime.logviewer.LogFile}
 *
 * @author Harald Pehl
 */
public class LogFileIndicator extends Composite {

    public static final String STYLE_NAME = "hal-LogFileIndicator";

    private final FlowPanel indicator;

    private int bytesConsumed;
    private int consumedSnapshot;
    private int lastSkipped;
    private double ratio;

    public LogFileIndicator() {
        FlowPanel container = new FlowPanel();
        this.indicator = new FlowPanel();

        container.add(indicator);
        initWidget(container);

        setStyleName(STYLE_NAME);
        indicator.setStyleName(STYLE_NAME + "__indicator");
        indicator.getElement().getStyle().setTop(0, PX);
    }

    public void refresh(LogFile logFile, Action action) {
        int bytesPerFile = logFile.getFileSize();
        int containerHeight = getElement().getParentElement().getClientHeight();
        boolean resized = lastSkipped == logFile.getSkipped() && action instanceof ChangePageSize;

        if (logFile.getPosition() == Position.HEAD) {
            bytesConsumed = consumedSnapshot = logFile.getNumBytes();
        } else if (logFile.getPosition() == Position.TAIL) {
            bytesConsumed = consumedSnapshot = bytesPerFile;

        } else {
            if (!resized) {
                consumedSnapshot = bytesConsumed;
            }

            if (logFile.getSkipped() > lastSkipped) {
                // skipped increased
                if (logFile.getReadFrom() == Position.HEAD) {
                    // next
                    bytesConsumed += logFile.getNumBytes();
                } else {
                    // prev
                    bytesConsumed -= logFile.getNumBytes();
                }

            } else if (logFile.getSkipped() < lastSkipped) {
                // skipped decreased
                if (logFile.getReadFrom() == Position.HEAD) {
                    // prev
                    bytesConsumed -= logFile.getNumBytes();
                } else {
                    // next
                    bytesConsumed += logFile.getNumBytes();
                }

            } else if (resized) {
                if (logFile.getReadFrom() == Position.HEAD) {
                    bytesConsumed = consumedSnapshot + logFile.getNumBytes();
                } else {
                    bytesConsumed = consumedSnapshot - logFile.getNumBytes();
                }
            }
        }

        ratio = (100.0 / bytesPerFile) * bytesConsumed;
        ratio = max(0.0, ratio);
        ratio = min(100.0, ratio);

        double indicatorHeight = (containerHeight / 100.0) * ratio;
        indicatorHeight = max(5, indicatorHeight);
        indicatorHeight = min(containerHeight, indicatorHeight);
        indicator.getElement().getStyle().setHeight(indicatorHeight, PX);

        lastSkipped = logFile.getSkipped();
    }

    public double getRatio() {
        return ratio;
    }
}
