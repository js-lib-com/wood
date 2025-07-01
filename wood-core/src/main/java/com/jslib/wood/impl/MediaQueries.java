package com.jslib.wood.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.jslib.wood.Project;
import com.jslib.wood.WoodException;
import com.jslib.wood.util.StringsUtil;

public class MediaQueries implements Comparable<MediaQueries> {
	private final Project project;
	private final List<MediaQueryDefinition> queries = new ArrayList<>();

	public MediaQueries(Project project) {
		this.project = project;
	}

	public boolean add(String alias) {
		assert alias != null && !alias.isEmpty(): "Alias argument is null or empty";
		MediaQueryDefinition query = project.getMediaQueryDefinition(alias);
		if (query == null) {
			return false;
		}
		if(queries.contains(query)) {
			throw new WoodException("Media query defintion override for alias |%s|.", alias);
		}
		queries.add(query);
		return true;
	}

	public boolean isEmpty() {
		return queries.isEmpty();
	}

	public String getExpression() {
		List<String> expressions = queries.stream().map(query -> StringsUtil.concat("( ", query.getExpression(), " )")).collect(Collectors.toList());
		return StringsUtil.join(expressions, " and ");
	}

	public List<MediaQueryDefinition> getQueries() {
		return queries;
	}

	public Long getWeight() {
		return queries.stream().map(query -> (long) query.getWeight()).reduce(1L, (subtotal, weight) -> subtotal * weight);
	}

	@Override
	public int compareTo(MediaQueries other) {
		return this.getWeight().compareTo(other.getWeight());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((queries == null) ? 0 : queries.hashCode());
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
		if (queries == null) {
			if (other.queries != null)
				return false;
		} else if (!queries.equals(other.queries))
			return false;
		return true;
	}
}
