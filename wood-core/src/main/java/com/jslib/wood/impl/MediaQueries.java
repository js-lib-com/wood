package com.jslib.wood.impl;

import com.jslib.wood.Project;
import com.jslib.wood.WoodException;
import com.jslib.wood.util.StringsUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MediaQueries {
    private final Project project;
    private final List<MediaQueryDefinition> queries = new ArrayList<>();

    public MediaQueries(Project project) {
        this.project = project;
    }

    /**
     * Add media query definition, identified by its alias, from project to {@link #queries} list. If alias is not
     * defined returns false.
     *
     * @param alias alias identifying the media query definition.
     * @return true if requested alias is defined in project media query definitions or false if alias is not defined.
     */
    public boolean add(String alias) {
        assert alias != null && !alias.isEmpty() : "Alias argument is null or empty";
        MediaQueryDefinition query = project.getMediaQueryDefinition(alias);
        if (query == null) {
            return false;
        }
        if (queries.contains(query)) {
            throw new WoodException("Media query definition override for alias %s", alias);
        }
        queries.add(query);
        return true;
    }

    public boolean isEmpty() {
        return queries.isEmpty();
    }

    public String getMedia() {
        Set<String> medias = queries.stream().map(MediaQueryDefinition::getMedia).collect(Collectors.toSet());
        return medias.size() == 1 ? medias.iterator().next() : "all";
    }

    /**
     * Returns the concatenated expression string for all query media definitions.
     *
     * @return the concatenated expression string; returns an empty string if no expressions are found (never {@code null})
     */
    public String getExpression() {
        List<String> expressions = queries.stream().filter(query -> query.getExpression() != null).map(query -> StringsUtil.concat("( ", query.getExpression(), " )")).collect(Collectors.toList());
        return StringsUtil.join(expressions, " and ");
    }

    public List<MediaQueryDefinition> getQueries() {
        return queries;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + queries.hashCode();
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
        MediaQueries other = (MediaQueries) obj;
        return queries.equals(other.queries);
    }
}
