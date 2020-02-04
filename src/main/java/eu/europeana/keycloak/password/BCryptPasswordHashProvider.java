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

import java.nio.charset.StandardCharsets;

/**
 * This class implements BCrypt hashing with salt and pepper for passwords.
 */
public class BCryptPasswordHashProvider implements PasswordHashProvider  {

    private static final Logger LOG = LogManager.getLogger(BCryptPasswordHashProvider.class);
    private              int    logRounds;
    private String providerId;
    private String pepper;


    public BCryptPasswordHashProvider(String providerId, int logRounds) {
        LOG.debug("BCryptPasswordHashProvider created");
        this.providerId     = providerId;
        this.logRounds      = logRounds;
        this.pepper         = StaticPropertyUtil.getPepper();
    }

    @Override
    public boolean policyCheck(PasswordPolicy passwordPolicy, CredentialModel credentialModel) {
        LOG.debug("BCryptPasswordHashProvider policy check");
        return passwordPolicy.getHashAlgorithm().equals(credentialModel.getAlgorithm())
                && (passwordPolicy.getHashIterations() == credentialModel.getHashIterations());
    }

    @Override
    public void encode(String rawPassword, int iterations, CredentialModel credentialModel) {
        LOG.debug("BCryptPasswordHashProvider encoding password ...");
        String salt     = BCrypt.gensalt(logRounds);
        String hashedPassword = getHash(rawPassword, salt);

        credentialModel.setAlgorithm(providerId);
        credentialModel.setType(UserCredentialModel.PASSWORD);
        credentialModel.setSalt(salt.getBytes(StandardCharsets.UTF_8));
        credentialModel.setValue(hashedPassword);
        credentialModel.setHashIterations(iterations);
    }

    private String getHash(String rawPassword, String salt) {
        LOG.debug("BCryptPasswordHashProvider adding salt and pepper ...");
        String pepperedPassword = rawPassword + pepper;
        String base64PepperedPw = new String(Base64.encodeBase64(pepperedPassword.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);
        return BCrypt.hashpw(base64PepperedPw, salt);
    }

    @Override
    public boolean verify(String rawPassword, CredentialModel credentialModel) {
        LOG.debug("BCryptPasswordHashProvider verifying password ...");
        return getHash(rawPassword, new String(credentialModel.getSalt(), StandardCharsets.UTF_8))
                .equals(credentialModel.getValue());
    }

    @Override
    public void close() {
        // no need to do anything
    }
}
