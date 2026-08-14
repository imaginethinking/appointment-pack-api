package net.imaginethinking.appointmentpack.security;

public final class JwtClaims {

    public static final String ISSUER = "appointment-pack-api";
    public static final String PURPOSE = "purpose";
    public static final String ACCESS_PURPOSE = "ACCESS";
    public static final String ROLES = "roles";

    private JwtClaims() {
    }
}