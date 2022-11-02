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
package es.jdbc.sql.analyzers;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.join.query.JoinQueryBuilders;

import es.jdbc.sql.SQLInfo;
import es.jdbc.sql.beans.AnalyzerPolicy;
import es.jdbc.sql.beans.JoinRelation;
import es.jdbc.sql.search.functions.IFunction;
import es.jdbc.utils.CommonParams;
import es.jdbc.utils.CommonUtils;

import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnalyticExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.ExtractExpression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.HexValue;
import net.sf.jsqlparser.expression.IntervalExpression;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.JsonExpression;
import net.sf.jsqlparser.expression.KeepExpression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.MySQLGroupConcat;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.NumericBind;
import net.sf.jsqlparser.expression.OracleHierarchicalExpression;
import net.sf.jsqlparser.expression.OracleHint;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.RowConstructor;
import net.sf.jsqlparser.expression.SignedExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeKeyExpression;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.WithinGroupExpression;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Modulo;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.expression.operators.relational.JsonOperator;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.Matches;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.expression.operators.relational.RegExpMatchOperator;
import net.sf.jsqlparser.expression.operators.relational.RegExpMySQLOperator;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SubSelect;

public class WhereAnalyzer implements ExpressionVisitor {
    /**
     * パラメーターリスト.
     */
    protected List<Object> paramList;
    /**
     * 分析中DSL文格納スタック.
     */
    protected Stack<BoolQueryBuilder> queryBuilderStack;
    /**
     * 点数検索DSL文リスト.
     */
    protected List<QueryBuilder> scoreQueryBuilderStack;
    /**
     * 現在処理中カラム名称リスト.
     */
    protected List<String> currentColumnNameList;
    /**
     * 現在処理中カラム値リスト.
     */
    protected List<Object> currentColumnValueList;
    /**
     * 現在処理式関係値リスト.
     */
    protected Stack<CommonParams.BOOL_QUERY_TYPE> currentBoolQueryType;
    /**
     * 検索対象テーブル名称.
     */
    protected String tableName;
    /**
     * analyzerPolicy.
     */
    protected AnalyzerPolicy analyzerPolicy;
    
    /**
     * コンストラクター.
     * @param paramList パラメーターリスト.
     */
    public WhereAnalyzer(final List<Object> paramList, final AnalyzerPolicy analyzerPolicy) {
        this.paramList = paramList;
        //分析中DSL文格納スタック
        this.queryBuilderStack = new Stack<BoolQueryBuilder>();
        this.queryBuilderStack.push(QueryBuilders.boolQuery());
        //現在処理式関係値格納スタック
        this.currentBoolQueryType = new Stack<CommonParams.BOOL_QUERY_TYPE>();
        this.currentBoolQueryType.push(CommonParams.BOOL_QUERY_TYPE.MUST);
        this.scoreQueryBuilderStack = new ArrayList<QueryBuilder>();
        this.analyzerPolicy = analyzerPolicy;
    }

