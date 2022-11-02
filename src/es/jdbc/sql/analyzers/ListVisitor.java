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

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor;
import net.sf.jsqlparser.expression.operators.relational.MultiExpressionList;
import net.sf.jsqlparser.statement.select.SubSelect;

/**
 * This analyzer is designed for analyzing the values of IN clauses in SQL.
 * e.g. WHERE user_id IN (1000, 1001, 1002)
 * 
 * @author Ming Zhu
 * 
 */
public class ListVisitor implements ItemsListVisitor {

    /**
     * The Where analyzer which this Expression is exist.
     */
    private ExpressionVisitor whereAnalyzer;
    /**
     * コンストラクター.
     * @param expressionVisitor ExpressionVisitorインスタンス
     */
    public ListVisitor(final ExpressionVisitor whereAnalyzer) {
        this.whereAnalyzer = whereAnalyzer;
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor#visit(net.sf.jsqlparser.expression.operators.relational.ExpressionList)
     */
    @Override
    public void visit(ExpressionList arg0) {
        List<Expression> expressionList = arg0.getExpressions();
        if (expressionList != null && expressionList.size() > 0) {
            for (Expression expression : expressionList) {
                expression.accept(whereAnalyzer);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor#visit(net.sf.jsqlparser.statement.select.SubSelect)
     */
    @Override
    public void visit(SubSelect arg0) {
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor#visit(net.sf.jsqlparser.expression.operators.relational.MultiExpressionList)
     */
    @Override
    public void visit(MultiExpressionList arg0) {
    }
}
