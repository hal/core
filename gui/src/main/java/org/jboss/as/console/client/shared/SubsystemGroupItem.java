/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.console.client.shared;

/**
 * @author Heiko Braun
 * @author David Bosschaert
 * @date 5/3/11
 */
public class SubsystemGroupItem {

    private String name;
    private String key;
    private String presenter;
    private final int major;
    private final int minor;
    private final int micro;
    private boolean disabled = false;

    public SubsystemGroupItem(String name, String key) {
        this(name, key, key.toLowerCase().replace(" ", "_"));
    }

    public SubsystemGroupItem(String name, String key, String presenter) {
        this(name, key, presenter, 0, 0, 0);
    }

    public SubsystemGroupItem(String name, String key, String presenter, int major, int minor, int micro) {
        this.name = name;
        this.key = key;
        this.presenter = presenter;
        this.major = major;
        this.minor = minor;
        this.micro = micro;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public String getPresenter() {
        return presenter;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getMicro() {
        return micro;
    }
}
