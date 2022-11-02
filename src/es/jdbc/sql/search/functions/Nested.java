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

import java.util.ArrayList;
import java.util.List;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import es.jdbc.sql.search.ISelectItem;
import es.jdbc.sql.search.sources.Source;
import es.jdbc.utils.CommonParams;

public class Nested extends AbstractCommonFunction {
    /**
     * コンストラクター.
     */
    public Nested() {
        this.parameters = new ArrayList<ISelectItem>();
    }
    
    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.IFunction#addFunctionParameter(net.sf.jsqlparser.expression.Expression)
     */
    @Override
    public void addParameter(Expression expression, List<Object> jdbcValues) {
        if (expression instanceof Column) {
            Column column = (Column) expression;
            Source source = new Source();
            source.setName(column.getColumnName());
            this.addParameter(source);
        } else if (expression instanceof StringValue) {
            StringValue value = (StringValue) expression;
            Source source = new Source();
            source.setName(value.getValue());
            this.addParameter(source);
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
                if (expression instanceof Column) {
                    Column column = (Column) expression;
                    Source source = new Source();
                    source.setName(column.getColumnName());
                    this.addParameter(source);
                } else if (expression instanceof StringValue) {
                    StringValue value = (StringValue) expression;
                    Source source = new Source();
                    source.setName(value.getValue());
                    this.addParameter(source);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#buildAggregation()
     */
    @Override
    public AbstractAggregationBuilder<?> buildAggregation() {
        /** NESTED("パス名称") */
        ISelectItem path = parameters.get(0);
        String nestedName = String.format(CommonParams.ELASTICSEARCH_AGGREGATION_KEYS.HASH_KEY_FORMAT.getKey(), 
                path.getName(), this.hashCode());
        return this.baseAggregation = AggregationBuilders.nested(nestedName, path.getName());
    }
    
    /* (non-Javadoc)
     * @see jp.co.peachjohn.crm.front.frontservice.model.sql.wrapper.elasticsearch.valueobject.selectitem.functions.IFunction#buildQuery()
     */
    @Override
    public QueryBuilder buildQuery() {
        /** NESTED("パス名称") */
        ISelectItem path = this.parameters.get(0);
        this.subQueryContainer = QueryBuilders.boolQuery();
        return QueryBuilders.nestedQuery(path.getName(), this.subQueryContainer, ScoreMode.None);
    }
}
