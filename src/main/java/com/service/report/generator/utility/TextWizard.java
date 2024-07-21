package com.service.report.generator.utility;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class TextWizard implements TextHelper{

    public boolean isBlank(String str) {
        if (null == str || str.isEmpty())
            return true;

        int strLen = str.length();
        for (int i = 0; i < strLen; ++i) {
            if (!isAsciiWhitespace(str.charAt(i)))
                return false;
        }

        return true;
    }

    @Override
    public String sanitize(String str) {
        if (isBlank(str))
            return null;

        return Jsoup.clean(
                str.trim(),
                Safelist.simpleText()
        );
    }


    public double evaluateExpression(String expression) {
        try (Context context = Context.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")                  // Disable warning
                .build()) {
            Value result = context.eval("js", expression);
            return result.asDouble();
        }
    }

    private boolean isAsciiWhitespace(char ch) {
        return ch == 32 || ch == 9 || ch == 10 || ch == 12 || ch == 13;
    }

}