    /**
     * DSLオブジェクト取得.
     * @return DSLオブジェクト
     */
    public BoolQueryBuilder getQueryBuilder() {
        BoolQueryBuilder queryBuilder = this.queryBuilderStack.peek();
        if (queryBuilder.hasClauses()) {
            return queryBuilder;
        } else {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.NullValue)
     */
    @Override
    public void visit(NullValue arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.Function)
     */
    @Override
    public void visit(Function arg0) {
        IFunction foundResult = IFunction.FUNCTION_LIST.findFunction(arg0);
        if (foundResult == null) {
            ExpressionList paramList = arg0.getParameters();
            List<Expression> expressionList = null;
            if (paramList != null && (expressionList = paramList.getExpressions()) != null) {
                for (Expression expression : expressionList) {
                    expression.accept(this);
                }
            }
        } else {
            foundResult.setParameters(arg0.getParameters(), this.paramList);
            QueryBuilder queryBuilder = foundResult.buildQuery();
            if (foundResult.isScoreQuery()) {
                this.scoreQueryBuilderStack.add(queryBuilder);
            } else {
                if (CommonParams.BOOL_QUERY_TYPE.MUST.equals(this.currentBoolQueryType.peek())) {
                    this.peekQueryBuilder().must(queryBuilder);
                } else if (CommonParams.BOOL_QUERY_TYPE.SHOULD.equals(this.currentBoolQueryType.peek())) {
                    this.peekQueryBuilder().should(queryBuilder);
                }
                BoolQueryBuilder subQueryContainer = foundResult.subQueryContainer();
                if (subQueryContainer != null) {
                    this.pushQueryBuilder(subQueryContainer);
                }
            }
        }
    }
    
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.schema.Column)
     */
    @Override
    public void visit(Column arg0) {
        String currentColumnName = arg0.getColumnName();
        if (StringUtils.startsWith(currentColumnName, "\"")
                && StringUtils.endsWith(currentColumnName, "\"")) {
            currentColumnName = StringUtils.removeStart(currentColumnName, "\"");
            currentColumnName = StringUtils.removeEnd(currentColumnName, "\"");
        }
        this.addColumnName(currentColumnName);
    }
    
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.JdbcParameter)
     */
    @Override
    public void visit(JdbcParameter arg0) {
        //カラム値へ追加し、パラメーターリストから削除
        this.addColumnValue(this.paramList.remove(0));
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.CaseExpression)
     */
    @Override
    public void visit(CaseExpression arg0) {
        //Swith
        Expression switchExp = arg0.getSwitchExpression();
        if (switchExp != null) {
            switchExp.accept(this);
        }
        //WHEN
        List<Expression> whenExpList = arg0.getWhenClauses();
        if (whenExpList != null && whenExpList.size() > 0) {
            for (Expression whenExp : whenExpList) {
                whenExp.accept(this);
            }
        }
        //ELSE
        Expression elseExp = arg0.getElseExpression();
        if (elseExp != null) {
            elseExp.accept(this);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.WhenClause)
     */
    @Override
    public void visit(WhenClause arg0) {
        if (arg0.getWhenExpression() != null) {
            arg0.getWhenExpression().accept(this);
        }
        if (arg0.getThenExpression() != null) {
            arg0.getThenExpression().accept(this);
        }
    }
    
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.AnalyticExpression)
     */
    @Override
    public void visit(AnalyticExpression arg0) {
        if (arg0.getExpression() != null) {
            arg0.getExpression().accept(this);
        }
    }
    
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.conditional.AndExpression)
     */
    @Override
    public void visit(AndExpression arg0) {
        //ANDのため関係式MUSTを追加
        this.currentBoolQueryType.push(CommonParams.BOOL_QUERY_TYPE.MUST);
        //左側処理
        arg0.getLeftExpression().accept(this);
        //右側処理
        arg0.getRightExpression().accept(this);
        //関係式削除
        this.currentBoolQueryType.pop();
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.conditional.OrExpression)
     */
    @Override
    public void visit(OrExpression arg0) {
        //ORのため関係式SHOULDを追加
        this.currentBoolQueryType.push(CommonParams.BOOL_QUERY_TYPE.SHOULD);
        //左側処理
        arg0.getLeftExpression().accept(this);
        //右側処理
        arg0.getRightExpression().accept(this);
        //関係式削除
        this.currentBoolQueryType.pop();
    }
    
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.Parenthesis)
     */
    @Override
    public void visit(Parenthesis arg0) {
        this.pushQueryBuilder(QueryBuilders.boolQuery());
        //
        this.currentBoolQueryType.push(CommonParams.BOOL_QUERY_TYPE.MUST);
        arg0.getExpression().accept(this);
        //関係式削除
        this.currentBoolQueryType.pop();
        
        BoolQueryBuilder parenthesisQuery = this.popQueryBuilder();
        if (parenthesisQuery.hasClauses()) {
            if (CommonParams.BOOL_QUERY_TYPE.MUST.equals(this.currentBoolQueryType.peek())) {
                this.peekQueryBuilder().must(parenthesisQuery);
            } else if (CommonParams.BOOL_QUERY_TYPE.SHOULD.equals(this.currentBoolQueryType.peek())) {
                this.peekQueryBuilder().should(parenthesisQuery);
            }
        }
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.Between)
     */
    @Override
    public void visit(Between arg0) {
        arg0.getLeftExpression().accept(this);
        boolean isExpressionNameLeft = currentColumnValueList != null && currentColumnValueList.size() > 0;
        arg0.getBetweenExpressionStart().accept(this);
        arg0.getBetweenExpressionEnd().accept(this);
        
        if (currentColumnNameList != null && currentColumnNameList.size() > 0
                && currentColumnValueList != null && currentColumnValueList.size() > 0) {
            if (isExpressionNameLeft && this.currentColumnValueList.size() == 2) {
                //「Column between JdbcParameter and JdbcParameter」の場合
                RangeQueryBuilder rangeBuilder1 = QueryBuilders.rangeQuery(this.currentColumnNameList.get(0));
                BoolQueryBuilder rangeQuery = QueryBuilders.boolQuery();
                if (arg0.isNot()) {
                    rangeBuilder1.gt(this.currentColumnValueList.get(0));
                    RangeQueryBuilder rangeBuilder2 = QueryBuilders.rangeQuery(this.currentColumnNameList.get(0)).lt(this.currentColumnValueList.get(1));
                    rangeQuery.should(rangeBuilder1).should(rangeBuilder2);
                } else {
                    rangeBuilder1.from(this.currentColumnValueList.get(0)).to(this.currentColumnValueList.get(1));
                    rangeQuery.must(rangeBuilder1);
                }
                
                if (CommonParams.BOOL_QUERY_TYPE.MUST.equals(this.currentBoolQueryType.peek())) {
                    this.peekQueryBuilder().must(rangeQuery);
                } else if (CommonParams.BOOL_QUERY_TYPE.SHOULD.equals(this.currentBoolQueryType.peek())) {
                    this.peekQueryBuilder().should(rangeQuery);
                }
            } else if (this.currentColumnValueList.size() == 1 && this.currentColumnNameList.size() == 2) {
                //「JdbcParameter between Column and Column」の場合
                RangeQueryBuilder rangeFrom = QueryBuilders.rangeQuery(this.currentColumnNameList.get(0));
                RangeQueryBuilder rangeTo = QueryBuilders.rangeQuery(this.currentColumnNameList.get(1));
                BoolQueryBuilder rangeQuery = QueryBuilders.boolQuery();
                if (arg0.isNot()) {
                    rangeFrom.gt(this.currentColumnValueList.get(0));
                    rangeTo.lt(this.currentColumnValueList.get(0));
                    rangeQuery.should(rangeFrom).should(rangeTo);
                } else {
                    rangeFrom.lte(this.currentColumnValueList.get(0));
                    rangeTo.gte(this.currentColumnValueList.get(0));
                    rangeQuery.must(rangeFrom).must(rangeTo);
                }
                
                if (CommonParams.BOOL_QUERY_TYPE.MUST.equals(this.currentBoolQueryType.peek())) {
                    this.peekQueryBuilder().must(rangeQuery);
                } else if (CommonParams.BOOL_QUERY_TYPE.SHOULD.equals(this.currentBoolQueryType.peek())) {
                    this.peekQueryBuilder().should(rangeQuery);
                }
            }
        }
        //ローカルパラメーターリセット
        this.resetCurrentColumnParameter();
    }
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.EqualsTo)
     */
    @Override
    public void visit(EqualsTo arg0) {
        arg0.getLeftExpression().accept(this);
        arg0.getRightExpression().accept(this);
        if (currentColumnNameList != null && currentColumnNameList.size() > 0
                && currentColumnValueList != null && currentColumnValueList.size() > 0) {
            if (CommonParams.BOOL_QUERY_TYPE.MUST.equals(this.currentBoolQueryType.peek())) {
                this.peekQueryBuilder().must(QueryBuilders.termQuery(this.currentColumnNameList.get(0), this.currentColumnValueList.get(0)));
            } else if (CommonParams.BOOL_QUERY_TYPE.SHOULD.equals(this.currentBoolQueryType.peek())) {
                this.peekQueryBuilder().should(QueryBuilders.termQuery(this.currentColumnNameList.get(0), this.currentColumnValueList.get(0)));
            }
        }
        //ローカルパラメーターリセット
        this.resetCurrentColumnParameter();
    }
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.NotEqualsTo)
     */
    @Override
    public void visit(NotEqualsTo arg0) {
        arg0.getLeftExpression().accept(this);
        arg0.getRightExpression().accept(this);
        if (currentColumnNameList != null && currentColumnNameList.size() > 0
                && currentColumnValueList != null && currentColumnValueList.size() > 0) {
            if (CommonParams.BOOL_QUERY_TYPE.MUST.equals(this.currentBoolQueryType.peek())) {
                this.peekQueryBuilder().mustNot(QueryBuilders.termQuery(this.currentColumnNameList.get(0), this.currentColumnValueList.get(0)));
            } else if (CommonParams.BOOL_QUERY_TYPE.SHOULD.equals(this.currentBoolQueryType.peek())) {
                this.peekQueryBuilder().should(QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery(this.currentColumnNameList.get(0), this.currentColumnValueList.get(0))));
            }
        }
        //ローカルパラメーターリセット
        this.resetCurrentColumnParameter();
    }


    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.IsNullExpression)
     */
    @Override
    public void visit(IsNullExpression arg0) {
        arg0.getLeftExpression().accept(this);
        //
        if (currentColumnNameList != null && currentColumnNameList.size() > 0) {
            if (CommonParams.BOOL_QUERY_TYPE.MUST.equals(this.currentBoolQueryType.peek())) {
                this.peekQueryBuilder().mustNot(QueryBuilders.existsQuery(this.currentColumnNameList.get(0)));
            } else if (CommonParams.BOOL_QUERY_TYPE.SHOULD.equals(this.currentBoolQueryType.peek())) {
                this.peekQueryBuilder().should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(this.currentColumnNameList.get(0))));
            }
        }
        //ローカルパラメーターリセット
        this.resetCurrentColumnParameter();
    }
    
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.GreaterThan)
     */
    @Override
    public void visit(GreaterThan arg0) {
        arg0.getLeftExpression().accept(this);
        boolean isExpressionNameLeft = currentColumnNameList != null && currentColumnNameList.size() > 0;
        arg0.getRightExpression().accept(this);
        if (currentColumnNameList != null && currentColumnNameList.size() > 0
                && currentColumnValueList != null && currentColumnValueList.size() > 0) {
            if (CommonParams.BOOL_QUERY_TYPE.MUST.equals(this.currentBoolQueryType.peek())) {
                if (isExpressionNameLeft) {
                    this.peekQueryBuilder().must(QueryBuilders.rangeQuery(this.currentColumnNameList.get(0)).gt(this.currentColumnValueList.get(0)));
                } else {
                    this.peekQueryBuilder().must(QueryBuilders.rangeQuery(this.currentColumnNameList.get(0)).lt(this.currentColumnValueList.get(0)));
                }
            } else if (CommonParams.BOOL_QUERY_TYPE.SHOULD.equals(this.currentBoolQueryType.peek())) {
                if (isExpressionNameLeft) {
                    this.peekQueryBuilder().should(QueryBuilders.rangeQuery(this.currentColumnNameList.get(0)).gt(this.currentColumnValueList.get(0)));
                } else {
                    this.peekQueryBuilder().should(QueryBuilders.rangeQuery(this.currentColumnNameList.get(0)).lt(this.currentColumnValueList.get(0)));
                }
            }
        }
        //ローカルパラメーターリセット
        this.resetCurrentColumnParameter();
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals)
     */
    @Override
    public void visit(GreaterThanEquals arg0) {
        arg0.getLeftExpression().accept(this);
        boolean isExpressionNameLeft = currentColumnNameList != null && currentColumnNameList.size() > 0;
        arg0.getRightExpression().accept(this);
        if (currentColumnNameList != null && currentColumnNameList.size() > 0
                && currentColumnValueList != null && currentColumnValueList.size() > 0) {
            if (CommonParams.BOOL_QUERY_TYPE.MUST.equals(this.currentBoolQueryType.peek())) {
                if (isExpressionNameLeft) {
                    this.peekQueryBuilder().must(QueryBuilders.rangeQuery(this.currentColumnNameList.get(0)).gte(this.currentColumnValueList.get(0)));
                } else {
                    this.peekQueryBuilder().must(QueryBuilders.rangeQuery(this.currentColumnNameList.get(0)).lte(this.currentColumnValueList.get(0)));
                }
            } else if (CommonParams.BOOL_QUERY_TYPE.SHOULD.equals(this.currentBoolQueryType.peek())) {
                if (isExpressionNameLeft) {
                    this.peekQueryBuilder().should(QueryBuilders.rangeQuery(this.currentColumnNameList.get(0)).gte(this.currentColumnValueList.get(0)));
                } else {
                    this.peekQueryBuilder().should(QueryBuilders.rangeQuery(this.currentColumnNameList.get(0)).lte(this.currentColumnValueList.get(0)));
                }
            }
        }
        //ローカルパラメーターリセット
        this.resetCurrentColumnParameter();
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.MinorThan)
     */
    @Override
    public void visit(MinorThan arg0) {
        arg0.getLeftExpression().accept(this);
        boolean isExpressionNameLeft = currentColumnNameList != null && currentColumnNameList.size() > 0;
        arg0.getRightExpression().accept(this);
        if (currentColumnNameList != null && currentColumnNameList.size() > 0
                && currentColumnValueList != null && currentColumnValueList.size() > 0) {
            if (CommonParams.BOOL_QUERY_TYPE.MUST.equals(this.currentBoolQueryType.peek())) {
                if (isExpressionNameLeft) {
                    this.peekQueryBuilder().must(QueryBuilders.rangeQuery(this.currentColumnNameList.get(0)).lt(this.currentColumnValueList.get(0)));
                } else {
                    this.peekQueryBuilder().must(QueryBuilders.rangeQuery(this.currentColumnNameList.get(0)).gt(this.currentColumnValueList.get(0)));
                }
            } else if (CommonParams.BOOL_QUERY_TYPE.SHOULD.equals(this.currentBoolQueryType.peek())) {
                if (isExpressionNameLeft) {
                    this.peekQueryBuilder().should(QueryBuilders.rangeQuery(this.currentColumnNameList.get(0)).lt(this.currentColumnValueList.get(0)));
                } else {
                    this.peekQueryBuilder().should(QueryBuilders.rangeQuery(this.currentColumnNameList.get(0)).gt(this.currentColumnValueList.get(0)));
                }
            }
        }
        //ローカルパラメーターリセット
        this.resetCurrentColumnParameter();
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.MinorThanEquals)
     */
    @Override
    public void visit(MinorThanEquals arg0) {
        arg0.getLeftExpression().accept(this);
        boolean isExpressionNameLeft = currentColumnNameList != null && currentColumnNameList.size() > 0;
        arg0.getRightExpression().accept(this);
        if (currentColumnNameList != null && currentColumnNameList.size() > 0
                && currentColumnValueList != null && currentColumnValueList.size() > 0) {
            if (CommonParams.BOOL_QUERY_TYPE.MUST.equals(this.currentBoolQueryType.peek())) {
                if (isExpressionNameLeft) {
                    this.peekQueryBuilder().must(QueryBuilders.rangeQuery(this.currentColumnNameList.get(0)).lte(this.currentColumnValueList.get(0)));
                } else {
                    this.peekQueryBuilder().must(QueryBuilders.rangeQuery(this.currentColumnNameList.get(0)).gte(this.currentColumnValueList.get(0)));
                }
            } else if (CommonParams.BOOL_QUERY_TYPE.SHOULD.equals(this.currentBoolQueryType.peek())) {
                if (isExpressionNameLeft) {
                    this.peekQueryBuilder().should(QueryBuilders.rangeQuery(this.currentColumnNameList.get(0)).lte(this.currentColumnValueList.get(0)));
                } else {
                    this.peekQueryBuilder().should(QueryBuilders.rangeQuery(this.currentColumnNameList.get(0)).gte(this.currentColumnValueList.get(0)));
                }
            }
        }
        //ローカルパラメーターリセット
        this.resetCurrentColumnParameter();
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.InExpression)
     */
    @Override
    public void visit(InExpression arg0) {
        arg0.getLeftExpression().accept(this);
        ItemsList inRightValues = arg0.getRightItemsList();
        inRightValues.accept(new ListVisitor(this));
        if (currentColumnNameList != null && currentColumnNameList.size() > 0
                && currentColumnValueList != null && currentColumnValueList.size() > 0) {
            if (CommonParams.BOOL_QUERY_TYPE.MUST.equals(this.currentBoolQueryType.peek())) {
                this.peekQueryBuilder().must(QueryBuilders.termsQuery(this.currentColumnNameList.get(0), this.currentColumnValueList));
            } else if (CommonParams.BOOL_QUERY_TYPE.SHOULD.equals(this.currentBoolQueryType.peek())) {
                this.peekQueryBuilder().should(QueryBuilders.termsQuery(this.currentColumnNameList.get(0), this.currentColumnValueList));
            }
        }
        //ローカルパラメーターリセット
        this.resetCurrentColumnParameter();
    }
    
    /**
     * Create Relation Query by definition of JoinRelation.
     * @param joinRelation definition of relationship
     * @param preRelation
     * @return
     */
    protected static QueryBuilder createRelationQuery(JoinRelation joinRelation, JoinRelation preRelation, String name, BoolQueryBuilder queryClauses) {
        QueryBuilder retValue = null;
        if (joinRelation == null) {
            return retValue;
        }
        
        boolean isParent = false;
        JoinRelation nextRelation = null;
        BoolQueryBuilder nextQueryBool = QueryBuilders.boolQuery();
        JoinRelation parent = joinRelation.getParent();
        List<JoinRelation> children = joinRelation.getChildren();
        if (parent != null && !parent.equals(preRelation)) {
            //retValue = JoinQueryBuilders.hasParentQuery(parent.getName(), nextQueryBool, false);
            nextRelation = parent;
            isParent = true;
        } else if (children != null && children.size() > 0) {
            for (JoinRelation child : children) {
                if (child != null && !child.equals(preRelation)) {
                    //retValue = JoinQueryBuilders.hasChildQuery(child.getName(), nextQueryBool, ScoreMode.None);
                    nextRelation = child;
                    break;
                }
            }
        }
        
        if (nextRelation != null) {
        	if (name.equals(nextRelation.getName())) {
        		if (isParent) {
        			retValue = JoinQueryBuilders.hasParentQuery(nextRelation.getName(), queryClauses, false);
        		} else {
        			retValue = JoinQueryBuilders.hasChildQuery(nextRelation.getName(), queryClauses, ScoreMode.None);
        		}
        	} else {
        		if (isParent) {
        			retValue = JoinQueryBuilders.hasParentQuery(nextRelation.getName(), nextQueryBool, false);
        		} else {
        			retValue = JoinQueryBuilders.hasChildQuery(nextRelation.getName(), nextQueryBool, ScoreMode.None);
        		}
	            QueryBuilder nextQuery = createRelationQuery(nextRelation, joinRelation, name, queryClauses);
	            if (nextQuery != null) {
	                nextQueryBool.must(nextQuery);
	            }
        	}
        }
        
        return retValue;
    }
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.ExistsExpression)
     */
    @Override
    public void visit(ExistsExpression arg0) {
        //SubSelectの場合
        if (arg0.getRightExpression() != null && arg0.getRightExpression() instanceof SubSelect) {
            //SelectVisitor初期化
            SelectAnalyzer selectVisitor = new SelectAnalyzer(SelectAnalyzer.PROCESS_MODE.EXISTS_CLAUSE, paramList, analyzerPolicy);
            //selectVisitor.setExistsClause(true);
            ((SubSelect) arg0.getRightExpression()).getSelectBody().accept(selectVisitor);
            SQLInfo sqlInfo = selectVisitor.getAnalyticSqlInfo();
            //Parent-Childクエリ作成
            //if (StringUtils.isNotBlank(sqlInfo.getTableName()) && sqlInfo.isQueryClauseExists()) {
            if (StringUtils.isNotBlank(sqlInfo.getTableName())) {
                //ParentType名称が存在、且つExists内検索条件が存在の場合
                //QueryBuilder relationQuery = null;
                //boolean isParent = false;
                
                //JoinRelation joinRelation = analyzerPolicy.getJoinRelation();
                JoinRelation joinRelation = CommonUtils.getRelationPath(analyzerPolicy.getJoinRelation(), this.tableName, sqlInfo.getTableName());
//                JoinRelation parent = joinRelation.getParent();
//                List<JoinRelation> children = joinRelation.getChildren();
//                try {
//                    //TODO isParent
//System.out.println(this.tableName);
//System.out.println(sqlInfo.getTableName());
//                    isParent = CommonUtils.isParent(this.tableName, this.tableName, sqlInfo.getTableName());
//                } catch (IOException e) {
//                    throw new ElasticsearchJDBCException(e);
//                }
//                if (isParent) {
//                    relationQuery = JoinQueryBuilders.hasParentQuery(sqlInfo.getTableName(), sqlInfo.getQueryClauses(), false);
//                } else {
//                    relationQuery = JoinQueryBuilders.hasChildQuery(sqlInfo.getTableName(), sqlInfo.getQueryClauses(), ScoreMode.None);
//                }
                QueryBuilder relationQuery = createRelationQuery(joinRelation, joinRelation, sqlInfo.getTableName(), sqlInfo.getQueryClauses());
                if (arg0.isNot()) {
                    if (CommonParams.BOOL_QUERY_TYPE.MUST.equals(this.currentBoolQueryType.peek())) {
                        this.peekQueryBuilder().mustNot(relationQuery);
                    } else if (CommonParams.BOOL_QUERY_TYPE.SHOULD.equals(this.currentBoolQueryType.peek())) {
                        this.peekQueryBuilder().should(QueryBuilders.boolQuery().mustNot(relationQuery));
                    }
                } else {
                    if (CommonParams.BOOL_QUERY_TYPE.MUST.equals(this.currentBoolQueryType.peek())) {
                        this.peekQueryBuilder().must(relationQuery);
                    } else if (CommonParams.BOOL_QUERY_TYPE.SHOULD.equals(this.currentBoolQueryType.peek())) {
                        this.peekQueryBuilder().should(relationQuery);
                    }
                }
            }
        }
        
        //ローカルパラメーターリセット
        this.resetCurrentColumnParameter();
    }
    
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.statement.select.SubSelect)
     */
    @Override
    public void visit(SubSelect arg0) {
        if (arg0.getSelectBody() instanceof PlainSelect) {
            Expression subSelectWhere = ((PlainSelect) arg0.getSelectBody()).getWhere();
            if (subSelectWhere != null) {
                //ANDのため関係式MUSTを追加
                this.currentBoolQueryType.push(CommonParams.BOOL_QUERY_TYPE.MUST);
                subSelectWhere.accept(this);
                
                //
                this.popQueryBuilder();
                //関係式削除
                this.currentBoolQueryType.pop();
            }
        }
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.DoubleValue)
     */
    @Override
    public void visit(DoubleValue arg0) {
        if (!analyzerPolicy.isIgnoreValueCondition()) {
            addColumnValue(arg0.getValue());
        }
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.LongValue)
     */
    @Override
    public void visit(LongValue arg0) {
        if (!analyzerPolicy.isIgnoreValueCondition()) {
            addColumnValue(arg0.getValue());
        }
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.HexValue)
     */
    @Override
    public void visit(HexValue arg0) {
        if (!analyzerPolicy.isIgnoreValueCondition()) {
            addColumnValue(arg0.getValue());
        }
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.DateValue)
     */
    @Override
    public void visit(DateValue arg0) {
        if (!analyzerPolicy.isIgnoreValueCondition()) {
            addColumnValue(arg0.getValue());
        }
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.TimeValue)
     */
    @Override
    public void visit(TimeValue arg0) {
        if (!analyzerPolicy.isIgnoreValueCondition()) {
            addColumnValue(arg0.getValue());
        }
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.TimestampValue)
     */
    @Override
    public void visit(TimestampValue arg0) {
        if (!analyzerPolicy.isIgnoreValueCondition()) {
            addColumnValue(arg0.getValue());
        }
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.StringValue)
     */
    @Override
    public void visit(StringValue arg0) {
        if (!analyzerPolicy.isIgnoreValueCondition()) {
            addColumnValue(arg0.getValue());
        }
    }
    
    /**
     * TODO 以下未実装.
     */

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.SignedExpression)
     */
    @Override
    public void visit(SignedExpression arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.JdbcNamedParameter)
     */
    @Override
    public void visit(JdbcNamedParameter arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.arithmetic.Addition)
     */
    @Override
    public void visit(Addition arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.arithmetic.Division)
     */
    @Override
    public void visit(Division arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.arithmetic.Multiplication)
     */
    @Override
    public void visit(Multiplication arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.arithmetic.Subtraction)
     */
    @Override
    public void visit(Subtraction arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.LikeExpression)
     */
    @Override
    public void visit(LikeExpression arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.AllComparisonExpression)
     */
    @Override
    public void visit(AllComparisonExpression arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.AnyComparisonExpression)
     */
    @Override
    public void visit(AnyComparisonExpression arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.arithmetic.Concat)
     */
    @Override
    public void visit(Concat arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.Matches)
     */
    @Override
    public void visit(Matches arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd)
     */
    @Override
    public void visit(BitwiseAnd arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr)
     */
    @Override
    public void visit(BitwiseOr arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor)
     */
    @Override
    public void visit(BitwiseXor arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.CastExpression)
     */
    @Override
    public void visit(CastExpression arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.arithmetic.Modulo)
     */
    @Override
    public void visit(Modulo arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.WithinGroupExpression)
     */
    @Override
    public void visit(WithinGroupExpression arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.ExtractExpression)
     */
    @Override
    public void visit(ExtractExpression arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.IntervalExpression)
     */
    @Override
    public void visit(IntervalExpression arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.OracleHierarchicalExpression)
     */
    @Override
    public void visit(OracleHierarchicalExpression arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.RegExpMatchOperator)
     */
    @Override
    public void visit(RegExpMatchOperator arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.JsonExpression)
     */
    @Override
    public void visit(JsonExpression arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.RegExpMySQLOperator)
     */
    @Override
    public void visit(RegExpMySQLOperator arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.UserVariable)
     */
    @Override
    public void visit(UserVariable arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.NumericBind)
     */
    @Override
    public void visit(NumericBind arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.KeepExpression)
     */
    @Override
    public void visit(KeepExpression arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.MySQLGroupConcat)
     */
    @Override
    public void visit(MySQLGroupConcat arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.RowConstructor)
     */
    @Override
    public void visit(RowConstructor arg0) {
        
    }
    
    /**
     * 分析中DSL文を格納.
     * @param queryBuilder DSL文
     */
    public void pushQueryBuilder(final BoolQueryBuilder queryBuilder) {
        this.queryBuilderStack.push(queryBuilder);
    }
    /**
     * 分析中DSL文を取得.
     * @return DSL文
     */
    public BoolQueryBuilder peekQueryBuilder() {
        return this.queryBuilderStack.peek();
    }
    /**
     * 分析中DSL文を取得.
     * @return DSL文
     */
    public BoolQueryBuilder popQueryBuilder() {
        return this.queryBuilderStack.pop();
    }
    /**
     * 現在処理中カラム名称を追加.
     * @param name カラム名称
     */
    public void addColumnName(final String name) {
        if (this.currentColumnNameList == null) {
            this.currentColumnNameList = new ArrayList<String>();
        }
        this.currentColumnNameList.add(name);
    }
    /**
     * 現在処理中カラム値を追加.
     * @param value カラム値
     */
    public void addColumnValue(final Object value) {
        if (this.currentColumnValueList == null) {
            this.currentColumnValueList = new ArrayList<Object>();
        }
        this.currentColumnValueList.add(value);
    }
    /**
     * ローカルリストをリセット.
     */
    public void resetCurrentColumnParameter() {
        this.currentColumnNameList = null;
        this.currentColumnValueList = null;
    }
    
    /**
     * @return the scoreQueryBuilderStack
     */
    public List<QueryBuilder> getScoreQueryBuilderStack() {
        if (this.scoreQueryBuilderStack.size() == 0) {
            return null;
        }
        return this.scoreQueryBuilderStack;
    }
    /**
     * @param scoreQueryBuilderStack the scoreQueryBuilderStack to set
     */
    public void setScoreQueryBuilderStack(final List<QueryBuilder> scoreQueryBuilderStack) {
        this.scoreQueryBuilderStack = scoreQueryBuilderStack;
    }
    
    /**
     * @return the tableName
     */
    public String getTableName() {
        return tableName;
    }
    /**
     * @param tableName the tableName to set
     */
    public void setTableName(final String tableName) {
        this.tableName = tableName;
    }

    @Override
    public void visit(JsonOperator jsonExpr) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(OracleHint hint) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(TimeKeyExpression timeKeyExpression) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(DateTimeLiteralExpression literal) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void visit(NotExpression aThis) {
        // TODO Auto-generated method stub
        
    }
}
