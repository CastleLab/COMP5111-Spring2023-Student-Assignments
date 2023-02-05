package comp5111.assignment.cut;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.reflect.Array;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.math.BigInteger;
import java.math.BigDecimal;


public class Subject {

    public static class FilenameTasks {

        private static final String[] EMPTY_STRING_ARRAY = {};
    
        private static final int NOT_FOUND = -1;
    
        /**
         * The Unix separator character.
         */
        private static final char UNIX_NAME_SEPARATOR = '/';
    
        /**
         * The Windows separator character.
         */
        private static final char WINDOWS_NAME_SEPARATOR = '\\';
    
        private static final Pattern IPV4_PATTERN = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    
        private static final int IPV4_MAX_OCTET_VALUE = 255;

        private static final int IPV6_MAX_HEX_GROUPS = 8;

        private static final int IPV6_MAX_HEX_DIGITS_PER_GROUP = 4;

        private static final int MAX_UNSIGNED_SHORT = 0xffff;

        private static final int BASE_16 = 16;
    
        private static final Pattern REG_NAME_PART_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9-]*$");
    
        /**
         * Flips the Windows name separator to Linux and vice-versa.
         *
         * @param ch The Windows or Linux name separator.
         * @return The Windows or Linux name separator.
         */
        static char flipSeparator(final char ch) {
            if (ch == UNIX_NAME_SEPARATOR) {
                return WINDOWS_NAME_SEPARATOR;
            }
            if (ch == WINDOWS_NAME_SEPARATOR) {
                return UNIX_NAME_SEPARATOR;
            }
            throw new IllegalArgumentException(String.valueOf(ch));
        }
    
        /**
         * Gets the name minus the path from a full fileName.
         * <p>
         * This method will handle a file in either Unix or Windows format.
         * The text after the last forward or backslash is returned.
         * </p>
         * <pre>
         * a/b/c.txt --&gt; c.txt
         * a.txt     --&gt; a.txt
         * a/b/c     --&gt; c
         * a/b/c/    --&gt; ""
         * </pre>
         * <p>
         * The output will be the same irrespective of the machine that the code is running on.
         * </p>
         *
         * @param fileName  the fileName to query, null returns null
         * @return the name of the file without the path, or an empty string if none exists
         * @throws IllegalArgumentException if the fileName contains the null character ({@code U+0000})
         */
        public static String getName(final String fileName) {
            if (fileName == null) {
                return null;
            }
            return requireNonNullChars(fileName).substring(indexOfLastSeparator(fileName) + 1);
        }
    
        /**
         * Returns the index of the last directory separator character.
         * <p>
         * This method will handle a file in either Unix or Windows format.
         * The position of the last forward or backslash is returned.
         * <p>
         * The output will be the same irrespective of the machine that the code is running on.
         *
         * @param fileName  the fileName to find the last path separator in, null returns -1
         * @return the index of the last separator character, or -1 if there
         * is no such character
         */
        public static int indexOfLastSeparator(final String fileName) {
            if (fileName == null) {
                return NOT_FOUND;
            }
            final int lastUnixPos = fileName.lastIndexOf(UNIX_NAME_SEPARATOR);
            final int lastWindowsPos = fileName.lastIndexOf(WINDOWS_NAME_SEPARATOR);
            return Math.max(lastUnixPos, lastWindowsPos);
        }
    
        /**
         * Checks whether a given string represents a valid IPv4 address.
         *
         * @param name the name to validate
         * @return true if the given name is a valid IPv4 address
         */
        public static boolean isIPv4Address(final String name) {
            final Matcher m = IPV4_PATTERN.matcher(name);
            if (!m.matches() || m.groupCount() != 4) {
                return false;
            }
    
            // verify that address subgroups are legal
            for (int i = 1; i <= 4; i++) {
                final String ipSegment = m.group(i);
                final int iIpSegment = Integer.parseInt(ipSegment);
                if (iIpSegment > IPV4_MAX_OCTET_VALUE) {
                    return false;
                }
    
                if (ipSegment.length() > 1 && ipSegment.startsWith("0")) {
                    return false;
                }
    
            }
    
            return true;
        }
  
        /**
         * Checks whether a given string represents a valid IPv6 address.
         *
         * @param inet6Address the name to validate
         * @return true if the given name is a valid IPv6 address
         */
        public static boolean isIPv6Address(final String inet6Address) {
            final boolean containsCompressedZeroes = inet6Address.contains("::");
            if (containsCompressedZeroes && inet6Address.indexOf("::") != inet6Address.lastIndexOf("::")) {
                return false;
            }
            if (inet6Address.startsWith(":") && !inet6Address.startsWith("::")
                    || inet6Address.endsWith(":") && !inet6Address.endsWith("::")) {
                return false;
            }
            String[] octets = inet6Address.split(":");
            if (containsCompressedZeroes) {
                final List<String> octetList = new ArrayList<>(Arrays.asList(octets));
                if (inet6Address.endsWith("::")) {
                    // String.split() drops ending empty segments
                    octetList.add("");
                } else if (inet6Address.startsWith("::") && !octetList.isEmpty()) {
                    octetList.remove(0);
                }
                octets = octetList.toArray(EMPTY_STRING_ARRAY);
            }
            if (octets.length > IPV6_MAX_HEX_GROUPS) {
                return false;
            }
            int validOctets = 0;
            int emptyOctets = 0; // consecutive empty chunks
            for (int index = 0; index < octets.length; index++) {
                final String octet = octets[index];
                if (octet.isEmpty()) {
                    emptyOctets++;
                    if (emptyOctets > 1) {
                        return false;
                    }
                } else {
                    emptyOctets = 0;
                    // Is last chunk an IPv4 address?
                    if (index == octets.length - 1 && octet.contains(".")) {
                        if (!isIPv4Address(octet)) {
                            return false;
                        }
                        validOctets += 2;
                        continue;
                    }
                    if (octet.length() > IPV6_MAX_HEX_DIGITS_PER_GROUP) {
                        return false;
                    }
                    final int octetInt;
                    try {
                        octetInt = Integer.parseInt(octet, BASE_16);
                    } catch (final NumberFormatException e) {
                        return false;
                    }
                    if (octetInt < 0 || octetInt > MAX_UNSIGNED_SHORT) {
                        return false;
                    }
                }
                validOctets++;
            }
            return validOctets <= IPV6_MAX_HEX_GROUPS && (validOctets >= IPV6_MAX_HEX_GROUPS || containsCompressedZeroes);
        }

