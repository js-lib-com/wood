package com.jslib.wood.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.jslib.wood.util.StringsUtil.format;

/**
 * Handy methods, mostly reflexive, related to class and class loader.
 *
 * @author Iulian Rotaru
 */
public class ClassesUtil {
    protected ClassesUtil() {
    }

    /**
     * Prefixes used for getter methods. See {@link #getGetter(Class, String)}.
     */
    private static final String[] GETTERS_PREFIX = new String[]{"get", "is"};

    /**
     * Get class getter method for requested field.
     * <p>
     * By convention a method getter for boolean values may have <code>is</code> prefix. Unfortunately this convention is
     * not constrained and there are exception, i.e. use <code>get</code> prefix for booleans. To cope with case this
     * method tries both prefixes and throws exception only if none found.
     * <p>
     * Since getter never has parameters this method does not provide a <code>parameterTypes</code> argument. Also note
     * that parameter <code>fieldName</code> is what its name says: the name of the field not the method name.
     * <p>
     * For convenience field name argument supports dashed case, that is, can contain dash separator. For example
     * <code>phone-number</code> is considered a valid field name and searched method names are
     * <code>getPhoneNumber</code> and <code>isPhoneNumber</code>.
     *
     * @param clazz     class to return getter from,
     * @param fieldName field name.
     * @return getter method.
     * @throws NoSuchMethodException if there is no getter method for requested field.
     */
    public static Method getGetter(Class<?> clazz, String fieldName) throws NoSuchMethodException {
        for (String prefix : GETTERS_PREFIX) {
            try {
                Method method = clazz.getDeclaredMethod(StringsUtil.getMethodAccessor(prefix, fieldName));
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            } catch (SecurityException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new NoSuchMethodException(format("No getter for %s#%s", clazz.getCanonicalName(), fieldName));
    }

    /**
     * Convenient method to retrieve named resource as reader. It is caller responsibility to ensure resources exists;
     * if resource is missing this method throws IO exception.
     *
     * @param name resource name.
     * @return reader for named resource.
     * @throws IOException if resource not found.
     */
    public static Reader getResourceAsReader(String name) throws IOException {
        return new InputStreamReader(getResourceAsStream(name), StandardCharsets.UTF_8);
    }

    private static InputStream getResourceAsStream(String name) throws IOException {
        assert name != null && !name.isEmpty() : "Resource name argument is null or empty";
        // not documented behavior: accept but ignore trailing path separator
        if (name.charAt(0) == '/') {
            name = name.substring(1);
        }

        InputStream stream = getResourceAsStream(name, new ClassLoader[]
                {
                        Thread.currentThread().getContextClassLoader(), ClassesUtil.class.getClassLoader(), ClassLoader.getSystemClassLoader()
                });
        if (stream == null) {
            throw new IOException(format("Resource %s not found", name));
        }
        return stream;
    }

    private static InputStream getResourceAsStream(String name, ClassLoader[] classLoaders) {
        // Java standard class loader require resource name to be an absolute path without leading path separator
        // at this point <name> argument is guaranteed to not start with leading path separator

        for (ClassLoader classLoader : classLoaders) {
            InputStream stream = classLoader.getResourceAsStream(name);
            if (stream == null) {
                // it seems there are class loaders that require leading path separator
                // not confirmed rumor but found in similar libraries
                stream = classLoader.getResourceAsStream('/' + name);
            }
            if (stream != null) {
                return stream;
            }
        }
        return null;
    }

    /**
     * Determine if a given type is a kind of one of the requested types to match. Traverses <code>typesToMatch</code> and
     * delegates {@link #isKindOf(Type, Type)} till first positive match and returns true. If no match found returns
     * false. If <code>type</code> is null returns false. If a type to match happened to be null is considered no match.
     *
     * @param t            type to test, possible null,
     * @param typesToMatch variable number of types to match.
     * @return true if <code>type</code> is a kind of one of <code>typesToMatch</code>.
     * @throws IllegalArgumentException if <code>typesToMach</code> is null or empty.
     */
    public static boolean isKindOf(Type t, Type... typesToMatch) throws IllegalArgumentException {
        assert typesToMatch != null && typesToMatch.length > 0 : "Types to match argument is null or empty";
        for (Type typeToMatch : typesToMatch) {
            if (isKindOf(t, typeToMatch)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if a given type is a kind of requested type to match. Returns true if <code>type</code> is a subclass
     * or implements <code>typeToMatch</code> - not necessarily direct. Boxing classes for primitive values are
     * compatible. This departs from {@link Class#isAssignableFrom(Class)} that consider primitive and related boxing class
     * as different.
     * <p>
     * If either type or type to match are parameterized types uses the raw class. If either type or type to match are
     * null returns false.
     *
     * @param t           type to test,
     * @param typeToMatch desired type to match.
     * @return true if <code>type</code> is subclass of or implements <code>typeToMatch</code>.
     */
    private static boolean isKindOf(Type t, Type typeToMatch) {
        if (t == null || typeToMatch == null) {
            return false;
        }
        if (t.equals(typeToMatch)) {
            return true;
        }

        Class<?> clazz = typeToClass(t);
        if (clazz == null) {
            return false;
        }
        Class<?> classToMatch = typeToClass(typeToMatch);
        if (classToMatch == null) {
            return false;
        }

        if (clazz.isPrimitive()) {
            return BOXING_MAP.get(clazz) == classToMatch;
        }
        if (classToMatch.isPrimitive()) {
            return BOXING_MAP.get(classToMatch) == clazz;
        }

        return classToMatch.isAssignableFrom(clazz);
    }

    /**
     * Java language primitive values boxing classes.
     */
    private static final Map<Type, Type> BOXING_MAP = new HashMap<>();

    static {
        BOXING_MAP.put(boolean.class, Boolean.class);
        BOXING_MAP.put(byte.class, Byte.class);
        BOXING_MAP.put(char.class, Character.class);
        BOXING_MAP.put(short.class, Short.class);
        BOXING_MAP.put(int.class, Integer.class);
        BOXING_MAP.put(long.class, Long.class);
        BOXING_MAP.put(float.class, Float.class);
        BOXING_MAP.put(double.class, Double.class);
    }

    /**
     * Cast Java reflective type to language class. If <code>type</code> is instance of {@link Class} just return it. If
     * is parameterized type returns the raw class.
     *
     * @param t Java reflective type.
     * @return the class described by given <code>type</code>.
     */
    private static Class<?> typeToClass(Type t) {
        if (t instanceof Class<?>) {
            return (Class<?>) t;
        }
        if (t instanceof TypeVariable) {
            return null;
            // t = ((TypeVariable)t).getBounds()[0];
        }
        if (t instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) t).getRawType();
        }
        throw new IllegalStateException(format("Unknown type %s to convert to class", t));
    }
}
