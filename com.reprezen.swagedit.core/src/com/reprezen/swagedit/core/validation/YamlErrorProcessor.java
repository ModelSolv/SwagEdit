package com.reprezen.swagedit.core.validation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;

public class YamlErrorProcessor {

    public String rewriteMessage(Exception exception) {
        if (exception instanceof YAMLException) {
            return rewriteMessage((YAMLException) exception);
        }
        // TODO remove hack when the fix for JacksonYAMLParseException is available, see comment above
        String context = hack_getContext(exception);
        if ("while parsing a block mapping".equals(context)) {
            return Messages.error_yaml_parser_indentation;
        }
        return exception != null ? exception.getLocalizedMessage() : null;
    }

    private String hack_getContext(Exception e) {
        try {
            Method method = e.getClass().getMethod("getContext", null);
            Object returnValue = method.invoke(e, null);
            if (returnValue instanceof String) {
                return (String) returnValue;
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e1) {
            return null;
        }
        return null;
    }

    public String rewriteMessage(YAMLException exception) {
        if (exception instanceof ParserException) {
            if ("while parsing a block mapping".equals(((ParserException) exception).getContext())) {
                return Messages.error_yaml_parser_indentation;
            }
        }
        if (exception instanceof ScannerException) {
            return ((ScannerException) exception).getProblem();
        }
        return exception != null ? exception.getLocalizedMessage() : null;
    }

}
