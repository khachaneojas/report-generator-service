package com.service.report.generator.aspect;

import com.service.report.generator.dto.TokenValidationResponse;
import com.service.report.generator.entity.UserModel;
import com.service.report.generator.entity.UserRoleModel;
import com.service.report.generator.exception.UnauthorizedException;
import com.service.report.generator.repository.UserRepository;
import com.service.report.generator.tag.UserRole;
import com.service.report.generator.utility.JwtWizard;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;



@Component
@RequiredArgsConstructor
public class AuditorProxy {
    private final UserRepository userAuthenticatorRepository;
    private final JwtWizard jwtHelper;

    @Transactional(
            readOnly = true,
            isolation = Isolation.READ_COMMITTED
    )
    public TokenValidationResponse isTokenValid(String authorizationHeader, Set<UserRole> authorizedRoles) {
        String tokenId = jwtHelper.getSubject(authorizationHeader);
        UserModel user = Optional
                .ofNullable(tokenId)
                .filter(StringUtils::isNotBlank)
                .map(userAuthenticatorRepository::findByUserUid)
                .orElseThrow(UnauthorizedException::new);


        if (jwtHelper.getPayload(authorizationHeader) != user.getTokenAt().toEpochMilli()) {
            throw new UnauthorizedException("Your account may have been updated. To ensure you have access to the updated information, please log in again.");
        }

        Set<UserRole> userRoles = user.getUserRoles().stream().map(UserRoleModel::getRole).collect(Collectors.toCollection(LinkedHashSet::new));
        if (userRoles.isEmpty() || userRoles.stream().noneMatch(authorizedRoles::contains)) {
            throw new UnauthorizedException();
        }

        setAuthenticationInSecurityContext(user.getUserUid(), userRoles.stream().map(Enum::toString).collect(Collectors.toSet()));
        return TokenValidationResponse.builder()
                .pid(user.getUserPid())
                .uid(user.getUserUid())
                .adminRole(userRoles.contains(UserRole.ADMIN))
                .build();
    }

    private static void setAuthenticationInSecurityContext(String uid, Set<String> authorities) {
        Objects.requireNonNull(uid);
        Objects.requireNonNull(authorities);
        User userDetails = new User(uid, uid, authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet()));
        SecurityContextHolder
                .getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }
}
