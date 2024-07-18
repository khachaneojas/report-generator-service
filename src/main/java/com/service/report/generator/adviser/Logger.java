package com.service.report.generator.adviser;

import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;

public class Logger {
    private Logger() {}

    public static void log(Class<?> clazz, LogData logData, LogLevel level) {
        log(clazz, logData.toString(), level);
    }

    public static void log(Class<?> clazz, String logData, LogLevel level) {
        final org.slf4j.Logger logger = getLogger(clazz);
        if (LogLevel.INFO.equals(level) && logger.isInfoEnabled()) {
            logger.info(logData);
        } else if (LogLevel.ERROR.equals(level) && logger.isErrorEnabled()) {
            logger.error(logData);
        }
    }

    public static org.slf4j.Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
}
