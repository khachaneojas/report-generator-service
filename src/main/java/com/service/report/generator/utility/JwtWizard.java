package com.service.report.generator.utility;

import com.service.report.generator.exception.JwtException;
import com.service.report.generator.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;


@Component
public class JwtWizard {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    public static final long DEFAULT_EXPIRY_DURATION = Duration.of(24, ChronoUnit.HOURS).toMillis();

    private SecretKey getSignedSecretKey() {
        Objects.requireNonNull(jwtSecret);
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims getClaims(String jwtString) {
        try {
            return Jwts.parser()
                    .verifyWith(getSignedSecretKey())
                    .build()
                    .parseSignedClaims(jwtString)
                    .getPayload();
        } catch (Exception ex) {
            throw new JwtException(ex.getMessage());
        }
    }

    private String getJwtToken(String authorizationHeader) {
        if (StringUtils.isNotBlank(authorizationHeader) && authorizationHeader.startsWith("Bearer "))
            return authorizationHeader.substring(7);

        throw new UnauthorizedException();
    }

    public long getPayload(String authorizationHeader) {
        String token = getJwtToken(authorizationHeader);
        Claims claims = getClaims(token);
        if (!claims.containsKey("upt")) {
            throw new UnauthorizedException();
        }

        return claims.get("upt", Long.class);
    }

    public String getSubject(String authorizationHeader) {
        String token = getJwtToken(authorizationHeader);
        Claims claims = getClaims(token);
        String subject = claims.getSubject();
        if (StringUtils.isBlank(subject)) {
            throw new UnauthorizedException();
        }

        return subject;
    }

    public String issueToken(String subject, long claim) {
        Objects.requireNonNull(subject);
        return issueToken(subject, claim, DEFAULT_EXPIRY_DURATION);
    }

    public String issueToken(String subject, long claim, long expiration) {
        Objects.requireNonNull(subject);
        JwtBuilder jwtBuilder = Jwts.builder()
                .subject(subject)
                .claim("upt", claim)
                .issuedAt(new Date())
                .signWith(getSignedSecretKey(), Jwts.SIG.HS512);

        if (expiration > 1) {
            jwtBuilder.expiration(new Date(System.currentTimeMillis() + expiration));
        }

        return jwtBuilder.compact();
    }
}
