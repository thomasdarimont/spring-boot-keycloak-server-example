package eu.europeana.keycloak.password;

import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.UserCredentialModel;
import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * This class implements BCrypt hashing with salt for passwords. Current version does not support pepper.
 */
public class BCryptPasswordHashProvider implements PasswordHashProvider {

    private int logRounds;

    private String providerId;

    public BCryptPasswordHashProvider(String providerId, int logRounds) {
        this.providerId = providerId;
        this.logRounds = logRounds;
    }

    @Override
    public boolean policyCheck(PasswordPolicy passwordPolicy, CredentialModel credentialModel) {
        return passwordPolicy.getHashAlgorithm().equals(credentialModel.getAlgorithm()) && passwordPolicy.getHashIterations() == credentialModel.getHashIterations();
    }

    @Override
    public void encode(String rawPassword, int iterations, CredentialModel credentialModel) {
        String salt = BCrypt.gensalt(logRounds);
        String hashedPassword = getHash(rawPassword, salt);

        credentialModel.setAlgorithm(providerId);
        credentialModel.setType(UserCredentialModel.PASSWORD);
        credentialModel.setSalt(salt.getBytes());
        credentialModel.setValue(hashedPassword);
        credentialModel.setHashIterations(iterations);
    }

    private String getHash(String rawPassword, String salt) {
        return BCrypt.hashpw(rawPassword, salt);
    }

    @Override
    public boolean verify(String rawPassword, CredentialModel credentialModel) {
        return getHash(rawPassword, new String(credentialModel.getSalt())).equals(credentialModel.getValue());
    }

    @Override
    public void close() {

    }
}
