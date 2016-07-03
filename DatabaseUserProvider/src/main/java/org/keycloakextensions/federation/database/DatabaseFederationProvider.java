package org.keycloakextensions.federation.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;

public class DatabaseFederationProvider implements UserFederationProvider {

	protected static final Set<String> supportedCredentialTypes = new HashSet<String>();

	private final DataSource datasource;
	private String sqlAllUsers;
	private String sqlExistsUserByName;
	private String sqlPasswordOfUser;
	protected KeycloakSession session;
	protected UserFederationProviderModel model;

	static {
		supportedCredentialTypes.add(UserCredentialModel.PASSWORD);
	}

	public DatabaseFederationProvider(KeycloakSession session, UserFederationProviderModel model,
			DataSource datasource) {
		this.datasource = datasource;
		this.session = session;
		this.model = model;
	}


	public void setSqlAllUsers(String sqlAllUsers) {
		this.sqlAllUsers = sqlAllUsers;
	}

	public void setSqlExistsUserByName(String sqlExistsUserByName) {
		this.sqlExistsUserByName = sqlExistsUserByName;
	}

	public void setSqlPasswordOfUser(String sqlPasswordOfUser) {
		this.sqlPasswordOfUser = sqlPasswordOfUser;
	}

	public Set<String> getAllUserNames() {
		Set<String> allUserNames = new HashSet<>();

		try (Connection connection = this.datasource.getConnection()) {

			try (PreparedStatement statement = connection.prepareStatement(this.sqlAllUsers)) {
				try (ResultSet result = statement.executeQuery()) {
					while (result.next()) {
						allUserNames.add(result.getString("userName"));
					}
				}
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return allUserNames;
	}

	@Override
	public void close() {

	}

	@Override
	public List<UserModel> getGroupMembers(RealmModel arg0, GroupModel arg1, int arg2, int arg3) {
		return Collections.emptyList();
	}

	@Override
	public Set<String> getSupportedCredentialTypes() {
		return supportedCredentialTypes;
	}

	@Override
	public Set<String> getSupportedCredentialTypes(UserModel arg0) {
		return supportedCredentialTypes;
	}

	@Override
	public UserModel getUserByEmail(RealmModel arg0, String arg1) {
		return null;
	}

	@Override
	public UserModel getUserByUsername(RealmModel realm, String username) {

		if (isExistisUser(username)) {
			UserModel userModel = session.userStorage().addUser(realm, username);
			userModel.setEnabled(true);
			userModel.setFederationLink(model.getId());
			return userModel;
		}

		return null;
	}

	@Override
	public boolean isValid(RealmModel realmModel, UserModel userModel) {
		return isExistisUser(userModel.getUsername());
	}

	@Override
	public void preRemove(RealmModel arg0) {

	}

	@Override
	public void preRemove(RealmModel arg0, RoleModel arg1) {

	}

	@Override
	public void preRemove(RealmModel arg0, GroupModel arg1) {

	}

	@Override
	public UserModel register(RealmModel realm, UserModel model) {
		throw new IllegalStateException("Registration not supported");
	}

	@Override
	public boolean removeUser(RealmModel realm, UserModel model) {
		throw new IllegalStateException("Remove not supported");
	}

	@Override
	public List<UserModel> searchByAttributes(Map<String, String> attributes, RealmModel realm, int maxResults) {
		String username = attributes.get(USERNAME);
		if (username != null) {
			if (session.userStorage().getUserByUsername(username, realm) == null) {
				UserModel user = getUserByUsername(realm, username);
				if (user != null) {
					List<UserModel> list = new ArrayList<UserModel>(1);
					list.add(user);
					return list;
				}
			}
		}
		return Collections.emptyList();
	}

	@Override
	public boolean synchronizeRegistrations() {
		return false;
	}

	@Override
	public boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input) {
		for (UserCredentialModel cred : input) {
			if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
				String password = getUserPassword(user.getUsername());
				if (password == null)
					return false;
				return password.equals(cred.getValue());
			} else {
				return false; // invalid cred type
			}
		}
		return false;
	}

	@Override
	public boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... input) {
		for (UserCredentialModel cred : input) {
			if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
				String password = getUserPassword(user.getUsername());
				if (password == null)
					return false;
				return password.equals(cred.getValue());
			} else {
				return false; // invalid cred type
			}
		}
		return true;
	}

	@Override
	public CredentialValidationOutput validCredentials(RealmModel realm, UserCredentialModel credential) {
		return CredentialValidationOutput.failed();
	}

	@Override
	public UserModel validateAndProxy(RealmModel realm, UserModel local) {
		if (isValid(realm, local)) {
			return new ReadonlyUserModelProxy(local);
		} else {
			return null;
		}
	}

	private boolean isExistisUser(String username) {
		try (Connection connection = this.datasource.getConnection()) {
			try (PreparedStatement statement = connection.prepareStatement(sqlExistsUserByName)) {
				statement.setString(0, username);
				try (ResultSet result = statement.executeQuery()) {
					if (result.next()) {
						int userCount = result.getInt(0);
						if (userCount > 0) {
							return true;
						}
					}
				}

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;

	}
	
	private String getUserPassword(String username){
		try (Connection connection = this.datasource.getConnection()) {
			try (PreparedStatement statement = connection.prepareStatement(sqlPasswordOfUser)) {
				statement.setString(0, username);
				try (ResultSet result = statement.executeQuery()) {
					if (result.next()) {
						String userPassword = result.getString(0);
						return userPassword;
					}
				}

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}

}
