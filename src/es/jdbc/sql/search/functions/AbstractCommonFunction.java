/*
 * Copyright (C) 2018 The elasticsearch-jdbc-wrapper Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
/*
 *                  ___====-_  _-====___
 *            _--^^^#####//      \\#####^^^--_
 *         _-^##########// (    ) \\##########^-_
 *        -############//  |\^^/|  \\############-
 *      _/############//   (@::@)   \\############\_
 *     /#############((     \\//     ))#############\
 *    -###############\\    (oo)    //###############-
 *   -#################\\  / VV \  //#################-
 *  -###################\\/      \//###################-
 * _#/|##########/\######(   /\   )######/\##########|\#_
 * |/ |#/\#/\#/\/  \#/\##\  |  |  /##/\#/  \/\#/\#/\#| \|
 * `  |/  V  V  `   V  \#\| |  | |/#/  V   '  V  V  \|  '
 *    `   `  `      `   / | |  | | \   '      '  '   '
 *                     (  | |  | |  )
 *                    __\ | |  | | /__
 *                   (vvv(VVV)(VVV)vvv)
 *  Code is far away from bug with the dragon's protection.
 */
package es.jdbc.sql.search.functions;

import java.util.List;
import java.util.Map;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregation;

import es.jdbc.sql.search.ISelectItem;
import es.jdbc.sql.search.SelectItemFactory;

public abstract class AbstractCommonFunction implements IFunction {
    /**
     * 関数名称.
     */
    protected String name;
    /**
     * タイプ名称.
     */
    protected String type;
    /**
     * パラメーターリスト.
     */
    protected List<ISelectItem> parameters;
    /**
     * distinct.
     */
    protected boolean isDistinct;
    /**
     * isNestedExists.
     */
    protected boolean isNestedExists;
    /**
     * isTotalAggregation.
     */
    protected boolean isTotalAggregation;
    /**
     * isScoreQuery.
     */
    protected boolean isScoreQuery;
    /**
     * isSqlNodeUsed.
     */
    protected boolean isSqlNodeUsed;
    /**
     * サブQueryコンテナー.
     */
    protected BoolQueryBuilder subQueryContainer;
    /**
     * BaseAggregation.
     */
    protected AbstractAggregationBuilder<?> baseAggregation;
    
    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.ISelectItem#setName(java.lang.String)
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.ISelectItem#getName()
     */
    @Override
    public String getName() {
        return StringUtils.isBlank(this.name)?Integer.toString(this.hashCode()):this.name;
    }
    
    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.ISelectItem#setType(java.lang.String)
     */
    @Override
    public void setType(String type) {
        this.type = type;
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.ISelectItem#getType()
     */
    @Override
    public String getType() {
        return type;
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.ISelectItem#getFunctionParameters()
     */
    @Override
    public List<ISelectItem> getParameters() {
        return this.parameters;
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.ISelectItem#addFunctionParameter(jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.ISelectItem)
     */
    @Override
    public void addParameter(ISelectItem param) {
        this.parameters.add(param);
        this.isTotalAggregation |= (param instanceof OverPartition);
    }
    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.IFunction#addFunctionParameter(net.sf.jsqlparser.expression.Expression)
     */
    @Override
    public void addParameter(Expression expression, List<Object> jdbcValues) {
        ISelectItem parameter = SelectItemFactory.manufacture(expression, jdbcValues);
        if (parameter != null) {
            this.addParameter(parameter);
        }
    }
    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.ISelectItem#addFunctionParameter(jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.ISelectItem)
     */
    @Override
    public void setParameters(ExpressionList expressions, List<Object> jdbcValues) {
        if (expressions != null && expressions.getExpressions() != null && expressions.getExpressions().size() > 0) {
            List<Expression> expressionList = expressions.getExpressions();
            for (Expression expression : expressionList) {
                this.addParameter(expression, jdbcValues);
            }
        }
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#getParameterList()
     */
    @Override
    public List<ISelectItem> getParameterList() {
        return this.parameters;
    }
    
    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.ISelectItem#isFunction()
     */
    @Override
    public boolean isFunction() {
        return true;
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.ISelectItem#isDistinct()
     */
    @Override
    public boolean isDistinct() {
        return this.isDistinct;
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.ISelectItem#setDistinct(boolean)
     */
    @Override
    public void setDistinct(boolean isDistinct) {
        this.isDistinct = isDistinct;
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#fillResult(java.util.List, java.util.List)
     */
    @Override
    public void fillResult(final List<Map<String, Object>> aggsContainer, final List<Aggregation> aggsList) {
        //各自実装
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#setNestedExists(boolean)
     */
    @Override
    public void setNestedExists(boolean isNestedExists) {
        this.isNestedExists = isNestedExists;
    }
    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#getNestedExists()
     */
    @Override
    public boolean isNestedParamExists() {
        boolean retValue = false;
        if (this.parameters != null) {
            for (ISelectItem param : this.parameters) {
                if (param instanceof Nested) {
                    retValue = true;
                    break;
                }
            }
        }
        return retValue;
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#buildQuery()
     */
    @Override
    public QueryBuilder buildQuery() {
        return null;
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#buildAggregation()
     */
    @Override
    public AbstractAggregationBuilder<?> buildAggregation() {
        return null;
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#isTotalAggregation()
     */
    @Override
    public boolean isTotalAggregation() {
        return this.isTotalAggregation;
    }
    
    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#findBaseAggregation()
     */
    @Override
    public AbstractAggregationBuilder<?> findBaseAggregation() {
        return this.baseAggregation;
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#subQueryContainer()
     */
    @Override
    public BoolQueryBuilder subQueryContainer() {
        return this.subQueryContainer;
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#isScoreQuery()
     */
    @Override
    public boolean isScoreQuery() {
        return this.isScoreQuery;
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#isSqlNodeUsed()
     */
    @Override
    public boolean isSqlNodeUsed() {
        return this.isSqlNodeUsed;
    }
}
