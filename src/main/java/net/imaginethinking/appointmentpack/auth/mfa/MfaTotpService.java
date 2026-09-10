package net.imaginethinking.appointmentpack.auth.mfa;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Generates MFA secrets and checks the time based codes entered by the user.
 */
@Service
public class MfaTotpService {

    private static final String ISSUER = "The Appointment Pack";
    private static final Pattern TOTP_CODE_PATTERN = Pattern.compile("^[0-9]{6}$");

    private final GoogleAuthenticator authenticator = new GoogleAuthenticator();

    /**
     * Generates a new random secret used to set up TOTP MFA.
     */
    public String generateSecret() {
        GoogleAuthenticatorKey key = authenticator.createCredentials();
        return key.getKey();
    }

    /**
     * Builds the authenticator provisioning URI from the account email and pending MFA secret.
     */
    public String generateProvisioningUri(String email, String secret) {
        GoogleAuthenticatorKey key = new GoogleAuthenticatorKey.Builder(secret).build();

        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(ISSUER, email, key);
    }

    /**
     * Checks a six digit code against the current TOTP window for the supplied secret.
     */
    public boolean isValidCode(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null || !TOTP_CODE_PATTERN.matcher(code).matches()) {
            return false;
        }

        return authenticator.authorize(secret, Integer.parseInt(code));
    }
}