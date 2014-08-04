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

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Holds the state of an opened log file in the context of the {@link LogStore}.
 *
 * @author Harald Pehl
 */
public class LogState {

    public final static LogState NULL = new LogState("n/a", Arrays.asList("n/a"));

    private final String name;
    private final List<String> lines;

    private Position position;
    private int lineNumber;
    private boolean autoRefresh;
    private boolean stale;

    public LogState(String name, List<String> lines) {
        this.name = name;
        this.lines = new ArrayList<>();
        this.lines.addAll(lines);

        this.position = Position.TAIL;
        this.lineNumber = 0;
        this.autoRefresh = true;
        this.stale = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogState)) return false;

        LogState logState = (LogState) o;

        if (!name.equals(logState.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LogState(").append(name).append("@");
        if (isHead() || isTail()) {
            builder.append(position);
        } else {
            builder.append(lineNumber);
        }
        if (autoRefresh) {
            builder.append(", autoRefresh");
        }
        if (stale) {
            builder.append(", stale");
        }
        builder.append(")");
        return builder.toString();
    }

    void goTo(int line) {
        this.position = Position.LINE_NUMBER;
        this.lineNumber = line;
        this.autoRefresh = false;
    }

    void goTo(Position position) {
        this.position = position;
        this.lineNumber = 0;
        this.autoRefresh = position == Position.TAIL;
    }

    public String getName() {
        return name;
    }

    public Position getPosition() {
        return position;
    }

    public boolean isHead() {
        return position == Position.HEAD;
    }

    public boolean isTail() {
        return position == Position.TAIL;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getContent() {
        return Joiner.on('\n').join(lines);
    }

    public void setLines(List<String> lines) {
        this.lines.clear();
        this.lines.addAll(lines);
    }

    public List<String> getLines() {
        return lines;
    }

    public boolean isAutoRefresh() {
        return autoRefresh;
    }

    public void setAutoRefresh(boolean autoRefresh) {
        this.autoRefresh = autoRefresh;
    }

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }
}
