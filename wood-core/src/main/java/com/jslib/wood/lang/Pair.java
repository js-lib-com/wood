package com.jslib.wood.lang;

/**
 * Immutable pair of string values.
 *
 * @author Iulian Rotaru
 */
public final class Pair {
    /**
     * First value of this pair.
     */
    private final String first;

    /**
     * The second value of this pair.
     */
    private final String second;

    /**
     * Construct a pair instance
     *
     * @param first  first pair value,
     * @param second pair value.
     */
    public Pair(String first, String second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Get first value of this pair.
     *
     * @return this pair first value.
     */
    public String first() {
        return first;
    }

    /**
     * Get the second value of this pair.
     *
     * @return this pair second value.
     */
    public String second() {
        return second;
    }

    /**
     * Semantic alias for {@link Pair#first()}, when pair is used in a map context.
     *
     * @return this pair first value considered a key.
     */
    public String key() {
        return first;
    }

    /**
     * Semantic alias for {@link Pair#second()}, when pair is used in a map context.
     *
     * @return this pair second value.
     */
    public String value() {
        return second;
    }

    @Override
    public String toString() {
        return first + ":" + second;
    }

    /**
     * Cache hash code for this immutable instance.
     */
    private int hash;

    /**
     * Get instance hash code.
     */
    @Override
    public int hashCode() {
        if (hash == 0) {
            final int prime = 31;
            hash = 1;
            hash = prime * hash + ((first == null) ? 0 : first.hashCode());
            hash = prime * hash + ((second == null) ? 0 : second.hashCode());
        }
        return hash;
    }

    /**
     * Equality test operator.
     *
     * @param obj instance to compare with.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Pair other = (Pair) obj;
        if (first == null) {
            if (other.first != null) return false;
        } else if (!first.equals(other.first)) return false;
        if (second == null) {
            return other.second == null;
        } else return second.equals(other.second);
    }
}