package org.keycloakextensions.federation.database;


import java.util.HashSet;
import java.util.Set;

/**
 * Created by robson on 02/07/16.
 */
public class DatabaseFederationFactory implements UserFederationProviderFactory {

    static final Set<String> configOptions = new HashSet<String>();

    static {
        configOptions.add("path");
    }

}