        /**
         * Checks whether a given string is a valid host name according to
         * RFC 3986 - not accepting IP addresses.
         *
         * @see "https://tools.ietf.org/html/rfc3986#section-3.2.2"
         * @param name the hostname to validate
         * @return true if the given name is a valid host name
         */
        public static boolean isRFC3986HostName(final String name) {
            final String[] parts = name.split("\\.", -1);
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].isEmpty()) {
                    // trailing dot is legal, otherwise we've hit a .. sequence
                    return i == parts.length - 1;
                }
                if (!REG_NAME_PART_PATTERN.matcher(parts[i]).matches()) {
                    return false;
                }
            }
            return true;
        }
    
        /**
         * Checks if the character is a separator.
         *
         * @param ch  the character to check
         * @return true if it is a separator character
         */
        public static boolean isSeparator(final char ch) {
            return ch == UNIX_NAME_SEPARATOR || ch == WINDOWS_NAME_SEPARATOR;
        }
    
        /**
         * Checks the input for null characters ({@code U+0000}), a sign of unsanitized data being passed to file level functions.
         *
         * This may be used for poison byte attacks.
         *
         * @param path the path to check
         * @return The input
         * @throws IllegalArgumentException if path contains the null character ({@code U+0000})
         */
        private static String requireNonNullChars(final String path) {
            if (path.indexOf(0) >= 0) {
                throw new IllegalArgumentException(
                    "Null character present in file/path name. There are no known legitimate use cases for such data, but several injection attacks may use it");
            }
            return path;
        }
    
    
        /**
         * Splits a string into a number of tokens.
         * The text is split by '?' and '*'.
         * Where multiple '*' occur consecutively they are collapsed into a single '*'.
         *
         * @param text  the text to split
         * @return the array of tokens, never null
         */
        static String[] splitOnTokens(final String text) {
            // used by wildcardMatch
            // package level so a unit test may run on this
    
            if (text.indexOf('?') == NOT_FOUND && text.indexOf('*') == NOT_FOUND) {
                return new String[] { text };
            }
    
            final char[] array = text.toCharArray();
            final ArrayList<String> list = new ArrayList<>();
            final StringBuilder buffer = new StringBuilder();
            char prevChar = 0;
            for (final char ch : array) {
                if (ch == '?' || ch == '*') {
                    if (buffer.length() != 0) {
                        list.add(buffer.toString());
                        buffer.setLength(0);
                    }
                    if (ch == '?') {
                        list.add("?");
                    } else if (prevChar != '*') {// ch == '*' here; check if previous char was '*'
                        list.add("*");
                    }
                } else {
                    buffer.append(ch);
                }
                prevChar = ch;
            }
            if (buffer.length() != 0) {
                list.add(buffer.toString());
            }
    
            return list.toArray(EMPTY_STRING_ARRAY);
        }
    }
   
    public static class StringTasks {
    
        /**
         * Represents a failed index search.
         * @since 2.1
         */
        public static final int INDEX_NOT_FOUND = -1;

        /**
         * The empty String {@code ""}.
         * @since 2.0
         */
        public static final String EMPTY = "";

        /**
         * Gets a CharSequence length or {@code 0} if the CharSequence is
         * {@code null}.
         *
         * @param cs
         *            a CharSequence or {@code null}
         * @return CharSequence length or {@code 0} if the CharSequence is
         *         {@code null}.
         * @since 2.4
         * @since 3.0 Changed signature from length(String) to length(CharSequence)
         */
        public static int length(final CharSequence cs) {
            return cs == null ? 0 : cs.length();
        }
        
        /**
         * Checks if a CharSequence is empty (""), null or whitespace only.
         *
         * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
         *
         * <pre>
         * StringTasks.isBlank(null)      = true
         * StringTasks.isBlank("")        = true
         * StringTasks.isBlank(" ")       = true
         * StringTasks.isBlank("bob")     = false
         * StringTasks.isBlank("  bob  ") = false
         * </pre>
         *
         * @param cs  the CharSequence to check, may be null
         * @return {@code true} if the CharSequence is null, empty or whitespace only
         * @since 2.0
         * @since 3.0 Changed signature from isBlank(String) to isBlank(CharSequence)
         */
        public static boolean isBlank(final CharSequence cs) {
            final int strLen = length(cs);
            if (strLen == 0) {
                return true;
            }
            for (int i = 0; i < strLen; i++) {
                if (!Character.isWhitespace(cs.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Checks if a CharSequence is empty ("") or null.
         *
         * <pre>
         * StringTasks.isEmpty(null)      = true
         * StringTasks.isEmpty("")        = true
         * StringTasks.isEmpty(" ")       = false
         * StringTasks.isEmpty("bob")     = false
         * StringTasks.isEmpty("  bob  ") = false
         * </pre>
         *
         * <p>NOTE: This method changed in Lang version 2.0.
         * It no longer trims the CharSequence.
         * That functionality is available in isBlank().</p>
         *
         * @param cs  the CharSequence to check, may be null
         * @return {@code true} if the CharSequence is empty or null
         * @since 3.0 Changed signature from isEmpty(String) to isEmpty(CharSequence)
         */
        public static boolean isEmpty(final CharSequence cs) {
            return cs == null || cs.length() == 0;
        }
        
        /**
         * Checks if the CharSequence contains only Unicode digits.
         * A decimal point is not a Unicode digit and returns false.
         *
         * <p>{@code null} will return {@code false}.
         * An empty CharSequence (length()=0) will return {@code false}.</p>
         *
         * <p>Note that the method does not allow for a leading sign, either positive or negative.
         * Also, if a String passes the numeric test, it may still generate a NumberFormatException
         * when parsed by Integer.parseInt or Long.parseLong, e.g. if the value is outside the range
         * for int or long respectively.</p>
         *
         * <pre>
         * StringTasks.isNumeric(null)   = false
         * StringTasks.isNumeric("")     = false
         * StringTasks.isNumeric("  ")   = false
         * StringTasks.isNumeric("123")  = true
         * StringTasks.isNumeric("\u0967\u0968\u0969")  = true
         * StringTasks.isNumeric("12 3") = false
         * StringTasks.isNumeric("ab2c") = false
         * StringTasks.isNumeric("12-3") = false
         * StringTasks.isNumeric("12.3") = false
         * StringTasks.isNumeric("-123") = false
         * StringTasks.isNumeric("+123") = false
         * </pre>
         *
         * @param cs  the CharSequence to check, may be null
         * @return {@code true} if only contains digits, and is non-null
         * @since 3.0 Changed signature from isNumeric(String) to isNumeric(CharSequence)
         * @since 3.0 Changed "" to return false and not true
         */
        public static boolean isNumeric(final CharSequence cs) {
            if (isEmpty(cs)) {
                return false;
            }
            final int sz = cs.length();
            for (int i = 1; i < sz; i++) {
                if (!Character.isDigit(cs.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Remove the last character from a String.
         *
         * <p>If the String ends in {@code \r\n}, then remove both
         * of them.</p>
         *
         * <pre>
         * StringTasks.chop(null)          = null
         * StringTasks.chop("")            = ""
         * StringTasks.chop("abc \r")      = "abc "
         * StringTasks.chop("abc\n")       = "abc"
         * StringTasks.chop("abc\r\n")     = "abc"
         * StringTasks.chop("abc")         = "ab"
         * StringTasks.chop("abc\nabc")    = "abc\nab"
         * StringTasks.chop("a")           = ""
         * StringTasks.chop("\r")          = ""
         * StringTasks.chop("\n")          = ""
         * StringTasks.chop("\r\n")        = ""
         * </pre>
         *
         * @param str  the String to chop last character from, may be null
         * @return String without last character, {@code null} if null String input
         */
        public static String chop(final String str) {
            if (str == null) {
                return null;
            }
            final int strLen = str.length();
            if (strLen < 2) {
                return EMPTY;
            }
            final int lastIdx = strLen - 1;
            final String ret = str.substring(0, lastIdx);
            final char last = str.charAt(lastIdx);
            if (last == CharTasks.LF && ret.charAt(lastIdx - 1) == CharTasks.CR) {
                return ret.substring(0, lastIdx - 1);
            }
            return ret;
        }

        /**
         * Removes one newline from end of a String if it's there,
         * otherwise leave it alone.  A newline is &quot;{@code \n}&quot;,
         * &quot;{@code \r}&quot;, or &quot;{@code \r\n}&quot;.
         *
         * <p>NOTE: This method changed in 2.0.
         * It now more closely matches Perl chomp.</p>
         *
         * <pre>
         * StringTasks.chomp(null)          = null
         * StringTasks.chomp("")            = ""
         * StringTasks.chomp("abc \r")      = "abc "
         * StringTasks.chomp("abc\n")       = "abc"
         * StringTasks.chomp("abc\r\n")     = "abc"
         * StringTasks.chomp("abc\r\n\r\n") = "abc\r\n"
         * StringTasks.chomp("abc\n\r")     = "abc\n"
         * StringTasks.chomp("abc\n\rabc")  = "abc\n\rabc"
         * StringTasks.chomp("\r")          = ""
         * StringTasks.chomp("\n")          = ""
         * StringTasks.chomp("\r\n")        = ""
         * </pre>
         *
         * @param str  the String to chomp a newline from, may be null
         * @return String without newline, {@code null} if null String input
         */
        public static String chomp(final String str) {
            if (isEmpty(str)) {
                return str;
            }

            if (str.length() == 1) {
                final char ch = str.charAt(0);
                if (ch == CharTasks.CR || ch == CharTasks.LF) {
                    return EMPTY;
                }
                return str;
            }

            int lastIdx = str.length() - 1;
            final char last = str.charAt(lastIdx);

            if (last == CharTasks.LF) {
                if (str.charAt(lastIdx - 1) == CharTasks.CR) {
                    lastIdx--;
                }
            } else if (last != CharTasks.CR) {
                lastIdx++;
            }
            return str.substring(0, lastIdx);
        }

        /**
         * Splits a String by Character type as returned by
         * {@code java.lang.Character.getType(char)}. Groups of contiguous
         * characters of the same type are returned as complete tokens.
         * <pre>
         * StringTasks.splitByCharacterType(null)         = null
         * StringTasks.splitByCharacterType("")           = []
         * StringTasks.splitByCharacterType("ab de fg")   = ["ab", " ", "de", " ", "fg"]
         * StringTasks.splitByCharacterType("ab   de fg") = ["ab", "   ", "de", " ", "fg"]
         * StringTasks.splitByCharacterType("ab:cd:ef")   = ["ab", ":", "cd", ":", "ef"]
         * StringTasks.splitByCharacterType("number5")    = ["number", "5"]
         * StringTasks.splitByCharacterType("fooBar")     = ["foo", "B", "ar"]
         * StringTasks.splitByCharacterType("foo200Bar")  = ["foo", "200", "B", "ar"]
         * StringTasks.splitByCharacterType("ASFRules")   = ["ASFR", "ules"]
         * </pre>
         * @param str the String to split, may be {@code null}
         * @return an array of parsed Strings, {@code null} if null String input
         * @since 2.4
         */
        public static String[] splitByCharacterType(final String str) {
            return splitByCharacterType(str, false);
        }

        /**
         * <p>Splits a String by Character type as returned by
         * {@code java.lang.Character.getType(char)}. Groups of contiguous
         * characters of the same type are returned as complete tokens, with the
         * following exception: if {@code camelCase} is {@code true},
         * the character of type {@code Character.UPPERCASE_LETTER}, if any,
         * immediately preceding a token of type {@code Character.LOWERCASE_LETTER}
         * will belong to the following token rather than to the preceding, if any,
         * {@code Character.UPPERCASE_LETTER} token.
         * @param str the String to split, may be {@code null}
         * @param camelCase whether to use so-called "camel-case" for letter types
         * @return an array of parsed Strings, {@code null} if null String input
         * @since 2.4
         */
        private static String[] splitByCharacterType(final String str, final boolean camelCase) {
            if (str == null) {
                return null;
            }
            if (str.isEmpty()) {
                return ArrayTasks.EMPTY_STRING_ARRAY;
            }
            final char[] c = str.toCharArray();
            final List<String> list = new ArrayList<>();
            int tokenStart = 0;
            int currentType = Character.getType(c[tokenStart]);
            for (int pos = tokenStart + 1; pos < c.length - 1; pos++) {
                final int type = Character.getType(c[pos]);
                if (type == currentType) {
                    continue;
                }
                if (camelCase && type == Character.LOWERCASE_LETTER && currentType == Character.UPPERCASE_LETTER) {
                    final int newTokenStart = pos - 1;
                    if (newTokenStart != tokenStart) {
                        list.add(new String(c, tokenStart, newTokenStart - tokenStart));
                        tokenStart = newTokenStart;
                    }
                } else {
                    list.add(new String(c, tokenStart, pos - tokenStart));
                    tokenStart = pos;
                }
                currentType = type;
            }
            list.add(new String(c, tokenStart, c.length - tokenStart));
            return list.toArray(ArrayTasks.EMPTY_STRING_ARRAY);
        }


        /**
         * Checks if the CharSequence contains any character in the given
         * set of characters.
         *
         * <p>A {@code null} CharSequence will return {@code false}.
         * A {@code null} or zero length search array will return {@code false}.</p>
         *
         * <pre>
         * StringTasks.containsAny(null, *)                  = false
         * StringTasks.containsAny("", *)                    = false
         * StringTasks.containsAny(*, null)                  = false
         * StringTasks.containsAny(*, [])                    = false
         * StringTasks.containsAny("zzabyycdxx", ['z', 'a']) = true
         * StringTasks.containsAny("zzabyycdxx", ['b', 'y']) = true
         * StringTasks.containsAny("zzabyycdxx", ['z', 'y']) = true
         * StringTasks.containsAny("aba", ['z'])             = false
         * </pre>
         *
         * @param cs  the CharSequence to check, may be null
         * @param searchChars  the chars to search for, may be null
         * @return the {@code true} if any of the chars are found,
         * {@code false} if no match or null input
         * @since 2.4
         * @since 3.0 Changed signature from containsAny(String, char[]) to containsAny(CharSequence, char...)
         */
        public static boolean containsAny(final CharSequence cs, final char... searchChars) {
            if (isEmpty(cs) || ArrayTasks.isEmpty(searchChars)) {
                return false;
            }
            final int csLength = cs.length();
            final int searchLength = searchChars.length;
            final int csLast = csLength - 1;
            final int searchLast = searchLength - 1;
            for (int i = 0; i < csLength; i++) {
                final char ch = cs.charAt(i);
                for (int j = 0; j < searchLength; j++) {
                    if (searchChars[j] == ch) {
                        if (!Character.isHighSurrogate(ch)) {
                            // ch is in the Basic Multilingual Plane
                            return true;
                        }
                        if (j == searchLast) {
                            // missing low surrogate, fine, like String.indexOf(String)
                            return true;
                        }
                        if (i < csLast && searchChars[j + 1] == cs.charAt(i + 1)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        /**
         * Checks that the CharSequence does not contain certain characters.
         *
         * <p>A {@code null} CharSequence will return {@code true}.
         * A {@code null} invalid character array will return {@code true}.
         * An empty CharSequence (length()=0) always returns true.</p>
         *
         * <pre>
         * StringTasks.containsNone(null, *)       = true
         * StringTasks.containsNone(*, null)       = true
         * StringTasks.containsNone("", *)         = true
         * StringTasks.containsNone("ab", '')      = true
         * StringTasks.containsNone("abab", 'xyz') = true
         * StringTasks.containsNone("ab1", 'xyz')  = true
         * StringTasks.containsNone("abz", 'xyz')  = false
         * </pre>
         *
         * @param cs  the CharSequence to check, may be null
         * @param searchChars  an array of invalid chars, may be null
         * @return true if it contains none of the invalid chars, or is null
         * @since 2.0
         * @since 3.0 Changed signature from containsNone(String, char[]) to containsNone(CharSequence, char...)
         */
        public static boolean containsNone(final CharSequence cs, final char... searchChars) {
            if (cs == null || searchChars == null) {
                return true;
            }
            final int csLen = cs.length();
            final int csLast = csLen - 1;
            final int searchLen = searchChars.length;
            final int searchLast = searchLen - 1;
            for (int i = 0; i < csLen; i++) {
                final char ch = cs.charAt(i);
                for (int j = 0; j < searchLen; j++) {
                    if (searchChars[j] == ch) {
                        if (!Character.isHighSurrogate(ch)) {
                            // ch is in the Basic Multilingual Plane
                            return false;
                        }
                        if (j == searchLast) {
                            // missing low surrogate, fine, like String.indexOf(String)
                            return false;
                        }
                        if (i < csLast && searchChars[j + 1] == cs.charAt(i + 1)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        /**
         * <p>Splits a String by Character type as returned by
         * {@code java.lang.Character.getType(char)}. Groups of contiguous
         * characters of the same type are returned as complete tokens, with the
         * following exception: the character of type
         * {@code Character.UPPERCASE_LETTER}, if any, immediately
         * preceding a token of type {@code Character.LOWERCASE_LETTER}
         * will belong to the following token rather than to the preceding, if any,
         * {@code Character.UPPERCASE_LETTER} token.
         * <pre>
         * StringTasks.splitByCharacterTypeCamelCase(null)         = null
         * StringTasks.splitByCharacterTypeCamelCase("")           = []
         * StringTasks.splitByCharacterTypeCamelCase("ab de fg")   = ["ab", " ", "de", " ", "fg"]
         * StringTasks.splitByCharacterTypeCamelCase("ab   de fg") = ["ab", "   ", "de", " ", "fg"]
         * StringTasks.splitByCharacterTypeCamelCase("ab:cd:ef")   = ["ab", ":", "cd", ":", "ef"]
         * StringTasks.splitByCharacterTypeCamelCase("number5")    = ["number", "5"]
         * StringTasks.splitByCharacterTypeCamelCase("fooBar")     = ["foo", "Bar"]
         * StringTasks.splitByCharacterTypeCamelCase("foo200Bar")  = ["foo", "200", "Bar"]
         * StringTasks.splitByCharacterTypeCamelCase("ASFRules")   = ["ASF", "Rules"]
         * </pre>
         * @param str the String to split, may be {@code null}
         * @return an array of parsed Strings, {@code null} if null String input
         * @since 2.4
         */
        public static String[] splitByCharacterTypeCamelCase(final String str) {
            return splitByCharacterType(str, true);
        }

        /**
         * Compares two CharSequences, returning {@code true} if they represent
         * equal sequences of characters.
         *
         * <p>{@code null}s are handled without exceptions. Two {@code null}
         * references are considered to be equal. The comparison is <strong>case-sensitive</strong>.</p>
         *
         * <pre>
         * StringTasks.equals(null, null)   = true
         * StringTasks.equals(null, "abc")  = false
         * StringTasks.equals("abc", null)  = false
         * StringTasks.equals("abc", "abc") = true
         * StringTasks.equals("abc", "ABC") = false
         * </pre>
         *
         * @param cs1  the first CharSequence, may be {@code null}
         * @param cs2  the second CharSequence, may be {@code null}
         * @return {@code true} if the CharSequences are equal (case-sensitive), or both {@code null}
         * @since 3.0 Changed signature from equals(String, String) to equals(CharSequence, CharSequence)
         * @see Object#equals(Object)
         * @see #equalsIgnoreCase(CharSequence, CharSequence)
         */
        public static boolean equals(final CharSequence cs1, final CharSequence cs2) {
            if (cs1 == cs2) {
                return true;
            }
            if (cs1 == null || cs2 == null) {
                return false;
            }
            if (cs1.length() != cs2.length()) {
                return false;
            }
            if (cs1 instanceof String && cs2 instanceof String) {
                return cs1.equals(cs2);
            }
            // Step-wise comparison
            final int length = cs1.length();
            for (int i = 0; i < length; i++) {
                if (cs1.charAt(i) != cs2.charAt(i)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Strips any of a set of characters from the end of a String.
         *
         * <p>A {@code null} input String returns {@code null}.
         * An empty string ("") input returns the empty string.</p>
         *
         * <p>If the stripChars String is {@code null}, whitespace is
         * stripped as defined by {@link Character#isWhitespace(char)}.</p>
         *
         * <pre>
         * StringTasks.stripEnd(null, *)          = null
         * StringTasks.stripEnd("", *)            = ""
         * StringTasks.stripEnd("abc", "")        = "abc"
         * StringTasks.stripEnd("abc", null)      = "abc"
         * StringTasks.stripEnd("  abc", null)    = "  abc"
         * StringTasks.stripEnd("abc  ", null)    = "abc"
         * StringTasks.stripEnd(" abc ", null)    = " abc"
         * StringTasks.stripEnd("  abcyx", "xyz") = "  abc"
         * StringTasks.stripEnd("120.00", ".0")   = "12"
         * </pre>
         *
         * @param str  the String to remove characters from, may be null
         * @param stripChars  the set of characters to remove, null treated as whitespace
         * @return the stripped String, {@code null} if null String input
         */
        public static String stripEnd(final String str, final String stripChars) {
            int end = length(str);
            if (end == 0) {
                return str;
            }

            if (stripChars == null) {
                while (end != 0 && Character.isWhitespace(str.charAt(end - 1))) {
                    end--;
                }
            } else if (stripChars.isEmpty()) {
                return str;
            } else {
                while (end != 0 && stripChars.indexOf(str.charAt(end - 1)) != INDEX_NOT_FOUND) {
                    end--;
                }
            }
            return str.substring(0, end);
        }

        /**
         * Checks if the CharSequence contains mixed casing of both uppercase and lowercase characters.
         *
         * <p>{@code null} will return {@code false}. An empty CharSequence ({@code length()=0}) will return
         * {@code false}.</p>
         *
         * <pre>
         * StringTasks.isMixedCase(null)    = false
         * StringTasks.isMixedCase("")      = false
         * StringTasks.isMixedCase(" ")     = false
         * StringTasks.isMixedCase("ABC")   = false
         * StringTasks.isMixedCase("abc")   = false
         * StringTasks.isMixedCase("aBc")   = true
         * StringTasks.isMixedCase("A c")   = true
         * StringTasks.isMixedCase("A1c")   = true
         * StringTasks.isMixedCase("a/C")   = true
         * StringTasks.isMixedCase("aC\t")  = true
         * </pre>
         *
         * @param cs the CharSequence to check, may be null
         * @return {@code true} if the CharSequence contains both uppercase and lowercase characters
         * @since 3.5
         */
        public static boolean isMixedCase(final CharSequence cs) {
            if (isEmpty(cs) || cs.length() == 1) {
                return false;
            }
            boolean containsUppercase = false;
            boolean containsLowercase = false;
            final int sz = cs.length();
            for (int i = 0; i < sz; i++) {
                if (containsUppercase && containsLowercase) {
                    return true;
                }
                if (Character.isUpperCase(cs.charAt(i))) {
                    containsUppercase = true;
                } else if (Character.isLowerCase(cs.charAt(i))) {
                    containsLowercase = true;
                }
            }
            return containsUppercase && containsLowercase;
        }

        /**
         * <p>Replace part of a <code>String</code> with another value.</p>
         *
         * @param value <code>String</code> to perform the replacement on.
         * @param key The name of the constant.
         * @param replaceValue The value of the constant.
         *
         * @return The modified value.
         */
        public static String replace(String value, final String key, final String replaceValue) {

            if (value == null || key == null || replaceValue == null) {
                return value;
            }

            final int pos = value.indexOf(key);

            if (pos < 0) {
                return value;
            }

            final int length = value.length();
            final int start = pos;
            final int end = pos + key.length();

            if (length == key.length()) {
                value = replaceValue;

            } else if (end == length) {
                value = value.substring(0, start) + replaceValue;

            } else {
                value =
                        value.substring(0, start)
                        + replaceValue
                        + replace(value.substring(end), key, replaceValue);
            }

            return value;
        }
        
    }

    /**
     * Provides extra functionality for Java Number classes.
     *
     * @since 2.0
     */
    public static class NumberTasks {

        /**
         * Turns a string value into a java.lang.Number.
         *
         * <p>If the string starts with {@code 0x} or {@code -0x} (lower or upper case) or {@code #} or {@code -#}, it
         * will be interpreted as a hexadecimal Integer - or Long, if the number of digits after the
         * prefix is more than 8 - or BigInteger if there are more than 16 digits.
         * </p>
         * <p>Then, the value is examined for a type qualifier on the end, i.e. one of
         * {@code 'f', 'F', 'd', 'D', 'l', 'L'}.  If it is found, it starts
         * trying to create successively larger types from the type specified
         * until one is found that can represent the value.</p>
         *
         * <p>If a type specifier is not found, it will check for a decimal point
         * and then try successively larger types from {@link Integer} to
         * {@link BigInteger} and from {@link Float} to
         * {@link BigDecimal}.</p>
         *
         * <p>
         * Integral values with a leading {@code 0} will be interpreted as octal; the returned number will
         * be Integer, Long or BigDecimal as appropriate.
         * </p>
         *
         * <p>Returns {@code null} if the string is {@code null}.</p>
         *
         * <p>This method does not trim the input string, i.e., strings with leading
         * or trailing spaces will generate NumberFormatExceptions.</p>
         *
         * @param str  String containing a number, may be null
         * @return Number created from the string (or null if the input is null)
         * @throws NumberFormatException if the value cannot be converted
         */
        public static Number createNumber(final String str) {
            if (str == null) {
                return null;
            }
            if (StringTasks.isBlank(str)) {
                throw new NumberFormatException("A blank string is not a valid number");
            }
            // Need to deal with all possible hex prefixes here
            final String[] hex_prefixes = {"0x", "0X", "#"};
            final int length = str.length();
            final int offset = str.charAt(0) == '+' || str.charAt(0) == '-' ? 1 : 0;
            int pfxLen = 0;
            for (final String pfx : hex_prefixes) {
                if (str.startsWith(pfx, offset)) {
                    pfxLen += pfx.length() + offset;
                    break;
                }
            }
            if (pfxLen > 0) { // we have a hex number
                char firstSigDigit = 0; // strip leading zeroes
                for (int i = pfxLen; i < length; i++) {
                    firstSigDigit = str.charAt(i);
                    if (firstSigDigit != '0') {
                        break;
                    }
                    pfxLen++;
                }
                final int hexDigits = length - pfxLen;
                if (hexDigits > 16 || hexDigits == 16 && firstSigDigit > '7') { // too many for Long
                    return createBigInteger(str);
                }
                if (hexDigits > 8 || hexDigits == 8 && firstSigDigit > '7') { // too many for an int
                    return createLong(str);
                }
                return createInteger(str);
            }
            final char lastChar = str.charAt(length - 1);
            final String mant;
            final String dec;
            final String exp;
            final int decPos = str.indexOf('.');
            final int expPos = str.indexOf('e') + str.indexOf('E') + 1; // assumes both not present
            // if both e and E are present, this is caught by the checks on expPos (which prevent IOOBE)
            // and the parsing which will detect if e or E appear in a number due to using the wrong offset

            // Detect if the return type has been requested
            final boolean requestType = !Character.isDigit(lastChar) && lastChar != '.';
            if (decPos > -1) { // there is a decimal point
                if (expPos > -1) { // there is an exponent
                    if (expPos < decPos || expPos > length) { // prevents double exponent causing IOOBE
                        throw new NumberFormatException(str + " is not a valid number.");
                    }
                    dec = str.substring(decPos + 1, expPos);
                } else {
                    // No exponent, but there may be a type character to remove
                    dec = str.substring(decPos + 1, requestType ? length - 1 : length);
                }
                mant = getMantissa(str, decPos);
            } else {
                if (expPos > -1) {
                    if (expPos > length) { // prevents double exponent causing IOOBE
                        throw new NumberFormatException(str + " is not a valid number.");
                    }
                    mant = getMantissa(str, expPos);
                } else {
                    // No decimal, no exponent, but there may be a type character to remove
                    mant = getMantissa(str, requestType ? length - 1 : length);
                }
                dec = null;
            }
            if (requestType) {
                if (expPos > -1 && expPos < length - 1) {
                    exp = str.substring(expPos + 1, length - 1);
                } else {
                    exp = null;
                }
                //Requesting a specific type.
                final String numeric = str.substring(0, length - 1);
                
                switch (lastChar) {
                    case 'l' :
                    case 'L' :
                        if (dec == null
                            && exp == null
                            && (!numeric.isEmpty() && numeric.charAt(0) == '-' && isDigits(numeric.substring(1)) || isDigits(numeric))) {
                            try {
                                return createLong(numeric);
                            } catch (final NumberFormatException ignored) {
                                // Too big for a long
                            }
                            return createBigInteger(numeric);

                        }
                        throw new NumberFormatException(str + " is not a valid number.");
                    case 'f' :
                    case 'F' :
                        try {
                            final Float f = createFloat(str);
                            if (!(f.isInfinite() || f.floatValue() == 0.0F && !isZero(mant, dec))) {
                                //If it's too big for a float or the float value = 0 and the string
                                //has non-zeros in it, then float does not have the precision we want
                                return f;
                            }

                        } catch (final NumberFormatException ignored) {
                            // ignore the bad number
                        }
                        //$FALL-THROUGH$
                    case 'd' :
                    case 'D' :
                        try {
                            final Double d = createDouble(str);
                            if (!(d.isInfinite() || d.doubleValue() == 0.0D && !isZero(mant, dec))) {
                                return d;
                            }
                        } catch (final NumberFormatException ignored) {
                            // ignore the bad number
                        }
                        try {
                            return createBigDecimal(numeric);
                        } catch (final NumberFormatException ignored) {
                            // ignore the bad number
                        }
                        //$FALL-THROUGH$
                    default :
                        throw new NumberFormatException(str + " is not a valid number.");
                }
            }
            //User doesn't have a preference on the return type, so let's start
            //small and go from there...
            if (expPos > -1 && expPos < length - 1) {
                exp = str.substring(expPos + 1);
            } else {
                exp = null;
            }
            if (dec == null && exp == null) { // no decimal point and no exponent
                //Must be an Integer, Long, Biginteger
                try {
                    return createInteger(str);
                } catch (final NumberFormatException ignored) {
                    // ignore the bad number
                }
                try {
                    return createLong(str);
                } catch (final NumberFormatException ignored) {
                    // ignore the bad number
                }
                return createBigInteger(str);
            }

            //Must be a Float, Double, BigDecimal
            try {
                final Float f = createFloat(str);
                final Double d = createDouble(str);
                if (!f.isInfinite()
                        && !(f.floatValue() == 0.0F && !isZero(mant, dec))
                        && f.toString().equals(d.toString())) {
                    return f;
                }
                if (!d.isInfinite() && !(d.doubleValue() == 0.0D && !isZero(mant, dec))) {
                    final BigDecimal b = createBigDecimal(str);
                    if (b.compareTo(BigDecimal.valueOf(d.doubleValue())) == 0) {
                        return d;
                    }
                    return b;
                }
            } catch (final NumberFormatException ignored) {
                // ignore the bad number
            }
            return createBigDecimal(str);
        }

        /**
         * Utility method for {@link #createNumber(java.lang.String)}.
         *
         * <p>Returns mantissa of the given number.</p>
         *
         * @param str the string representation of the number
         * @param stopPos the position of the exponent or decimal point
         * @return mantissa of the given number
         */
        private static String getMantissa(final String str, final int stopPos) {
            final char firstChar = str.charAt(0);
            final boolean hasSign = firstChar == '-' || firstChar == '+';

            return hasSign ? str.substring(1, stopPos) : str.substring(0, stopPos);
        }

        /**
         * Utility method for {@link #createNumber(java.lang.String)}.
         *
         * <p>This will check if the magnitude of the number is zero by checking if there
         * are only zeros before and after the decimal place.</p>
         *
         * <p>Note: It is <strong>assumed</strong> that the input string has been converted
         * to either a Float or Double with a value of zero when this method is called.
         * This eliminates invalid input for example {@code ".", ".D", ".e0"}.</p>
         *
         * <p>Thus the method only requires checking if both arguments are null, empty or
         * contain only zeros.</p>
         *
         * <p>Given {@code s = mant + "." + dec}:</p>
         * <ul>
         * <li>{@code true} if s is {@code "0.0"}
         * <li>{@code true} if s is {@code "0."}
         * <li>{@code true} if s is {@code ".0"}
         * <li>{@code false} otherwise (this assumes {@code "."} is not possible)
         * </ul>
         *
         * @param mant the mantissa decimal digits before the decimal point (sign must be removed; never null)
         * @param dec the decimal digits after the decimal point (exponent and type specifier removed;
         *            can be null)
         * @return true if the magnitude is zero
         */
        private static boolean isZero(final String mant, final String dec) {
            return isAllZeros(mant) && isAllZeros(dec);
        }

        /**
         * Utility method for {@link #createNumber(java.lang.String)}.
         *
         * <p>Returns {@code true} if s is {@code null} or empty.</p>
         *
         * @param str the String to check
         * @return if it is all zeros or {@code null}
         */
        private static boolean isAllZeros(final String str) {
            if (str == null) {
                return true;
            }
            for (int i = str.length() - 1; i >= 0; i--) {
                if (str.charAt(i) != '0') {
                    return false;
                }
            }
            return true;
        }

        /**
         * Convert a {@link String} to a {@link Float}.
         *
         * <p>Returns {@code null} if the string is {@code null}.</p>
         *
         * @param str  a {@link String} to convert, may be null
         * @return converted {@link Float} (or null if the input is null)
         * @throws NumberFormatException if the value cannot be converted
         */
        public static Float createFloat(final String str) {
            if (str == null) {
                return null;
            }
            return Float.valueOf(str);
        }

        /**
         * Convert a {@link String} to a {@link Double}.
         *
         * <p>Returns {@code null} if the string is {@code null}.</p>
         *
         * @param str  a {@link String} to convert, may be null
         * @return converted {@link Double} (or null if the input is null)
         * @throws NumberFormatException if the value cannot be converted
         */
        public static Double createDouble(final String str) {
            if (str == null) {
                return null;
            }
            return Double.valueOf(str);
        }

        /**
         * Convert a {@link String} to a {@link Integer}, handling
         * hex (0xhhhh) and octal (0dddd) notations.
         * N.B. a leading zero means octal; spaces are not trimmed.
         *
         * <p>Returns {@code null} if the string is {@code null}.</p>
         *
         * @param str  a {@link String} to convert, may be null
         * @return converted {@link Integer} (or null if the input is null)
         * @throws NumberFormatException if the value cannot be converted
         */
        public static Integer createInteger(final String str) {
            if (str == null) {
                return null;
            }
            // decode() handles 0xAABD and 0777 (hex and octal) as well.
            return Integer.decode(str);
        }

        /**
         * Convert a {@link String} to a {@link Long};
         * since 3.1 it handles hex (0Xhhhh) and octal (0ddd) notations.
         * N.B. a leading zero means octal; spaces are not trimmed.
         *
         * <p>Returns {@code null} if the string is {@code null}.</p>
         *
         * @param str  a {@link String} to convert, may be null
         * @return converted {@link Long} (or null if the input is null)
         * @throws NumberFormatException if the value cannot be converted
         */
        public static Long createLong(final String str) {
            if (str == null) {
                return null;
            }
            return Long.decode(str);
        }

        /**
         * Convert a {@link String} to a {@link BigInteger};
         * since 3.2 it handles hex (0x or #) and octal (0) notations.
         *
         * <p>Returns {@code null} if the string is {@code null}.</p>
         *
         * @param str  a {@link String} to convert, may be null
         * @return converted {@link BigInteger} (or null if the input is null)
         * @throws NumberFormatException if the value cannot be converted
         */
        public static BigInteger createBigInteger(final String str) {
            if (str == null) {
                return null;
            }
            if (str.isEmpty()) {
                throw new NumberFormatException("An empty string is not a valid number");
            }
            int pos = 0; // offset within string
            int radix = 10;
            boolean negate = false; // need to negate later?
            final char char0 = str.charAt(0);
            if (char0 == '-') {
                negate = true;
                pos = 1;
            } else if (char0 == '+') {
                pos = 1;
            }
            if (str.startsWith("0x", pos) || str.startsWith("0X", pos)) { // hex
                radix = 16;
                pos += 2;
            } else if (str.startsWith("#", pos)) { // alternative hex (allowed by Long/Integer)
                radix = 16;
                pos++;
            } else if (str.startsWith("0", pos) && str.length() > pos + 1) { // octal; so long as there are additional digits
                radix = 8;
                pos++;
            } // default is to treat as decimal

            final BigInteger value = new BigInteger(str.substring(pos), radix);
            return negate ? value.negate() : value;
        }

        /**
         * Convert a {@link String} to a {@link BigDecimal}.
         *
         * <p>Returns {@code null} if the string is {@code null}.</p>
         *
         * @param str  a {@link String} to convert, may be null
         * @return converted {@link BigDecimal} (or null if the input is null)
         * @throws NumberFormatException if the value cannot be converted
         */
        public static BigDecimal createBigDecimal(final String str) {
            if (str == null) {
                return null;
            }
            // handle JDK1.3.1 bug where "" throws IndexOutOfBoundsException
            if (StringTasks.isBlank(str)) {
                throw new NumberFormatException("A blank string is not a valid number");
            }
            return new BigDecimal(str);
        }

        /**
         * Checks whether the {@link String} contains only
         * digit characters.
         *
         * <p>{@code null} and empty String will return
         * {@code false}.</p>
         *
         * @param str  the {@link String} to check
         * @return {@code true} if str contains only Unicode numeric
         */
        public static boolean isDigits(final String str) {
            return StringTasks.isNumeric(str);
        }

        /**
         * Checks whether the given String is a parsable number.
         *
         * <p>Parsable numbers include those Strings understood by {@link Integer#parseInt(String)},
         * {@link Long#parseLong(String)}, {@link Float#parseFloat(String)} or
         * {@link Double#parseDouble(String)}. This method can be used instead of catching {@link java.text.ParseException}
         * when calling one of those methods.</p>
         *
         * <p>Hexadecimal and scientific notations are <strong>not</strong> considered parsable.
         * See {@link #isCreatable(String)} on those cases.</p>
         *
         * <p>{@code null} and empty String will return {@code false}.</p>
         *
         * @param str the String to check.
         * @return {@code true} if the string is a parsable number.
         * @since 3.4
         */
        public static boolean isParsable(final String str) {
            if (StringTasks.isEmpty(str)) {
                return false;
            }
            if (str.charAt(str.length() - 1) == '.') {
                return false;
            }
            if (str.charAt(0) == '-') {
                if (str.length() == 1) {
                    return false;
                }
                return withDecimalsParsing(str, 1);
            }
            return withDecimalsParsing(str, 0);
        }

        private static boolean withDecimalsParsing(final String str, final int beginIdx) {
            int decimalPoints = 0;
            for (int i = beginIdx; i < str.length(); i++) {
                final boolean isDecimalPoint = str.charAt(i) == '.';
                if (isDecimalPoint) {
                    decimalPoints++;
                }
                if (decimalPoints > 1) {
                    return false;
                }
                if (!isDecimalPoint && !Character.isDigit(str.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Compares two {@code byte} values numerically. This is the same functionality as provided in Java 7.
         *
         * @param x the first {@code byte} to compare
         * @param y the second {@code byte} to compare
         * @return the value {@code 0} if {@code x == y};
         *         a value less than {@code 0} if {@code x < y}; and
         *         a value greater than {@code 0} if {@code x > y}
         * @since 3.4
         */
        public static int compare(final byte x, final byte y) {
            return x - y;
        }
    }

    public static class BooleanTasks {
        /**
         * Compares two {@code boolean} values. This is the same functionality as provided in Java 7.
         *
         * @param x the first {@code boolean} to compare
         * @param y the second {@code boolean} to compare
         * @return the value {@code 0} if {@code x == y};
         *         a value less than {@code 0} if {@code !x && y}; and
         *         a value greater than {@code 0} if {@code x && !y}
         * @since 3.4
         */
        public static int compare(final boolean x, final boolean y) {
            if (x == y) {
                return 0;
            }
            return x ? 1 : -1;
        }
    }
    
    public static class CharTasks {

        /**
         * Linefeed character LF ({@code '\n'}, Unicode 000a).
         *
         * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-3.10.6">JLF: Escape Sequences
         *      for Character and String Literals</a>
         * @since 2.2
         */
        public static final char LF = '\n';
    
        /**
         * Carriage return character CR ('\r', Unicode 000d).
         *
         * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-3.10.6">JLF: Escape Sequences
         *      for Character and String Literals</a>
         * @since 2.2
         */
        public static final char CR = '\r';
    
        /**
         * {@code \u0000} null control character ('\0'), abbreviated NUL.
         *
         * @since 3.6
         */
        public static final char NUL = '\0';
   
        /**
         * Converts the Character to a char handling {@code null}.
         *
         * <pre>
         *   CharTasks.toChar(null, 'X') = 'X'
         *   CharTasks.toChar(' ', 'X')  = ' '
         *   CharTasks.toChar('A', 'X')  = 'A'
         * </pre>
         *
         * @param ch  the character to convert
         * @param defaultValue  the value to use if the  Character is null
         * @return the char value of the Character or the default if null
         */
        public static char toChar(final Character ch, final char defaultValue) {
            return ch != null ? ch.charValue() : defaultValue;
        }
    
        /**
         * Checks whether the character is ASCII 7 bit.
         *
         * <pre>
         *   CharTasks.isAscii('a')  = true
         *   CharTasks.isAscii('A')  = true
         *   CharTasks.isAscii('3')  = true
         *   CharTasks.isAscii('-')  = true
         *   CharTasks.isAscii('\n') = true
         *   CharTasks.isAscii('&copy;') = false
         * </pre>
         *
         * @param ch  the character to check
         * @return true if less than 128
         */
        public static boolean isAscii(final char ch) {
            return ch < 128;
        }
    
        /**
         * Checks whether the character is ASCII 7 bit printable.
         *
         * <pre>
         *   CharTasks.isAsciiPrintable('a')  = true
         *   CharTasks.isAsciiPrintable('A')  = true
         *   CharTasks.isAsciiPrintable('3')  = true
         *   CharTasks.isAsciiPrintable('-')  = true
         *   CharTasks.isAsciiPrintable('\n') = false
         *   CharTasks.isAsciiPrintable('&copy;') = false
         * </pre>
         *
         * @param ch  the character to check
         * @return true if between 32 and 126 inclusive
         */
        public static boolean isAsciiPrintable(final char ch) {
            return ch >= 32 && ch < 127;
        }
    
        /**
         * Checks whether the character is ASCII 7 bit control.
         *
         * <pre>
         *   CharTasks.isAsciiControl('a')  = false
         *   CharTasks.isAsciiControl('A')  = false
         *   CharTasks.isAsciiControl('3')  = false
         *   CharTasks.isAsciiControl('-')  = false
         *   CharTasks.isAsciiControl('\n') = true
         *   CharTasks.isAsciiControl('&copy;') = false
         * </pre>
         *
         * @param ch  the character to check
         * @return true if less than 32 or equals 127
         */
        public static boolean isAsciiControl(final char ch) {
            return ch < 32 || ch == 127;
        }
    
        /**
         * Checks whether the character is ASCII 7 bit alphabetic.
         *
         * <pre>
         *   CharTasks.isAsciiAlpha('a')  = true
         *   CharTasks.isAsciiAlpha('A')  = true
         *   CharTasks.isAsciiAlpha('3')  = false
         *   CharTasks.isAsciiAlpha('-')  = false
         *   CharTasks.isAsciiAlpha('\n') = false
         *   CharTasks.isAsciiAlpha('&copy;') = false
         * </pre>
         *
         * @param ch  the character to check
         * @return true if between 65 and 90 or 97 and 122 inclusive
         */
        public static boolean isAsciiAlpha(final char ch) {
            return isAsciiAlphaUpper(ch) || isAsciiAlphaLower(ch);
        }
    
        /**
         * Checks whether the character is ASCII 7 bit alphabetic upper case.
         *
         * <pre>
         *   CharTasks.isAsciiAlphaUpper('a')  = false
         *   CharTasks.isAsciiAlphaUpper('A')  = true
         *   CharTasks.isAsciiAlphaUpper('3')  = false
         *   CharTasks.isAsciiAlphaUpper('-')  = false
         *   CharTasks.isAsciiAlphaUpper('\n') = false
         *   CharTasks.isAsciiAlphaUpper('&copy;') = false
         * </pre>
         *
         * @param ch  the character to check
         * @return true if between 65 and 90 inclusive
         */
        public static boolean isAsciiAlphaUpper(final char ch) {
            return ch >= 'A' && ch <= 'Z';
        }
    
        /**
         * Checks whether the character is ASCII 7 bit alphabetic lower case.
         *
         * <pre>
         *   CharTasks.isAsciiAlphaLower('a')  = true
         *   CharTasks.isAsciiAlphaLower('A')  = false
         *   CharTasks.isAsciiAlphaLower('3')  = false
         *   CharTasks.isAsciiAlphaLower('-')  = false
         *   CharTasks.isAsciiAlphaLower('\n') = false
         *   CharTasks.isAsciiAlphaLower('&copy;') = false
         * </pre>
         *
         * @param ch  the character to check
         * @return true if between 97 and 122 inclusive
         */
        public static boolean isAsciiAlphaLower(final char ch) {
            return ch >= 'a' && ch <= 'z';
        }
    
        /**
         * Compares two {@code char} values numerically. This is the same functionality as provided in Java 7.
         *
         * @param x the first {@code char} to compare
         * @param y the second {@code char} to compare
         * @return the value {@code 0} if {@code x == y};
         *         a value less than {@code 0} if {@code x < y}; and
         *         a value greater than {@code 0} if {@code x > y}
         * @since 3.4
         */
        public static int compare(final char x, final char y) {
            return x - y;
        }

    }

    public static class ArrayTasks {

        /**
         * An empty immutable {@link String} array.
         */
        public static final String[] EMPTY_STRING_ARRAY = {};

        /**
         * The index value when an element is not found in a list or array: {@code -1}.
         * This value is returned by methods in this class and can also be used in comparisons with values returned by
         * various method from {@link java.util.List}.
         */
        public static final int INDEX_NOT_FOUND = -1;
        
        /**
         * Returns the length of the specified array.
         * This method can deal with {@link Object} arrays and with primitive arrays.
         * <p>
         * If the input array is {@code null}, {@code 0} is returned.
         * </p>
         * <pre>
         * ArrayTasks.getLength(null)            = 0
         * ArrayTasks.getLength([])              = 0
         * ArrayTasks.getLength([null])          = 1
         * ArrayTasks.getLength([true, false])   = 2
         * ArrayTasks.getLength([1, 2, 3])       = 3
         * ArrayTasks.getLength(["a", "b", "c"]) = 3
         * </pre>
         *
         * @param array  the array to retrieve the length from, may be null
         * @return The length of the array, or {@code 0} if the array is {@code null}
         * @throws IllegalArgumentException if the object argument is not an array.
         * @since 2.1
         */
        public static int getLength(final Object array) {
            return array != null ? Array.getLength(array) : 0;
        }
        
        /**
         * Checks if an array is empty or {@code null}.
         *
         * @param array the array to test
         * @return {@code true} if the array is empty or {@code null}
         */
        private static boolean isArrayEmpty(final Object array) {
            return getLength(array) == 0;
        }

        /**
         * Checks if an array of primitive chars is empty or {@code null}.
         *
         * @param array  the array to test
         * @return {@code true} if the array is empty or {@code null}
         * @since 2.1
         */
        public static boolean isEmpty(final char[] array) {
            return isArrayEmpty(array);
        }

        /**
         * Checks if an array of primitive ints is empty or {@code null}.
         *
         * @param array  the array to test
         * @return {@code true} if the array is empty or {@code null}
         * @since 2.1
         */
        public static boolean isEmpty(final int[] array) {
            return isArrayEmpty(array);
        }

        /**
         * Finds the last index of the given object in the array starting at the given index.
         * <p>
         * This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
         * </p>
         * <p>
         * A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than
         * the array length will search from the end of the array.
         * </p>
         *
         * @param array  the array to traverse for looking for the object, may be {@code null}
         * @param objectToFind  the object to find, may be {@code null}
         * @param startIndex  the start index to traverse backwards from
         * @return the last index of the object within the array,
         *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
         */
        public static int lastIndexOf(final Object[] array, final Object objectToFind, int startIndex) {
            if (array == null || startIndex < 0) {
                return INDEX_NOT_FOUND;
            }
            if (startIndex >= array.length) {
                startIndex = array.length - 1;
            }
            if (objectToFind == null) {
                for (int i = startIndex; i >= 0; i--) {
                    if (array[i] == null) {
                        return i;
                    }
                }
            } else if (array.getClass().getComponentType().isInstance(objectToFind)) {
                for (int i = startIndex; i >= 0; i--) {
                    if (objectToFind.equals(array[i])) {
                        return i;
                    }
                }
            }
            return INDEX_NOT_FOUND;
        }
    }

    public static final class GregorianTasks {

        //~ Statische Felder/Initialisierungen --------------------------------
    
        /**
         * Minimum of supported year range (-999999999).
         */
        /*[deutsch]
         * Minimal unterst&uuml;tze Jahreszahl (-999999999).
         */
        public static final int MIN_YEAR = -999_999_999;
    
        /**
         * Maximum of supported year range (999999999).
         */
        /*[deutsch]
         * Maximal unterst&uuml;tze Jahreszahl (999999999).
         */
        public static final int MAX_YEAR = 999_999_999;
    
        //~ Konstruktoren -----------------------------------------------------
    
        //~ Methoden ----------------------------------------------------------
    
        /**
         * <p>
         * Queries if given year is a gregorian leap year.
         * In the Gregorian calendar, each leap year has 366 days instead of 365, 
         * by extending February to 29 days rather than the common 28. These extra 
         * days occur in each year that is an integer multiple of 4 
         * (except for years evenly divisible by 100, but not by 400)
         * </p>
         *
         * @param   year    number of proleptic year
         * @return  {@code true} if it is a leap year else {@code false}
         */
        /*[deutsch]
         * <p>Ist das angegebene Jahr ein gregorianisches Schaltjahr? </p>
         *
         * @param   year    proleptisches Jahr
         * @return  {@code true} if it is a leap year else {@code false}
         */
        public static boolean isLeapYear(int year) {
    
            if ((year > 1900) && (year < 2100)) {
                return ((year & 3) == 0);
            }
    
            return ((year & 3) == 0) || (((year % 100) != 0) || ((year % 400) == 0));
    
        }
    
        /**
         * <p>Determines the maximum length of month in days dependent on given
         * year (leap years!) and month. </p>
         *
         * @param   year    proleptic iso year
         * @param   month   gregorian month (1-12)
         * @return  length of month in days
         * @throws  IllegalArgumentException if month is out of range (1-12)
         */
        /*[deutsch]
         * <p>Ermittelt die maximale L&auml;nge des Monats in Tagen abh&auml;ngig
         * vom angegebenen Jahr (Schaltjahre!) und Monat. </p>
         *
         * @param   year    proleptic iso year
         * @param   month   gregorian month (1-12)
         * @return  length of month in days
         * @throws  IllegalArgumentException if month is out of range (1-12)
         */
        public static int getLengthOfMonth(
            int year,
            int month
        ) {
            //   turn switch-case to if-else
            if (month==1 || month==3 || month==5 || month==7 || month==8 || month==10 || month==12){return 31;}
            else if (month==4 || month==6 || month==9 || month==11) {return 30;}
            else if (month==2) {return (isLeapYear(year) ? 29 : 28);}
            else {throw new IllegalArgumentException("Invalid month: " + month);}

            // switch (month) {
            //     case 1:
            //     case 3:
            //     case 5:
            //     case 7:
            //     case 8:
            //     case 10:
            //     case 12:
            //         return 31;
            //     case 4:
            //     case 6:
            //     case 9:
            //     case 11:
            //         return 30;
            //     case 2:
            //         return (isLeapYear(year) ? 29 : 28);
            //     default:
            //         throw new IllegalArgumentException("Invalid month: " + month);
            // }
    
        }
    
        /**
         * <p>Checks the range limits of date values according to the rules
         * of gregorian calendar. </p>
         *
         * @param   year        proleptic iso year [(-999999999) - 999999999]
         * @param   month       gregorian month (1-12)
         * @param   dayOfMonth  day of month (1-31)
         * @throws  IllegalArgumentException if any argument is out of range
         * @see     #isValid(int, int, int)
         */
        /*[deutsch]
         * <p>&Uuml;berpr&uuml;ft die Bereichsgrenzen der Datumswerte nach
         * den gregorianischen Kalenderregeln. </p>
         *
         * @param   year        proleptic iso year [(-999999999) - 999999999]
         * @param   month       gregorian month (1-12)
         * @param   dayOfMonth  day of month (1-31)
         * @throws  IllegalArgumentException if any argument is out of range
         * @see     #isValid(int, int, int)
         */
        public static void checkDate(
            int year,
            int month,
            int dayOfMonth
        ) {
    
            if (year < MIN_YEAR || year > MAX_YEAR) {
                throw new IllegalArgumentException(
                    "YEAR out of range: " + year);
            } else if ((month < 1) || (month > 12)) {
                throw new IllegalArgumentException(
                    "MONTH out of range: " + month);
            } else if ((dayOfMonth < 1) || (dayOfMonth > 31)) {
                throw new IllegalArgumentException(
                    "DAY_OF_MONTH out of range: " + dayOfMonth);
            } else if (dayOfMonth > getLengthOfMonth(year, month)) {
                throw new IllegalArgumentException(
                    "DAY_OF_MONTH exceeds month length in given year: "
                    + toString(year, month, dayOfMonth));
            }
    
        }
    
        /**
         * <p>Returns the day of week for given gregorian date. </p>
         *
         * <p>This method is based on ISO-8601 and assumes that Monday is the
         * first day of week. </p>
         *
         * @param   year        proleptic iso year
         * @param   month       gregorian month (1-12)
         * @param   dayOfMonth  day of month (1-31)
         * @return  day of week (monday = 1, ..., sunday = 7)
         * @throws  IllegalArgumentException if the month or the day are
         *          out of range
         */
        /*[deutsch]
         * <p>Liefert den Tag des Woche f&uuml;r das angegebene Datum. </p>
         *
         * <p>Diese Methode setzt gem&auml;&szlig; dem ISO-8601-Standard den
         * Montag als ersten Tag der Woche voraus. </p>
         *
         * @param   year        proleptic iso year
         * @param   month       gregorian month (1-12)
         * @param   dayOfMonth  day of month (1-31)
         * @return  day of week (monday = 1, ..., sunday = 7)
         * @throws  IllegalArgumentException if the month or the day are
         *          out of range
         */
        public static int getDayOfWeek(
            int year,
            int month,
            int dayOfMonth
        ) {
    
            if ((dayOfMonth < 1) || (dayOfMonth > 31)) {
                throw new IllegalArgumentException(
                    "Day out of range: " + dayOfMonth);
            } else if (dayOfMonth > getLengthOfMonth(year, month)) {
                throw new IllegalArgumentException(
                    "Day exceeds month length: "
                    + toString(year, month, dayOfMonth));
            }
    
            int m = gaussianWeekTerm(month);
            int y = (year % 100);
            int c = Math.floorDiv(year, 100);
    
            if (y < 0) {
                y += 100;
            }
    
            if (month <= 2) { // Januar oder Februar
                y--;
                if (y < 0) {
                    y = 99;
                    c--;
                }
            }
    
            // Gauß'sche Wochentagsformel
            int k = Math.floorDiv(c, 4);
            int w = ((dayOfMonth + m + y + (y / 4) + k - 2 * c) % 7);
    
            if (w <= 0) {
                w += 7;
            }
    
            return w;
    
        }
    
        // liefert eine ISO-konforme Standard-Darstellung eines Datums
        private static String toString(int year, int month, int dom) {
    
            StringBuilder calendar = new StringBuilder();
            calendar.append(year);
            calendar.append('-');
            if (month < 10) {
                calendar.append('0');
            }
            calendar.append(month);
            calendar.append('-');
            if (dom < 10) {
                calendar.append('0');
            }
            calendar.append(dom);
            return calendar.toString();
    
        }
    
        // entspricht dem Ausdruck [2.6 * m - 0.2] in der Gauß-Formel
        // corresponds to the expression [2.6 * m - 0.2] in the Gauss formula
        private static int gaussianWeekTerm(int month) {

            //   turn switch-case to if-else
            if (month == 1) {return 28;}
            else if (month == 2) {return 31;}
            else if (month == 3) {return 2;}
            else if (month == 4) {return 5;}
            else if (month == 5) {return 7;}
            else if (month == 6) {return 10;}
            else if (month == 7) {return 12;}
            else if (month == 8) {return 15;}
            else if (month == 9) {return 18;}
            else if (month == 10) {return 20;}
            else if (month == 11) {return 23;}
            else if (month == 12) {return 25;}
            else {
                throw new IllegalArgumentException("Month out of range: " + month);
                }
    
            // switch (month) {
            //     case 1:
            //         return 28;
            //     case 2:
            //         return 31;
            //     case 3:
            //         return 2;
            //     case 4:
            //         return 5;
            //     case 5:
            //         return 7;
            //     case 6:
            //         return 10;
            //     case 7:
            //         return 12;
            //     case 8:
            //         return 15;
            //     case 9:
            //         return 18;
            //     case 10:
            //         return 20;
            //     case 11:
            //         return 23;
            //     case 12:
            //         return 25;
            //     default:
            //         throw new IllegalArgumentException(
            //             "Month out of range: " + month);
            // }
    
        }
    }

}
