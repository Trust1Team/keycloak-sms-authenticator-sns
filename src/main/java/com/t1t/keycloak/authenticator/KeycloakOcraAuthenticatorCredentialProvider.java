package com.t1t.keycloak.authenticator;

import org.keycloak.common.util.Time;
import org.keycloak.credential.*;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author Michallis Pashidis
 * @Since 2017
 */
public class KeycloakOcraAuthenticatorCredentialProvider implements CredentialProvider, CredentialInputValidator, CredentialInputUpdater, OnUserCache {
    private static final String CACHE_KEY = KeycloakOcraAuthenticatorCredentialProvider.class.getName() + "." + KeycloakOcraAuthenticatorConstants.USR_CRED_MDL_OCRA_CODE;

    private final KeycloakSession session;

    public KeycloakOcraAuthenticatorCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    private CredentialModel getSecret(RealmModel realm, UserModel user) {
        CredentialModel secret = null;
        if (user instanceof CachedUserModel) {
            CachedUserModel cached = (CachedUserModel) user;
            secret = (CredentialModel) cached.getCachedWith().get(CACHE_KEY);

        } else {
            List<CredentialModel> creds = session.userCredentialManager().getStoredCredentialsByType(realm, user, KeycloakOcraAuthenticatorConstants.USR_CRED_MDL_OCRA_CODE);
            if (!creds.isEmpty()) secret = creds.get(0);
        }
        return secret;
    }


    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!KeycloakOcraAuthenticatorConstants.USR_CRED_MDL_OCRA_CODE.equals(input.getType())) return false;
        if (!(input instanceof UserCredentialModel)) return false;
        UserCredentialModel credInput = (UserCredentialModel) input;
        List<CredentialModel> creds = session.userCredentialManager().getStoredCredentialsByType(realm, user, KeycloakOcraAuthenticatorConstants.USR_CRED_MDL_OCRA_CODE);
        if (creds.isEmpty()) {
            CredentialModel secret = new CredentialModel();
            secret.setType(KeycloakOcraAuthenticatorConstants.USR_CRED_MDL_OCRA_CODE);
            secret.setValue(credInput.getValue());
            secret.setCreatedDate(Time.currentTimeMillis());
            session.userCredentialManager().createCredential(realm, user, secret);
        } else {
            creds.get(0).setValue(credInput.getValue());
            session.userCredentialManager().updateCredential(realm, user, creds.get(0));
        }
        session.userCache().evict(realm, user);
        return true;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        if (!KeycloakOcraAuthenticatorConstants.USR_CRED_MDL_OCRA_CODE.equals(credentialType)) return;
        session.userCredentialManager().disableCredentialType(realm, user, credentialType);
        session.userCache().evict(realm, user);

    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        if (!session.userCredentialManager().getStoredCredentialsByType(realm, user, KeycloakOcraAuthenticatorConstants.USR_CRED_MDL_OCRA_CODE).isEmpty()) {
            Set<String> set = new HashSet<>();
            set.add(KeycloakOcraAuthenticatorConstants.USR_CRED_MDL_OCRA_CODE);
            return set;
        } else {
            return Collections.<String>emptySet();
        }

    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return KeycloakOcraAuthenticatorConstants.USR_CRED_MDL_OCRA_CODE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return KeycloakOcraAuthenticatorConstants.USR_CRED_MDL_OCRA_CODE.equals(credentialType) && getSecret(realm, user) != null;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!KeycloakOcraAuthenticatorConstants.USR_CRED_MDL_OCRA_CODE.equals(input.getType())) return false;
        if (!(input instanceof UserCredentialModel)) return false;

        String secret = getSecret(realm, user).getValue();

        return secret != null && ((UserCredentialModel) input).getValue().equals(secret);
    }

    @Override
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
        List<CredentialModel> creds = session.userCredentialManager().getStoredCredentialsByType(realm, user, KeycloakOcraAuthenticatorConstants.USR_CRED_MDL_OCRA_CODE);
        if (!creds.isEmpty()) {
            user.getCachedWith().put(CACHE_KEY, creds.get(0));
        }
    }
}
