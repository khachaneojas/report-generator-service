package com.service.report.generator.aspect;

import com.service.report.generator.adviser.LogData;
import com.service.report.generator.adviser.Logger;
import com.service.report.generator.annotation.Auditor;
import com.service.report.generator.dto.APIResponse;
import com.service.report.generator.dto.TokenValidationResponse;
import com.service.report.generator.exception.AnonymityException;
import com.service.report.generator.exception.MissingAuthorizationException;
import com.service.report.generator.tag.UserRole;
import com.service.report.generator.utility.JwtWizard;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;


@Aspect
@Component
@RequiredArgsConstructor
public class AuditorInterceptor {
	private final AuditorProxy proxy;
	private final JwtWizard jwtHelper;

	@Around("@annotation(auditorAnnotation)")
	public Object auditor(ProceedingJoinPoint joinPoint, Auditor auditorAnnotation) throws Throwable {
		HttpServletRequest request = extractHttpServletRequest((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());
		String userAgents = request.getHeader(HttpHeaders.USER_AGENT);

		LogData logData = buildLogData(request);
		String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

		boolean isAuthorizationNotProvided = StringUtils.isBlank(authorizationHeader);

		Object result;
		try {
			if (auditorAnnotation.auditJwt()) {
				if (isAuthorizationNotProvided)
					throw new MissingAuthorizationException();

				Set<UserRole> authorizedRoles= arrayToEnumSet(UserRole.class, auditorAnnotation.allowedRoles());
				TokenValidationResponse response = proxy.isTokenValid(authorizationHeader, authorizedRoles);

				Object[] args = joinPoint.getArgs();
                for (Object arg : args) {
                    if (arg instanceof TokenValidationResponse existing) {
                        existing.setPid(response.getPid());
						existing.setUid(response.getUid());
						existing.setAdminRole(response.isAdminRole());
                    }
                }
			}

			if(!isAuthorizationNotProvided) {
				logData.setId(jwtHelper.getSubject(authorizationHeader));
			}

			result = joinPoint.proceed();
            if (result instanceof ResponseEntity<?> responseEntity && responseEntity.getBody() instanceof APIResponse<?> response) {
                logData.setResponse(response.getMessage());
			}

			Logger.log(joinPoint.getTarget().getClass(), logData, LogLevel.INFO);
		} catch (Exception exception) {
			Throwable rootCause = ExceptionUtils.getRootCause(exception);
			Class<? extends Throwable> clazz = Objects.requireNonNullElse(rootCause, exception).getClass();
			if (clazz.getPackageName().startsWith("com.service.report.generator")) {
				logData.setResponse(exception.getMessage());
			} else {
				logData.setResponse(ExceptionUtils.getStackTrace(exception));
			}

			Logger.log(clazz, logData, LogLevel.ERROR);
			throw exception;
		}

		return result;
	}



	private static LogData buildLogData(HttpServletRequest request) {
		LogData logData = new LogData();
		logData.setHttpMethod(request.getMethod());
		logData.setEndPoint(request.getRequestURI());
		String remoteIp = request.getHeader("X-Forwarded-For");
		String remoteHost = request.getHeader(HttpHeaders.ORIGIN);
		logData.setRemoteIp(StringUtils.isBlank(remoteIp) ? request.getRemoteAddr() : remoteIp);
		logData.setRemoteHost(StringUtils.isBlank(remoteHost) ? request.getRemoteHost() : remoteHost);
		return logData;
	}

	private static <E extends Enum<E>> Set<E> arrayToEnumSet(Class<E> enumClass, E[] array) {
		EnumSet<E> enumSet = EnumSet.noneOf(enumClass);
		if (array.length > 0)
			enumSet.addAll(Arrays.asList(array));

		return enumSet;
	}

	private static void ensureUserAgentContains(String userAgents, String toBeCheckedWith) {
		if (StringUtils.isBlank(userAgents) || StringUtils.isBlank(toBeCheckedWith) || !toBeCheckedWith.toLowerCase().contains(userAgents.toLowerCase()))
			throw new AnonymityException();
	}

	private static HttpServletRequest extractHttpServletRequest(ServletRequestAttributes servletRequestAttributes) {
		return Optional.of(servletRequestAttributes)
				.map(ServletRequestAttributes::getRequest)
				.orElseThrow(() -> new IllegalStateException(
						"No current request bound to the thread. Ensure that this operation is performed within the context of an active HTTP request."
				));
	}
}
