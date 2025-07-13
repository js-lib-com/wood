package com.jslib.wood.util;

import com.jslib.wood.lang.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.List;

/**
 * Strings manipulation utility.
 *
 * @author Iulian Rotaru
 */
public class StringsUtil {
    /**
     * Prevent default constructor synthesis but allow sub-classing.
     */
    protected StringsUtil() {
    }

    /**
     * Convert words separated by dash, underscore, space, slash and backslash to title case. Title case follows English
     * convention: uses space for separator and every word begin with upper case. Returns null or empty string if given
     * <code>string</code> parameter is null, respective empty.
     *
     * @param string string containing words to convert.
     * @return <code>string</code> converted to title case, null or empty.
     */
    public static String toTitleCase(String string) {
        if (string == null) {
            return null;
        }
        if (string.isEmpty()) {
            return "";
        }

        StringBuilder title = new StringBuilder();
        int index = 0;
        for (String word : split(string, '-', '_', ' ', '/', '\\')) {
            if (index++ > 0) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
        }
        return title.toString();
    }

    /**
     * Convert words separated by dash, space, slash and backslash to Java member name. First word is left as it is but
     * the next ones are converted to title case, that is, first character to upper case and the rest to lower. Since Java
     * member name can contain underscore it is not considered as word separator. Returns null or empty string if given
     * <code>string</code> parameter is null, respective empty.
     *
     * @param string string containing words to convert.
     * @return <code>string</code> converted to title case, null or empty.
     */
    public static String toMemberName(String string) {
        if (string == null) {
            return null;
        }
        if (string.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for (String word : split(string, '-', ' ', '/', '\\')) {
            if (first) {
                first = false;
                sb.append(word);
                continue;
            }

            sb.append(Character.toUpperCase(word.charAt(0)));
            sb.append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    /**
     * Get Java accessor for a given member name. Returns the given <code>memberName</code> prefixed by
     * <code>prefix</code>. If <code>memberName</code> is dashed case, that is, contains dash character convert it to
     * camel case. For example, getter for <em>email-addresses</em> is <em>getEmailAddresses</em> and for <em>picture</em>
     * is <em>getPicture</em>.
     * <p>
     * Accessor <code>prefix</code> is inserted before method name and for flexibility it can be anything. Anyway, usual
     * values are <code>get</code>, <code>set</code> and <code>is</code>. It is caller responsibility to supply the right
     * prefix.
     *
     * @param prefix     accessor prefix,
     * @param memberName member name.
     * @return member accessor name.
     * @throws IllegalArgumentException if any given parameter is null or empty.
     */
    public static String getMethodAccessor(String prefix, String memberName) throws IllegalArgumentException {
        assert prefix != null && !prefix.isEmpty() : "Prefix argument is null or empty";
        assert memberName != null && !memberName.isEmpty() : "Member name argument is null or empty";

        StringBuilder builder = new StringBuilder();
        builder.append(prefix);

        String[] parts = memberName.split("-+");
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    /**
     * Concatenates a variable number of objects, as strings. For every given argument, convert it to string using
     * {@link Object#toString()} overload and append to concatenated result. If a given argument happens to be null, skip
     * it. Return empty string if this method is invoked with no arguments at all.
     *
     * @param objects variable number of objects.
     * @return concatenated objects.
     */
    public static String concat(Object... objects) {
        StringBuilder sb = new StringBuilder();
        for (Object object : objects) {
            if (object != null) {
                sb.append(object);
            }
        }
        return sb.toString();
    }

    /**
     * Default separators used when {@link #isSeparator(char, char...)} separators list is empty.
     */
    private static final char[] DEFAULT_SEPARATORS = new char[]{' '};

    /**
     * Test if <code>character</code> is a separator as defined by given <code>separators</code> list. If separator list
     * is empty consider used {@link #DEFAULT_SEPARATORS}. This predicate is designed specifically for
     * {@link #split(String, char...)} operation.
     *
     * @param character  character to test if usable as separator,
     * @param separators variable number of separator characters.
     * @return true if <code>character</code> is a separator, as defined by this method.
     */
    private static boolean isSeparator(char character, char... separators) {
        if (separators.length == 0) {
            separators = DEFAULT_SEPARATORS;
        }
        for (char separator : separators) {
            if (character == separator) {
                return true;
            }
        }
        return false;
    }

    /**
     * Splits string into not empty, trimmed items, using specified separator(s) or space if no separator provided. Please
     * note that returned list does not contain empty items and that all items are trimmed using standard
     * {@link String#trim()}.
     * <p>
     * Returns null if string argument is null and empty list if is empty. This method supports a variable number of
     * separator characters - as accepted by {@link #isSeparator(char, char...)} predicate; if none given uses space.
     *
     * @param string     source string,
     * @param separators variable number of characters used as separators.
     * @return strings list, possible empty.
     */
    public static List<String> split(String string, char... separators) {
        if (string == null) {
            return null;
        }

        class ItemsList {
            final List<String> list = new ArrayList<>();

            void add(StringBuilder wordBuilder) {
                String value = wordBuilder.toString().trim();
                if (!value.isEmpty()) {
                    list.add(value);
                }
            }
        }

        ItemsList itemsList = new ItemsList();
        StringBuilder itemBuilder = new StringBuilder();

        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            // append to on building item all characters that are not separators
            if (!isSeparator(c, separators)) {
                itemBuilder.append(c);
            }
            // if separator found add item to list and reset builder
            if (itemBuilder.length() > 0) {
                if (isSeparator(c, separators)) {
                    itemsList.add(itemBuilder);
                    itemBuilder.setLength(0);
                }
            }
        }

        itemsList.add(itemBuilder);
        return itemsList.list;
    }

    /**
     * Load string from character stream then closes it.
     *
     * @param reader   source character stream.
     * @param maxCount optional maximum character count to load, default to entire file.
     * @return loaded string, possible empty but never null.
     * @throws IOException if read operation fails.
     */
    public static String load(Reader reader, Integer... maxCount) throws IOException {
        long maxCountValue = maxCount.length > 0 ? maxCount[0] : Long.MAX_VALUE;
        StringWriter writer = new StringWriter();

        try {
            char[] buffer = new char[1024];
            for (; ; ) {
                int readChars = reader.read(buffer, 0, (int) Math.min(buffer.length, maxCountValue));
                if (readChars <= 0) {
                    break;
                }
                writer.write(buffer, 0, readChars);
                maxCountValue -= readChars;
            }
        } finally {
            FilesUtil.close(reader);
            FilesUtil.close(writer);
        }

        return writer.toString();
    }

    public static String loadResource(String resource) throws IOException {
        try (InputStream stream = StringsUtil.class.getResourceAsStream(resource)) {
            if (stream == null) {
                return null;
            }
            Reader reader = new InputStreamReader(stream);
            return load(reader);
        }
    }

    /**
     * Process formatted string with arguments transform and no illegal format exception. Java string format throws
     * unchecked {@link IllegalFormatException} if given string is not well formatted. This method catches it and return
     * original, not formatted string if exception happened. Uses this method instead of Java String whenever source
     * string is not from safe source, that can guaranty its format correctness.
     * <p>
     * Return null if format string argument is null and empty if empty. If optional arguments is missing return original
     * string.
     * <p>
     * This method takes care to pre-process arguments as follows:
     * <ul>
     * <li>replace {@link Class} with its canonical name,
     * <li>replace {@link Throwable} with exception message or exception class canonical name if null message,
     * <li>replace {@link Thread} with concatenation of thread name and thread ID,
     * <li>replace {@link File} with its absolute path.
     * </ul>
     * All pre-processed arguments are replaced with string value and format specifier should be also string (%s).
     *
     * @param format string to format,
     * @param args   variable number of arguments related to format.
     * @return formatted string, possible empty or null.
     */
    public static String format(String format, Object... args) {
        if (format == null) {
            return null;
        }
        if (format.isEmpty()) {
            return "";
        }
        if (args.length == 0) {
            return format;
        }

        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Class) {
                args[i] = ((Class<?>) args[i]).getCanonicalName();
            } else if (args[i] instanceof Throwable) {
                String s = ((Throwable) args[i]).getMessage();
                if (s == null) {
                    s = args[i].getClass().getCanonicalName();
                }
                args[i] = s;
            } else if (args[i] instanceof Thread) {
                Thread thread = (Thread) args[i];
                args[i] = thread.getName() + ':' + thread.getId();
            } else if (args[i] instanceof File) {
                args[i] = ((File) args[i]).getAbsolutePath();
            }
        }

        try {
            return String.format(format, args);
        } catch (IllegalFormatException e) {
            // ignore format errors and return original string
        }
        return format;
    }

    /**
     * Join array of objects, converted to string, using specified separator. Returns null if given objects array is null
     * and empty if empty. Null objects or empty strings from given <code>objects</code> parameter are ignored.
     *
     * @param objects   array of objects to join,
     * @param separator character used as separator.
     * @return joined string.
     */
    public static String join(Object[] objects, char separator) {
        return join(objects, Character.toString(separator));
    }

    /**
     * Join array of objects, converted to string, using specified separator. Returns null if given objects
     * array is null and empty if empty. If separator is null uses space string instead, like invoking
     * {@link StringsUtil#join(Iterable)}. Null objects or empty strings from given <code>objects</code> parameter are
     * ignored.
     *
     * @param objects   array of objects to join,
     * @param separator string used as separator.
     * @return joined string.
     */
    public static String join(Object[] objects, String separator) {
        return objects != null ? join(Arrays.asList(objects), separator) : null;
    }

    /**
     * Join collection of objects, converted to string, using specified char separator. Returns null if given objects
     * array is null and empty if empty.
     *
     * @param objects   collection of objects to join,
     * @param separator character used as separator.
     * @return joined string.
     */
    public static String join(Iterable<?> objects, char separator) {
        return join(objects, Character.toString(separator));
    }

    /**
     * Join collection of objects, converted to string, using space as separator. Returns null if given objects array is
     * null and empty if empty.
     *
     * @param objects collection of objects to join.
     * @return joined string.
     */
    public static String join(Iterable<?> objects) {
        return join(objects, " ");
    }

    /**
     * Join collection of objects, converted to string, using specified string separator. Concatenates strings from
     * collection converted to string but take care to avoid null items. Uses given separator between strings. Returns
     * null if given objects array is null and empty if empty. If separator is null uses space string instead, like
     * invoking {@link StringsUtil#join(Iterable)}. Null objects or empty strings from given <code>objects</code> parameter
     * are ignored.
     *
     * @param objects   collection of objects to join,
     * @param separator string used as separator.
     * @return joined string.
     */
    public static String join(Iterable<?> objects, String separator) {
        if (objects == null) {
            return null;
        }
        if (separator == null) {
            separator = " ";
        }

        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Object object : objects) {
            if (object == null) {
                continue;
            }
            String value = object instanceof String ? (String) object : object.toString();
            if (value.isEmpty()) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                builder.append(separator);
            }
            builder.append(value);
        }
        return builder.toString();
    }

    /**
     * Split string using given pairs separator and every pair using pair component separator. Return the list of pair
     * instances, possible empty if string value is empty. Returns null if string value is null.
     * <p>
     * Returned pair components are trimmed for spaces. This means that spaces around separators are eliminated; this is
     * true for both pairs and pair components separators. For example, " john : doe ; jane : doe ; " will return
     * Pair("john", "doe"), Pair("jane", "doe").
     * <p>
     * Trailing pairs separator is optional.
     *
     * @param string              string value,
     * @param pairsSeparator      pairs separator,
     * @param componentsSeparator pair components separator.
     * @return list of pairs, possible empty.
     * @throws IllegalArgumentException if a pair is not valid, that is, pair components separator is missing.
     */
    public static List<Pair> splitPairs(String string, char pairsSeparator, char componentsSeparator) {
        if (string == null) {
            return null;
        }
        string = string.trim();

        final int length = string.length();
        final List<Pair> list = new ArrayList<>();
        int beginIndex = 0;
        int endIndex = 0;

        while (endIndex < length) {
            if (string.charAt(endIndex) == pairsSeparator) {
                if (endIndex > beginIndex) {
                    list.add(pair(string.substring(beginIndex, endIndex), componentsSeparator));
                }
                beginIndex = ++endIndex;
            }
            ++endIndex;
        }
        if (beginIndex < length) {
            list.add(pair(string.substring(beginIndex), componentsSeparator));
        }
        return list;
    }

    /**
     * Split string value using given separator and return initialized pair instance. Pair values are trimmed for spaces
     * so that this method eliminates spaces around separator and around string value. For example, " john : doe " will
     * return Pair("john", "doe").
     *
     * @param string    string value,
     * @param separator pair components separator.
     * @return newly create pair instance.
     * @throws IllegalArgumentException if separator not found.
     */
    private static Pair pair(String string, char separator) {
        int separatorIndex = string.indexOf(separator);
        if (separatorIndex == -1) {
            throw new IllegalArgumentException("Missing pair separator. Cannot initialize pair instance.");
        }
        final String first = string.substring(0, separatorIndex).trim();
        final String second = string.substring(separatorIndex + 1).trim();
        return new Pair(first, second);
    }

    /**
     * Pattern for regular expression reserved characters.
     */
    private static final String REGEXP_PATTERN = "([/|.*+?()\\[\\]{}\\\\^$])";

    /**
     * First argument for regular expression string replacement.
     */
    private static final String REPLACE_ARG_REX = "\\\\$1";

    /**
     * Escape string for regular expression reserved characters. Return null if string argument is null and empty if
     * empty.
     *
     * @param string regular expression to escape.
     * @return newly created, escaped string.
     */
    public static String escapeRegExp(String string) {
        if (string == null) {
            return null;
        }
        return string.replaceAll(REGEXP_PATTERN, REPLACE_ARG_REX);
    }

    /**
     * Escape text for reserved XML characters. Replace quotes, apostrophe, ampersand, left and right angular brackets
     * with entities. Return the newly created, escaped string; if text argument is null or empty returns an empty string.
     *
     * @param text string to escape.
     * @return the newly created string.
     */
    public static String escapeXML(String text) {
        StringWriter writer = new StringWriter();
        try {
            escapeXML(text, writer);
        } catch (IOException e) {
            // there is no reason for IO exception on a string writer
            throw new IllegalStateException("IO failure while attempting to write to string.");
        }
        return writer.toString();
    }

    /**
     * Escape text for reserved XML characters to a specified writer. This method has the same logic as
     * {@link #escapeXML(String)} but result is serialized on the given writer. If <code>text</code> parameters is null
     * this method does nothing.
     *
     * @param text   string to escape,
     * @param writer writer to serialize resulted escaped string.
     * @throws IOException if writer operation fails.
     */
    public static void escapeXML(String text, Writer writer) throws IOException {
        if (text == null) {
            return;
        }
        for (int i = 0, l = text.length(); i < l; ++i) {
            char c = text.charAt(i);
            switch (c) {
                case '"':
                    writer.write("&quot;");
                    break;
                case '\'':
                    writer.write("&apos;");
                    break;
                case '&':
                    writer.write("&amp;");
                    break;
                case '<':
                    writer.write("&lt;");
                    break;
                case '>':
                    writer.write("&gt;");
                    break;
                default:
                    writer.write(c);
            }
        }
    }

    /**
     * Concatenates variable number of objects converted to string, separated by colon. Return empty string if this method
     * is invoked with no arguments at all. Note that this method uses private helper {@link #toString(Object)} to
     * actually convert every given object to string.
     *
     * @param objects variable number of objects.
     * @return objects string representation.
     */
    public static String toString(Object... objects) {
        if (objects.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(toString(objects[0]));
        for (int i = 1; i < objects.length; i++) {
            sb.append(":");
            sb.append(toString(objects[i]));
        }
        return sb.toString();
    }

    /**
     * Convert object to string representation. This method applies next heuristic to convert given object to string
     * representation:
     * <ul>
     * <li>if <code>object</code> is null returns "null",
     * <li>if it is a {@link String} returns it as it is,
     * <li>if is an instance of {@link Class} returns {@link Class#getName()},
     * <li>if is an instance of {@link Throwable} returns causes trace - limited to 8, from cause to cause; if no cause at
     * all returns {@link Throwable#toString()},
     * <li>if none of above returns {@link Object#toString()}.
     * </ul>
     *
     * @param object object to stringify.
     * @return object string representation.
     */
    private static String toString(Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof String) {
            return (String) object;
        }

        if (object instanceof Class) {
            return ((Class<?>) object).getName();
        }

        if (!(object instanceof Throwable)) {
            return object.toString();
        }

        Throwable t = (Throwable) object;
        if (t.getCause() == null) {
            return t.toString();
        }

        int level = 0;
        StringBuilder sb = new StringBuilder();
        for (; ; ) {
            sb.append(t.getClass().getName());
            sb.append(":");
            sb.append(" ");
            if (++level == 8) {
                sb.append("...");
                break;
            }
            if (t.getCause() == null) {
                String s = t.getMessage();
                if (s == null) {
                    s = t.getClass().getName();
                }
                sb.append(s);
                break;
            }
            t = t.getCause();
        }
        return sb.toString();
    }

    /**
     * Copy source characters to requested output bytes stream. If given <code>chars</code> parameter is null or empty
     * this method does nothing.
     *
     * @param chars        source characters stream,
     * @param outputStream target bytes stream.
     * @throws IOException if copy operation fails.
     */
    public static void save(CharSequence chars, OutputStream outputStream) throws IOException {
        if (chars != null) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(chars.toString().getBytes());
            FilesUtil.copy(inputStream, outputStream);
        }
    }
}
