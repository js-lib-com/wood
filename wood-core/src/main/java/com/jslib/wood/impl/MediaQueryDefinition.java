package com.jslib.wood.impl;

/**
 * Media query definition from project descriptor. The definition for a media query has an alias used to represent the media
 * query as file path variant. It also has media query expression that is used literally on style files, {@literal}media rule,
 * and a weight that determine media sections order.
 * <p>
 * Alias is also used as unique key for media query definition. Two instances are considered equal if you have the same alias.
 * <p>
 * Media query definition has no mutable state and is thread safe.
 *
 * @author Iulian Rotaru
 */
public class MediaQueryDefinition {
    private final String alias;
    private final String media;
    /**
     * Expression can miss from media query definition in which case is null.
     */
    private final String expression;

    public MediaQueryDefinition(String alias, String media, String expression) {
        assert alias != null && !alias.isEmpty() : "Alias argument is null or empty";
        assert media != null && !media.isEmpty() : "Media argument is null or empty";

        this.alias = alias;
        this.media = media;
        this.expression = expression;
    }

    public String getAlias() {
        return alias;
    }

    public String getMedia() {
        return media;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((alias == null) ? 0 : alias.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MediaQueryDefinition other = (MediaQueryDefinition) obj;
        if (alias == null) {
            return other.alias == null;
        } else return alias.equals(other.alias);
    }
}
