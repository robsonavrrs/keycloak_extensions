package org.keycloakextensions.federation.database;


import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserFederationSyncResult;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * Created by robson on 02/07/16.
 */
public class DatabaseFederationFactory implements UserFederationProviderFactory {

	private DataSource datasource;
	
    static final Set<String> configOptions = new HashSet<String>();

    static {
        configOptions.add("datasource");
        configOptions.add("sqlAllUsers");
        configOptions.add("sqlExistsUserByName");
        configOptions.add("sqlPasswordOfUser");
    }

	@Override
	public void close() {
		
		
	}

	@Override
	public void init(Scope scope) {
		
	}

	@Override
	public void postInit(KeycloakSessionFactory sessionFactory) {

		
	}

	@Override
	public UserFederationProvider create(KeycloakSession session) {
		return null;
	}

	@Override
	public Set<String> getConfigurationOptions() {
		return configOptions;
	}

	@Override
	public String getId() {
		
		return "Database User Federation";
	}

	@Override
	public UserFederationProvider getInstance(KeycloakSession session, UserFederationProviderModel model) {

        String datasourceJndiName = model.getConfig().get("datasource");
        if (datasourceJndiName == null) {
            throw new IllegalStateException("Datasource attribute not configured for provider");
        }
        
        String sqlAllUsers = model.getConfig().get("sqlAllUsers");
        if (sqlAllUsers == null || sqlAllUsers.trim().isEmpty()){
        	throw new IllegalStateException("sqlAllUsers attribute not configured for provider");
        }
		
        String sqlFindUserByName = model.getConfig().get("sqlFindUserByName");
        if (sqlFindUserByName == null || sqlFindUserByName.trim().isEmpty()){
        	throw new IllegalStateException("sqlFindUserByName attribute not configured for provider");
        }

        String sqlExistsUserByName = model.getConfig().get("sqlExistsUserByName");
        if (sqlExistsUserByName == null || sqlExistsUserByName.trim().isEmpty()){
        	throw new IllegalStateException("sqlExistsUserByName attribute not configured for provider");
        }

        String sqlPasswordOfUser = model.getConfig().get("sqlPasswordOfUser");
        if (sqlPasswordOfUser == null || sqlPasswordOfUser.trim().isEmpty()){
        	throw new IllegalStateException("sqlPasswordOfUser attribute not configured for provider");
        }
        
        
		if (null == datasource ){
	    	try {
				Context context = new InitialContext();
				datasource = (DataSource)context.lookup(datasourceJndiName);
			} catch (NamingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}
		
		DatabaseFederationProvider provider = new DatabaseFederationProvider(session, model, datasource);
		provider.setSqlAllUsers(sqlAllUsers);
		provider.setSqlExistsUserByName(sqlExistsUserByName);
		provider.setSqlPasswordOfUser(sqlPasswordOfUser);

		return provider;
	}

	@Override
	public UserFederationSyncResult syncAllUsers(KeycloakSessionFactory sessionFactory, final String realmId,
			final UserFederationProviderModel model) {
	       final UserFederationSyncResult syncResult = new UserFederationSyncResult();

	        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

	            @Override
	            public void run(KeycloakSession session) {
	                RealmModel realm = session.realms().getRealm(realmId);
	                DatabaseFederationProvider federationProvider = (DatabaseFederationProvider)getInstance(session, model);
	                Set<String> allUsernames = federationProvider.getAllUserNames();
	                UserProvider localProvider = session.userStorage();
	                for (String username : allUsernames) {
	                    UserModel localUser = localProvider.getUserByUsername(username, realm);

	                    if (localUser == null) {
	                        // New user, let's import him
	                        UserModel imported = federationProvider.getUserByUsername(realm, username);
	                        if (imported != null) {
	                            syncResult.increaseAdded();
	                        }
	                    }
	                }
	            }

	        });

	        return syncResult;
	}

	@Override
	public UserFederationSyncResult syncChangedUsers(KeycloakSessionFactory sessionFactory, String realmId,
			UserFederationProviderModel model, Date date) {
		return syncAllUsers(sessionFactory, realmId, model);
	}

}
