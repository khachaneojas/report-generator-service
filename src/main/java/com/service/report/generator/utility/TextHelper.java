package com.service.report.generator.utility;

public interface TextHelper {
    boolean isBlank(String str);
    String sanitize(String str);
    float evaluateExpression(String expression);
}
