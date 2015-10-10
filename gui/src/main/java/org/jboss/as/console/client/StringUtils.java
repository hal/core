package org.jboss.as.console.client;


public final class StringUtils {

    public static final String ELLIPSIS = "...";

    private StringUtils() {
    }

    public static String shortenStringIfNecessary(String string, int maxLength, String suffixToAppend) {
        if (string == null)
            return null; // FIXME: throw IllegalState or log a warning ?
        return (string.length() > maxLength) ? string.substring(0, maxLength) + suffixToAppend : string;
    }

    public static String shortenStringIfNecessary(String string, int maxLength) {
        return shortenStringIfNecessary(string, maxLength, ELLIPSIS);
    }

    public static String abbreviateMiddle(String string, int maxLength) {
        if (string == null || maxLength >= string.length()) {
            return string;
        }

        final int targetSting = maxLength - ELLIPSIS.length();
        final int startOffset = targetSting / 2 + targetSting % 2;
        final int endOffset = string.length() - targetSting / 2;

        return string.substring(0, startOffset) + ELLIPSIS + string.substring(endOffset);
    }

}
