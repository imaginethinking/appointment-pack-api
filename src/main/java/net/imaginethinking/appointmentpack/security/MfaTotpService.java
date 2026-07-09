package net.imaginethinking.appointmentpack.security;


import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.stereotype.Service;

@Service
public class MfaTotpService {

    private static final String ISSUER = "The Appointment Pack";
    private final GoogleAuthenticator authenticator = new GoogleAuthenticator();

    public String generateSecret() {
        GoogleAuthenticatorKey key = authenticator.createCredentials();
        return key.getKey();
    }

    public String generateQrCodeUri(String email, String secret) {
        GoogleAuthenticatorKey key = new GoogleAuthenticatorKey.Builder(secret).build();

        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                ISSUER,
                email,
                key
        );
    }

    public boolean isValidCode(String secret, String code) {
        if (code == null || code == null || !code.matches("\\d{6}")) {
            return false;
        }

        return authenticator.authorize(secret, Integer.parseInt(code));
    }


}
