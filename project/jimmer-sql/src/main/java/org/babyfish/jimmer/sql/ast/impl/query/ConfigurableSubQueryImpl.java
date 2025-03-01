package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.table.TableTypeProvider;
import org.babyfish.jimmer.sql.ast.query.ConfigurableSubQuery;
import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ConfigurableSubQueryImpl<R>
        extends AbstractConfigurableTypedQueryImpl
        implements ConfigurableSubQuery<R>, ExpressionImplementor<R> {

    private final Class<R> type;

    @SuppressWarnings("unchecked")
    ConfigurableSubQueryImpl(
            TypedQueryData data,
            MutableSubQueryImpl baseQuery
    ) {
        super(data, baseQuery);
        List<Selection<?>> selections = data.selections;
        switch (selections.size()) {
            case 1:
                Selection<?> selection = selections.get(0);
                if (selection instanceof TableTypeProvider) {
                    type = (Class<R>) ((TableTypeProvider)selection).getImmutableType().getJavaClass();
                } else {
                    type = (Class<R>)((ExpressionImplementor<?>)selection).getType();
                }
                break;
            case 2:
                type = (Class<R>) Tuple2.class;
                break;
            case 3:
                type = (Class<R>) Tuple3.class;
                break;
            case 4:
                type = (Class<R>) Tuple4.class;
                break;
            case 5:
                type = (Class<R>) Tuple5.class;
                break;
            case 6:
                type = (Class<R>) Tuple6.class;
                break;
            case 7:
                type = (Class<R>) Tuple7.class;
                break;
            case 8:
                type = (Class<R>) Tuple8.class;
                break;
            case 9:
                type = (Class<R>) Tuple9.class;
                break;
            default:
                throw new IllegalArgumentException("selection count must between 1 and 9");
        }
    }

    @SuppressWarnings("unchecked")
    static <R> TypedSubQuery<R> of(
            TypedQueryData data,
            MutableSubQueryImpl baseQuery
    ) {
        if (data.selections.size() == 1) {
            Selection<?> selection = data.selections.get(0);
            if (selection instanceof ExpressionImplementor<?>) {
                Class<?> type = ((ExpressionImplementor<?>) selection).getType();
                if (type == String.class) {
                    return (TypedSubQuery<R>) new Str(data, baseQuery);
                }
                if (Number.class.isAssignableFrom(type)) {
                    return (TypedSubQuery<R>) new Num<>(data, baseQuery);
                }
                if (Comparable.class.isAssignableFrom(type)) {
                    return (TypedSubQuery<R>) new Cmp<>(data, baseQuery);
                }
            }
        }
        return new ConfigurableSubQueryImpl<>(data, baseQuery);
    }

    @Override
    public Class<R> getType() {
        return type;
    }

    @Override
    public MutableSubQueryImpl getBaseQuery() {
        return (MutableSubQueryImpl) super.getBaseQuery();
    }

    @Override
    public ConfigurableSubQuery<R> limit(int limit) {
        return limitImpl(limit, null);
    }

    @Override
    public ConfigurableSubQuery<R> offset(long offset) {
        return limitImpl(null, offset);
    }

    @Override
    public ConfigurableSubQuery<R> limit(int limit, long offset) {
        return limitImpl(limit, offset);
    }

    private ConfigurableSubQuery<R> limitImpl(@Nullable Integer limit, @Nullable Long offset) {
        TypedQueryData data = getData();
        if (limit == null) {
            limit = data.limit;
        }
        if (offset == null) {
            offset = data.offset;
        }
        if (data.limit == limit && data.offset == offset) {
            return this;
        }
        if (limit < 0) {
            throw new IllegalArgumentException("'limit' can not be less than 0");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("'offset' can not be less than 0");
        }
        return new ConfigurableSubQueryImpl<>(
                data.limit(limit, offset),
                getBaseQuery()
        );
    }

    @Override
    public ConfigurableSubQuery<R> distinct() {
        TypedQueryData data = getData();
        if (data.distinct) {
            return this;
        }
        return new ConfigurableSubQueryImpl<>(
                data.distinct(),
                getBaseQuery()
        );
    }

    @Override
    public ConfigurableSubQuery<R> hint(@Nullable String hint) {
        TypedQueryData data = getData();
        return new ConfigurableSubQueryImpl<>(
                data.hint(hint),
                getBaseQuery()
        );
    }

    @Override
    public Expression<R> all() {
        return new SubQueryFunctionExpression.All<>(this);
    }

    @Override
    public Expression<R> any() {
        return new SubQueryFunctionExpression.Any<>(this);
    }

    @Override
    public Predicate exists() {
        return ExistsPredicate.of(this, false);
    }

    @Override
    public Predicate notExists() {
        return ExistsPredicate.of(this, true);
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        getBaseQuery().setParent(visitor.getAstContext().getStatement());
        if (visitor.visitSubQuery(this)) {
            super.accept(visitor);
        }
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        builder.enter(SqlBuilder.ScopeType.SUB_QUERY);
        super.renderTo(builder);
        builder.leave();
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public boolean hasVirtualPredicate() {
        return getBaseQuery().hasVirtualPredicate();
    }

    @Override
    public Ast resolveVirtualPredicate(AstContext ctx) {
        getBaseQuery().resolveVirtualPredicate(ctx);
        return this;
    }

    @Override
    public TypedSubQuery<R> union(TypedSubQuery<R> other) {
        return MergedTypedSubQueryImpl.of(getBaseQuery().getSqlClient(), "union", this, other);
    }

    @Override
    public TypedSubQuery<R> unionAll(TypedSubQuery<R> other) {
        return MergedTypedSubQueryImpl.of(getBaseQuery().getSqlClient(), "union all", this, other);
    }

    @Override
    public TypedSubQuery<R> minus(TypedSubQuery<R> other) {
        return MergedTypedSubQueryImpl.of(getBaseQuery().getSqlClient(), "minus", this, other);
    }

    @Override
    public TypedSubQuery<R> intersect(TypedSubQuery<R> other) {
        return MergedTypedSubQueryImpl.of(getBaseQuery().getSqlClient(), "intersect", this, other);
    }

    private static class Str extends ConfigurableSubQueryImpl<String> implements ConfigurableSubQuery.Str, StringExpressionImplementor {

        Str(TypedQueryData data, MutableSubQueryImpl baseQuery) {
            super(data, baseQuery);
        }

        @Override
        public TypedSubQuery.Str union(TypedSubQuery<String> other) {
            return (TypedSubQuery.Str) super.union(other);
        }

        @Override
        public TypedSubQuery.Str unionAll(TypedSubQuery<String> other) {
            return (TypedSubQuery.Str) super.unionAll(other);
        }

        @Override
        public TypedSubQuery.Str minus(TypedSubQuery<String> other) {
            return (TypedSubQuery.Str) super.minus(other);
        }

        @Override
        public TypedSubQuery.Str intersect(TypedSubQuery<String> other) {
            return (TypedSubQuery.Str) super.intersect(other);
        }
    }

    private static class Num<N extends Number & Comparable<N>> extends ConfigurableSubQueryImpl<N> implements ConfigurableSubQuery.Num<N>, NumericExpressionImplementor<N> {

        Num(TypedQueryData data, MutableSubQueryImpl baseQuery) {
            super(data, baseQuery);
        }

        @Override
        public TypedSubQuery.Num<N> union(TypedSubQuery<N> other) {
            return (TypedSubQuery.Num<N>) super.union(other);
        }

        @Override
        public TypedSubQuery.Num<N> unionAll(TypedSubQuery<N> other) {
            return (TypedSubQuery.Num<N>) super.unionAll(other);
        }

        @Override
        public TypedSubQuery.Num<N> minus(TypedSubQuery<N> other) {
            return (TypedSubQuery.Num<N>) super.minus(other);
        }

        @Override
        public TypedSubQuery.Num<N> intersect(TypedSubQuery<N> other) {
            return (TypedSubQuery.Num<N>) super.intersect(other);
        }
    }

    private static class Cmp<T extends Comparable<?>> extends ConfigurableSubQueryImpl<T> implements ConfigurableSubQuery.Cmp<T>, ComparableExpressionImplementor<T> {

        Cmp(TypedQueryData data, MutableSubQueryImpl baseQuery) {
            super(data, baseQuery);
        }

        @Override
        public TypedSubQuery.Cmp<T> union(TypedSubQuery<T> other) {
            return (TypedSubQuery.Cmp<T>) super.union(other);
        }

        @Override
        public TypedSubQuery.Cmp<T> unionAll(TypedSubQuery<T> other) {
            return (TypedSubQuery.Cmp<T>) super.unionAll(other);
        }

        @Override
        public TypedSubQuery.Cmp<T> minus(TypedSubQuery<T> other) {
            return (TypedSubQuery.Cmp<T>) super.minus(other);
        }

        @Override
        public TypedSubQuery.Cmp<T> intersect(TypedSubQuery<T> other) {
            return (TypedSubQuery.Cmp<T>) super.intersect(other);
        }
    }
}