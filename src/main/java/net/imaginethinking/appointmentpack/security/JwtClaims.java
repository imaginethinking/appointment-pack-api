package net.imaginethinking.appointmentpack.security;

/**
 * Defines the claim names and expected values used in application access tokens.
 */
public final class JwtClaims {

    public static final String ISSUER = "appointment-pack-api";
    public static final String PURPOSE = "purpose";
    public static final String ACCESS_PURPOSE = "ACCESS";
    public static final String ROLES = "roles";

    /**
     * Prevents the utility class from being instantiated.
     */
    private JwtClaims() {
    }
}