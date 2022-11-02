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

import java.util.List;

import es.jdbc.sql.beans.AnalyzerPolicy;
import es.jdbc.sql.search.ISelectItem;
import es.jdbc.sql.search.SelectItemFactory;
import es.jdbc.sql.search.sources.Source;

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
import net.sf.jsqlparser.expression.operators.relational.JsonOperator;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.Matches;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.expression.operators.relational.RegExpMatchOperator;
import net.sf.jsqlparser.expression.operators.relational.RegExpMySQLOperator;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SubSelect;

public class SelectExpressionAnalyzer implements ExpressionVisitor {
    /**
     * パラメーターリスト.
     */
    protected List<Object> paramList;
    /**
     * 検索項目.
     */
    protected ISelectItem selectItem;
    /**
     * analyzerPolicy.
     */
    protected AnalyzerPolicy analyzerPolicy;
    
    /**
     * コンストラクター.
     * @param paramList パラメーターリスト.
     */
    public SelectExpressionAnalyzer(final List<Object> paramList, final AnalyzerPolicy analyzerPolicy) {
        this.paramList = paramList;
        this.analyzerPolicy = analyzerPolicy;
    }

    /**
     * @return the selectItem
     */
    public ISelectItem getSelectItem() {
        return selectItem;
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
        if (this.selectItem == null) {
            this.selectItem = SelectItemFactory.manufacture(arg0, this.paramList);
        }
        if (this.selectItem == null) {
            // 全Paramループ
            ExpressionList paramList = arg0.getParameters();
            List<Expression> expressionList = null;
            if (paramList != null && (expressionList = paramList.getExpressions()) != null) {
                for (Expression expression : expressionList) {
                    expression.accept(this);
                }
            }
        }
    }
    
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.schema.Column)
     */
    @Override
    public void visit(Column arg0) {
        if (this.selectItem == null) {
            this.selectItem = SelectItemFactory.manufacture(arg0, this.paramList);
        }
    }
    
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.JdbcParameter)
     */
    @Override
    public void visit(JdbcParameter arg0) {
        //Select内パラメーターの場合、該当するパラメーターをパラメーターリストから削除
        this.paramList.remove(0);
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.CaseExpression)
     */
    @Override
    public void visit(CaseExpression arg0) {
        if (this.selectItem == null) {
            this.selectItem = new Source();
        }
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
            //When表現式
            arg0.getWhenExpression().accept(this);
        }
        if (arg0.getThenExpression() != null) {
            //Then表現式
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
        arg0.getLeftExpression().accept(this);
        arg0.getRightExpression().accept(this);
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.conditional.OrExpression)
     */
    @Override
    public void visit(OrExpression arg0) {
        arg0.getLeftExpression().accept(this);
        arg0.getRightExpression().accept(this);
    }
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.Parenthesis)
     */
    @Override
    public void visit(Parenthesis arg0) {
        arg0.getExpression().accept(this);
    }
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.EqualsTo)
     */
    @Override
    public void visit(EqualsTo arg0) {
        arg0.getLeftExpression().accept(this);
        arg0.getRightExpression().accept(this);
    }
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.NotEqualsTo)
     */
    @Override
    public void visit(NotEqualsTo arg0) {
        arg0.getLeftExpression().accept(this);
        arg0.getRightExpression().accept(this);
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.Between)
     */
    @Override
    public void visit(Between arg0) {
        arg0.getLeftExpression().accept(this);
        arg0.getBetweenExpressionStart().accept(this);
        arg0.getBetweenExpressionEnd().accept(this);
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.IsNullExpression)
     */
    @Override
    public void visit(IsNullExpression arg0) {
        arg0.getLeftExpression().accept(this);
    }
    
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.GreaterThan)
     */
    @Override
    public void visit(GreaterThan arg0) {
        arg0.getLeftExpression().accept(this);
        arg0.getRightExpression().accept(this);
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals)
     */
    @Override
    public void visit(GreaterThanEquals arg0) {
        arg0.getLeftExpression().accept(this);
        arg0.getRightExpression().accept(this);
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.MinorThan)
     */
    @Override
    public void visit(MinorThan arg0) {
        arg0.getLeftExpression().accept(this);
        arg0.getRightExpression().accept(this);
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.MinorThanEquals)
     */
    @Override
    public void visit(MinorThanEquals arg0) {
        arg0.getLeftExpression().accept(this);
        arg0.getRightExpression().accept(this);
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.statement.select.SubSelect)
     */
    @Override
    public void visit(SubSelect arg0) {
        SelectAnalyzer selectVisitor = new SelectAnalyzer(paramList, analyzerPolicy);
        arg0.getSelectBody().accept(selectVisitor);
    }
    
    /**
     * TODO 以下は未実装.
     */
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.StringValue)
     */
    @Override
    public void visit(StringValue arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.DoubleValue)
     */
    @Override
    public void visit(DoubleValue arg0) {
        
    }
    
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
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.LongValue)
     */
    @Override
    public void visit(LongValue arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.HexValue)
     */
    @Override
    public void visit(HexValue arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.DateValue)
     */
    @Override
    public void visit(DateValue arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.TimeValue)
     */
    @Override
    public void visit(TimeValue arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.TimestampValue)
     */
    @Override
    public void visit(TimestampValue arg0) {
        
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
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.InExpression)
     */
    @Override
    public void visit(InExpression arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.LikeExpression)
     */
    @Override
    public void visit(LikeExpression arg0) {
        
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.ExpressionVisitor#visit(net.sf.jsqlparser.expression.operators.relational.ExistsExpression)
     */
    @Override
    public void visit(ExistsExpression arg0) {
        
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
