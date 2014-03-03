/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.jboss.as.console.client.core;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.google.inject.Inject;
import org.jboss.as.console.client.Console;

/**
 * Configuration for the top level tabs.
 *
 * @author Harald Pehl
 */
public class ToplevelTabs implements Iterable<ToplevelTabs.Config> {

    public static class Config {

        private final String token;
        private final String title;
        private final boolean updateToken;

        public Config(final String token, final String title, final boolean updateToken) {
            this.token = token;
            this.title = title;
            this.updateToken = updateToken;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) { return true; }
            if (!(o instanceof Config)) { return false; }

            Config that = (Config) o;

            if (!token.equals(that.token)) { return false; }

            return true;
        }

        @Override
        public int hashCode() {
            return token.hashCode();
        }

        @Override
        public String toString() {
            return "ToplevelTab{token='" + token + '\'' + '}';
        }

        public String getToken() {
            return token;
        }

        public String getTitle() {
            return title;
        }

        public boolean isUpdateToken() {
            return updateToken;
        }
    }


    private final List<Config> tabs;

    @Inject
    public ToplevelTabs(final BootstrapContext bootstrapContext) {
        tabs = new LinkedList<Config>();
        tabs.add(new Config(NameTokens.HomepagePresenter, "Home", true));
        if (bootstrapContext.isStandalone()) {
            tabs.add(new Config(NameTokens.serverConfig, Console.CONSTANTS.common_label_configuration(), false));
            tabs.add(new Config(NameTokens.StandaloneRuntimePresenter, "Runtime", false));
        } else {
            tabs.add(
                    new Config(NameTokens.ProfileMgmtPresenter, Console.CONSTANTS.common_label_configuration(), false));
            tabs.add(new Config(NameTokens.HostMgmtPresenter, "Domain", false));
            tabs.add(new Config(NameTokens.DomainRuntimePresenter, "Runtime", false));
        }
        tabs.add(new Config(NameTokens.AdministrationPresenter, "Administration", false));
    }

    @Override
    public Iterator<Config> iterator() {
        return tabs.iterator();
    }

    public boolean isEmpty() {return tabs.isEmpty();}

    public boolean add(final Config config) {return tabs.add(config);}
}
