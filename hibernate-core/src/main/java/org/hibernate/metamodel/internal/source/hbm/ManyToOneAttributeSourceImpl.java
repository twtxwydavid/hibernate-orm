/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.hbm;

import java.util.List;

import org.hibernate.FetchMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.jaxb.spi.hbm.JaxbColumnElement;
import org.hibernate.jaxb.spi.hbm.JaxbManyToOneElement;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.ToOneAttributeSource;

/**
 * Implementation for {@code <many-to-one/>} mappings
 *
 * @author Steve Ebersole
 */
class ManyToOneAttributeSourceImpl extends AbstractHbmSourceNode implements ToOneAttributeSource {
	private final JaxbManyToOneElement manyToOneElement;
	private final SingularAttributeBinding.NaturalIdMutability naturalIdMutability;
	private final List<RelationalValueSource> valueSources;

	ManyToOneAttributeSourceImpl(
			MappingDocument sourceMappingDocument,
			final JaxbManyToOneElement manyToOneElement,
			final String logicalTableName,
			SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		super( sourceMappingDocument );
		this.manyToOneElement = manyToOneElement;
		this.naturalIdMutability = naturalIdMutability;
		this.valueSources = Helper.buildValueSources(
				sourceMappingDocument(),
				new Helper.ValueSourcesAdapter() {
					@Override
					public String getColumnAttribute() {
						return manyToOneElement.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return manyToOneElement.getFormulaAttribute();
					}

					@Override
					public List<JaxbColumnElement> getColumn() {
						return manyToOneElement.getColumn();
					}

					@Override
					public List<String> getFormula() {
						return manyToOneElement.getFormula();
					}

					@Override
					public String getContainingTableName() {
						return logicalTableName;
					}

					@Override
					public boolean isIncludedInInsertByDefault() {
						return manyToOneElement.isInsert();
					}

					@Override
					public boolean isIncludedInUpdateByDefault() {
						return manyToOneElement.isUpdate();
					}
				}
		);
	}

	@Override
	public String getName() {
			return manyToOneElement.getName();
	}

	@Override
	public ExplicitHibernateTypeSource getTypeInformation() {
		return Helper.TO_ONE_ATTRIBUTE_TYPE_SOURCE;
	}

	@Override
	public String getPropertyAccessorName() {
		return manyToOneElement.getAccess();
	}

	@Override
	public PropertyGeneration getGeneration() {
		return PropertyGeneration.NEVER;
	}

	@Override
	public boolean isLazy() {
		return getFetchTiming() != FetchTiming.IMMEDIATE;
	}

	@Override
	public SingularAttributeBinding.NaturalIdMutability getNaturalIdMutability() {
		return naturalIdMutability;
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return manyToOneElement.isOptimisticLock();
	}

	@Override
	public Iterable<CascadeStyle> getCascadeStyles() {
		return Helper.interpretCascadeStyles( manyToOneElement.getCascade(), bindingContext() );
	}

	@Override
	public FetchTiming getFetchTiming() {
		final String fetchSelection = manyToOneElement.getFetch() != null
				? manyToOneElement.getFetch().value()
				: null;
		final String lazySelection = manyToOneElement.getLazy() != null
				? manyToOneElement.getLazy().value()
				: null;
		final String outerJoinSelection = manyToOneElement.getOuterJoin() != null
				? manyToOneElement.getOuterJoin().value()
				: null;

		if ( lazySelection == null ) {
			if ( "join".equals( fetchSelection ) || "true".equals( outerJoinSelection ) ) {
				return FetchTiming.IMMEDIATE;
			}
			else if ( "false".equals( outerJoinSelection ) ) {
				return FetchTiming.DELAYED;
			}
			else {
				return bindingContext().getMappingDefaults().areAssociationsLazy()
						? FetchTiming.DELAYED
						: FetchTiming.IMMEDIATE;
			}
		}
		else  if ( "extra".equals( lazySelection ) ) {
			return FetchTiming.EXTRA_DELAYED;
		}
		else if ( "true".equals( lazySelection ) || "proxy".equals( lazySelection ) ) {
			return FetchTiming.DELAYED;
		}
		else if ( "false".equals( lazySelection ) ) {
			return FetchTiming.IMMEDIATE;
		}

		throw new MappingException(
				String.format(
						"Unexpected lazy selection [%s] on '%s'",
						lazySelection,
						manyToOneElement.getName()
				),
				origin()
		);
	}

	@Override
	public FetchStyle getFetchStyle() {
		// todo : handle batch fetches?

		final String fetchSelection = manyToOneElement.getFetch() != null
				? manyToOneElement.getFetch().value()
				: null;
		final String outerJoinSelection = manyToOneElement.getOuterJoin() != null
				? manyToOneElement.getOuterJoin().value()
				: null;

		if ( fetchSelection == null ) {
			if ( outerJoinSelection == null ) {
				return FetchStyle.SELECT;
			}
			else {
				if ( "auto".equals( outerJoinSelection ) ) {
					return bindingContext().getMappingDefaults().areAssociationsLazy()
							? FetchStyle.SELECT
							: FetchStyle.JOIN;
				}
				else {
					return "true".equals( outerJoinSelection ) ? FetchStyle.JOIN : FetchStyle.SELECT;
				}
			}
		}
		else {
			return "join".equals( fetchSelection ) ? FetchStyle.JOIN : FetchStyle.SELECT;
		}
	}

	@Override
	public FetchMode getFetchMode() {
		return manyToOneElement.getFetch() == null
				? FetchMode.DEFAULT
				: FetchMode.valueOf( manyToOneElement.getFetch().value() );
	}

	@Override
	public Nature getNature() {
		return Nature.MANY_TO_ONE;
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return manyToOneElement.isInsert();
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return manyToOneElement.isUpdate();
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return ! Helper.getValue( manyToOneElement.isNotNull(), false );
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return valueSources;
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public Iterable<? extends MetaAttributeSource> getMetaAttributeSources() {
		return manyToOneElement.getMeta();
	}

	@Override
	public String getReferencedEntityName() {
		return manyToOneElement.getClazz() != null
				? manyToOneElement.getClazz()
				: manyToOneElement.getEntityName();
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		return manyToOneElement.getPropertyRef() == null
				? null
				: new JoinColumnResolutionDelegateImpl();
	}

	@Override
	public String getExplicitForeignKeyName() {
		return manyToOneElement.getForeignKey();
	}

	public class JoinColumnResolutionDelegateImpl implements JoinColumnResolutionDelegate {
		@Override
		public String getReferencedAttributeName() {
			return manyToOneElement.getPropertyRef();
		}

		@Override
		public List<Value> getJoinColumns(JoinColumnResolutionContext context) {
			return context.resolveRelationalValuesForAttribute( manyToOneElement.getPropertyRef() );
		}
	}
}