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

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItemVisitor;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.TableFunction;
import net.sf.jsqlparser.statement.select.ValuesList;

/**
 * This analyzer is designed for analyzing the FROM clauses in SQL.
 * 
 * @author Ming Zhu
 * 
 */
public class FromAnalyzer implements FromItemVisitor{
    /**
     * List of table names in the From clause.
     */
    protected List<String> tableList;
    /**
     * Analyzer of sub Query in the From clause.
     */
    protected SelectAnalyzer selectVisitor;
    
    /**
     * Get the first table name in the From clause.  
     * @return table name
     */
    public String getFirstTable() {
        String retValue = null;
        if (tableList != null && tableList.size() > 0) {
            retValue = tableList.get(0);
        }
        return retValue;
    }
    /**
     * Get amount of tables in the From clause.
     * @return amount of tables
     */
    public int tableSize() {
        int retValue = 0;
        if (tableList != null) {
            retValue = tableList.size();
        }
        return retValue;
    }
    /**
     * Set analyzer of sub query.
     * @param selectVisitor analyzer of sub query
     */
    public void setSelectVisitor(final SelectAnalyzer selectVisitor) {
        this.selectVisitor = selectVisitor;
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.statement.select.FromItemVisitor#visit(net.sf.jsqlparser.schema.Table)
     */
    @Override
    public void visit(Table arg0) {
        //this.table = arg0.getName();
        if (tableList == null) {
            tableList = new ArrayList<String>();
        }
        tableList.add(arg0.getName());
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.statement.select.FromItemVisitor#visit(net.sf.jsqlparser.statement.select.SubSelect)
     */
    @Override
    public void visit(SubSelect arg0) {
        selectVisitor.increaseCurrentLevel();
        arg0.getSelectBody().accept(selectVisitor);
        selectVisitor.decreaseCurrentLevel();
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.statement.select.FromItemVisitor#visit(net.sf.jsqlparser.statement.select.SubJoin)
     */
    @Override
    public void visit(SubJoin arg0) {
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.statement.select.FromItemVisitor#visit(net.sf.jsqlparser.statement.select.LateralSubSelect)
     */
    @Override
    public void visit(LateralSubSelect arg0) {
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.statement.select.FromItemVisitor#visit(net.sf.jsqlparser.statement.select.ValuesList)
     */
    @Override
    public void visit(ValuesList arg0) {
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.statement.select.FromItemVisitor#visit(net.sf.jsqlparser.statement.select.TableFunction)
     */
    @Override
    public void visit(TableFunction tableFunction) {
    }
}
