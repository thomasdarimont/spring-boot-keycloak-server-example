package eu.europeana.keycloak.password;

import eu.europeana.keycloak.StaticPropertyUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.UserCredentialModel;
import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * This class implements BCrypt hashing with salt for passwords. Current version does not support pepper.
 */
public class BCryptPasswordHashProvider extends StaticPropertyUtil implements PasswordHashProvider  {

    private static final Logger LOG = LogManager.getLogger(BCryptPasswordHashProvider.class);
    private              int    logRounds;
    private String providerId;
    private String pepper;


    public BCryptPasswordHashProvider(String providerId, int logRounds) {
        LOG.info("BCryptPasswordHashProvider created");
        this.providerId     = providerId;
        this.logRounds      = logRounds;
        this.pepper         = StaticPropertyUtil.getPepper();
    }

    @Override
    public boolean policyCheck(PasswordPolicy passwordPolicy, CredentialModel credentialModel) {
        LOG.info("BCryptPasswordHashProvider policy check");
        return passwordPolicy.getHashAlgorithm().equals(credentialModel.getAlgorithm()) && passwordPolicy.getHashIterations() == credentialModel.getHashIterations();
    }

    @Override
    public void encode(String rawPassword, int iterations, CredentialModel credentialModel) {
        LOG.info("BCryptPasswordHashProvider encoding password ...");
        String salt     = BCrypt.gensalt(logRounds);
        String hashedPassword = getHash(rawPassword, salt);

        credentialModel.setAlgorithm(providerId);
        credentialModel.setType(UserCredentialModel.PASSWORD);
        credentialModel.setSalt(salt.getBytes());
        credentialModel.setValue(hashedPassword);
        credentialModel.setHashIterations(iterations);
    }

    private String getHash(String rawPassword, String salt) {
        LOG.info("BCryptPasswordHashProvider adding salt and pepper ...");
        String pepperedPassword = rawPassword + pepper;
        String base64PepperedPw = new String(Base64.encodeBase64(pepperedPassword.getBytes()));
        return BCrypt.hashpw(base64PepperedPw, salt);
    }

    @Override
    public boolean verify(String rawPassword, CredentialModel credentialModel) {
        LOG.info("BCryptPasswordHashProvider verifying password ...");
        return getHash(rawPassword, new String(credentialModel.getSalt())).equals(credentialModel.getValue());
    }

    @Override
    public void close() {

    }
}
