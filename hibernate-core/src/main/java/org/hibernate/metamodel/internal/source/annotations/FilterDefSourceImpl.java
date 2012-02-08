/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.internal.source.annotations;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.metamodel.spi.source.FilterDefSource;
import org.hibernate.metamodel.spi.source.FilterParameterSource;

/**
 * @author Steve Ebersole
 */
public class FilterDefSourceImpl implements FilterDefSource {
	private final String name;
	private final String condition;
	private List<FilterParameterSource> parameterSources;

	public FilterDefSourceImpl(AnnotationInstance filerDefAnnotation) {
		this.name = JandexHelper.getValue( filerDefAnnotation, "name", String.class );
		this.condition = JandexHelper.getValue( filerDefAnnotation, "defaultCondition", String.class );
		this.parameterSources = buildParameterSources( filerDefAnnotation );
	}

	private List<FilterParameterSource> buildParameterSources(AnnotationInstance filerDefAnnotation) {
		final List<FilterParameterSource> parameterSources = new ArrayList<FilterParameterSource>();
		for ( AnnotationInstance paramAnnotation : JandexHelper.getValue( filerDefAnnotation, "parameters", AnnotationInstance[].class ) ) {
			parameterSources.add( new FilterParameterSourceImpl( paramAnnotation ) );
		}
		return parameterSources;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getCondition() {
		return condition;
	}

	@Override
	public Iterable<FilterParameterSource> getParameterSources() {
		return parameterSources;
	}

	private static class FilterParameterSourceImpl implements FilterParameterSource {
		private final String name;
		private final String type;

		public FilterParameterSourceImpl(AnnotationInstance paramAnnotation) {
			this.name = JandexHelper.getValue( paramAnnotation, "name", String.class );
			this.type = JandexHelper.getValue( paramAnnotation, "type", String.class );
		}

		@Override
		public String getParameterName() {
			return name;
		}

		@Override
		public String getParameterValueTyeName() {
			return type;
		}
	}
}
