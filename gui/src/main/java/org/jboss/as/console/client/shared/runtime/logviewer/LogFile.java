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
public class LogFile {

    public final static LogFile NULL = new LogFile("n/a", Arrays.asList("n/a"));

    private final String name;
    private final List<String> lines;

    private Position position;
    private Direction lastDirection;
    private int skipped;
    private boolean follow;
    private boolean stale;

    public LogFile(String name, List<String> lines) {
        this.name = name;
        this.lines = new ArrayList<>();
        this.lines.addAll(lines);

        this.position = Position.TAIL;
        this.lastDirection = null;
        this.skipped = 0;
        this.follow = true;
        this.stale = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogFile)) return false;

        LogFile logFile = (LogFile) o;

        if (!name.equals(logFile.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LogFile(").append(name).append("@");
        if (isHead() || isTail()) {
            builder.append(position);
        } else {
            builder.append(skipped);
        }
        if (lastDirection != null) {
            builder.append(" >> ").append(lastDirection);
        }
        if (follow) {
            builder.append(", follow");
        }
        if (stale) {
            builder.append(", stale");
        }
        builder.append(")");
        return builder.toString();
    }

    void goTo(int line) {
        this.position = Position.LINE_NUMBER;
        this.skipped = line;
        this.follow = false;
    }

    void goTo(Position position) {
        this.position = position;
        this.skipped = 0;
        this.follow = position == Position.TAIL;
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

    public Direction getLastDirection() {
        return lastDirection;
    }

    public void setLastDirection(Direction lastDirection) {
        this.lastDirection = lastDirection;
    }

    public int getSkipped() {
        return skipped;
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

    public List<String> getLines(int from, int to) {
        List<String> subList = new ArrayList<>();
        if (to <= lines.size()) {
            subList = lines.subList(from, to);
        }
        return subList;
    }

    public boolean isFollow() {
        return follow;
    }

    public void setFollow(boolean follow) {
        this.follow = follow;
    }

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }
}
