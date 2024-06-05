package roomescape.auth.service.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import roomescape.auth.domain.Payload;
import roomescape.auth.service.TokenProvider;
import roomescape.exception.ErrorType;
import roomescape.exception.RoomescapeException;

@Component
public class JwtTokenProvider implements TokenProvider {
    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    public String createAccessToken(String payload) {
        Claims claims = Jwts.claims().setSubject(payload);
        Date now = new Date();
        Date validity = new Date(now.getTime() + jwtProperties.getExpireLength());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecretKey())
                .compact();
    }

    @Override
    public Payload<String> getPayload(String token) {
        try {
            return new Payload<>(
                    Jwts.parser().setSigningKey(jwtProperties.getSecretKey()).parseClaimsJws(token).getBody()
                            .getSubject(),
                    () -> isToken(token)
            );
        } catch (JwtException | IllegalArgumentException e) {
            throw new RoomescapeException(ErrorType.SECURITY_EXCEPTION);
        }

    }

    @Override
    public boolean isToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parser().setSigningKey(jwtProperties.getSecretKey()).parseClaimsJws(token);
            return !claims.getBody().getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}


