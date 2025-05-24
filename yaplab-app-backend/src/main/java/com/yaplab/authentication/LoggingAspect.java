package com.yaplab.authentication;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    // Log exceptions thrown by any method in the authentication package
    @AfterThrowing(pointcut = "execution(* com.ankitarsh.securemessaging.authentication..*(..))", throwing = "ex")
    public void logAfterThrowing(Exception ex) {
        logger.error("Exception caught: ", ex);
    }
}