package com.dji.sample.security;

import com.dji.sample.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String WEB_ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String WEB_REFRESH_TOKEN_TYPE = "REFRESH";

    private static final String DEVICE_ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String DEVICE_REFRESH_TOKEN_TYPE = "REFRESH";

    private static final String DEVICE_ROLE = "DEVICE";

    private final JwtProperties jwtProperties;

    /**
     * Used for normal ROBOPILOT web-user access and refresh tokens.
     */
    private SecretKey secretKey;

    /**
     * Used only for DJI Pilot MQTT device access and refresh tokens.
     */
    private SecretKey mqttSecretKey;

    @PostConstruct
    public void init() {
        String webSecret = jwtProperties.secret();

        if (webSecret == null || webSecret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 characters long"
            );
        }

        String mqttSecret = jwtProperties.mqttSecret();

        if (mqttSecret == null || mqttSecret.length() < 32) {
            throw new IllegalStateException(
                    "MQTT JWT secret must be at least 32 characters long"
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(
                webSecret.getBytes(StandardCharsets.UTF_8)
        );

        this.mqttSecretKey = Keys.hmacShaKeyFor(
                mqttSecret.getBytes(StandardCharsets.UTF_8)
        );
    }

    // -------------------------------------------------------------------------
    // Web-user JWT
    // -------------------------------------------------------------------------

    public String generateAccessToken(
            UUID userId,
            String username,
            String email,
            List<String> roles
    ) {
        Date now = new Date();
        Date expiry = new Date(
                now.getTime() + jwtProperties.accessTokenExpirationMs()
        );

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .claim("token_type", WEB_ACCESS_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(
            UUID userId,
            String username,
            String email
    ) {
        Date now = new Date();
        Date expiry = new Date(
                now.getTime() + jwtProperties.refreshTokenExpirationMs()
        );

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("email", email)
                .claim("token_type", WEB_REFRESH_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpiredSpecifically(String token) {
        try {
            parseToken(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractUserId(String token) {
        String userId = extractAllClaims(token)
                .get("userId", String.class);

        return UUID.fromString(userId);
    }

    public boolean isTokenValid(
            String token,
            UserDetails userDetails
    ) {
        String username = extractUsername(token);

        String tokenType = extractAllClaims(token)
                .get("token_type", String.class);

        return username.equals(userDetails.getUsername())
                && WEB_ACCESS_TOKEN_TYPE.equals(tokenType)
                && !isTokenExpired(token);
    }

    public Long getAccessTokenExpirationMs() {
        return jwtProperties.accessTokenExpirationMs();
    }

    public Long getRefreshTokenExpirationMs() {
        return jwtProperties.refreshTokenExpirationMs();
    }

    public <T> T extractClaim(
            String token,
            Function<Claims, T> resolver
    ) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token)
                .getExpiration()
                .before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return parseToken(token);
    }

    // -------------------------------------------------------------------------
    // DJI MQTT device JWT
    // -------------------------------------------------------------------------

    /**
     * Generates the MQTT access token returned to DJI Pilot.
     *
     * This token is intentionally signed with mqttSecretKey instead of the
     * normal ROBOPILOT web-user secret.
     */
    public String generateDeviceAccessToken(
            UUID deviceId,
            String deviceSn,
            UUID companyId,
            List<String> permissions
    ) {
        Date now = new Date();

        long expirationMs =
                jwtProperties.mqttAccessTokenExpirationSeconds() * 1000L;

        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(buildDeviceSubject(deviceSn))
                .issuer(jwtProperties.mqttIssuer())
                .id(UUID.randomUUID().toString())
                .claim("device_id", deviceId.toString())
                .claim("device_sn", deviceSn)
                .claim(
                        "company_id",
                        companyId != null ? companyId.toString() : null
                )
                .claim("role", DEVICE_ROLE)
                .claim(
                        "permissions",
                        permissions != null ? permissions : List.of()
                )
                .claim("token_type", DEVICE_ACCESS_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(mqttSecretKey)
                .compact();
    }

    /**
     * Generates the refresh token used by DJI Pilot to request a new MQTT
     * access token.
     */
    public String generateDeviceRefreshToken(
            UUID deviceId,
            String deviceSn
    ) {
        Date now = new Date();

        long expirationMs =
                jwtProperties.mqttRefreshTokenExpirationSeconds() * 1000L;

        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(buildDeviceSubject(deviceSn))
                .issuer(jwtProperties.mqttIssuer())
                .id(UUID.randomUUID().toString())
                .claim("device_id", deviceId.toString())
                .claim("device_sn", deviceSn)
                .claim("token_type", DEVICE_REFRESH_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(mqttSecretKey)
                .compact();
    }

    /**
     * Parses only DJI MQTT device tokens.
     *
     * Web tokens cannot be parsed by this method because a separate MQTT
     * signing key is used.
     */
    public Claims parseDeviceToken(String token) {
        return Jwts.parser()
                .verifyWith(mqttSecretKey)
                .requireIssuer(jwtProperties.mqttIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isDeviceRefreshTokenValid(String token) {
        try {
            Claims claims = parseDeviceToken(token);

            String tokenType = claims.get(
                    "token_type",
                    String.class
            );

            return DEVICE_REFRESH_TOKEN_TYPE.equals(tokenType)
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String extractDeviceSn(String token) {
        return parseDeviceToken(token)
                .get("device_sn", String.class);
    }

    public UUID extractDeviceId(String token) {
        String deviceId = parseDeviceToken(token)
                .get("device_id", String.class);

        return UUID.fromString(deviceId);
    }

    public Long getMqttAccessTokenExpirationSeconds() {
        return jwtProperties.mqttAccessTokenExpirationSeconds();
    }

    public Long getMqttRefreshTokenExpirationSeconds() {
        return jwtProperties.mqttRefreshTokenExpirationSeconds();
    }

    private String buildDeviceSubject(String deviceSn) {
        return "device:" + deviceSn;
    }
}