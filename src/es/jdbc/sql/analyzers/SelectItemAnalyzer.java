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

import org.apache.commons.lang3.StringUtils;

import es.jdbc.sql.beans.AnalyzerPolicy;
import es.jdbc.sql.search.ISelectItem;
import es.jdbc.sql.search.sources.Source;

import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitor;

public class SelectItemAnalyzer implements SelectItemVisitor{

    /**
     * パラメーターリスト.
     */
    protected List<Object> paramList;
    /**
     * ソースリスト.
     */
    protected List<ISelectItem> selectItems;
    /**
     * 対象タイプ名称.
     */
    protected String type;
    /**
     * analyzerPolicy.
     */
    protected AnalyzerPolicy analyzerPolicy;
    /**
     * コンストラクター.
     * @param paramList パラメーターリスト.
     */
    public SelectItemAnalyzer(final List<Object> paramList, final AnalyzerPolicy analyzerPolicy) {
        this.paramList = paramList;
        this.selectItems = new ArrayList<ISelectItem>();
        this.analyzerPolicy = analyzerPolicy;
    }
    
    /**
     * @return the selectItems
     */
    public List<ISelectItem> getSelectItems() {
        return selectItems;
    }

    /**
     * @param selectItems the selectItems to set
     */
    public void setSelectItems(List<ISelectItem> selectItems) {
        this.selectItems = selectItems;
    }
    
    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }
    
    /* (non-Javadoc)
     * @see net.sf.jsqlparser.statement.select.SelectItemVisitor#visit(net.sf.jsqlparser.statement.select.AllColumns)
     */
    @Override
    public void visit(AllColumns arg0) {
        //ALLの場合は暫定処理しない
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.statement.select.SelectItemVisitor#visit(net.sf.jsqlparser.statement.select.AllTableColumns)
     */
    @Override
    public void visit(AllTableColumns arg0) {
        //AllTableの場合は暫定処理しない
    }

    /* (non-Javadoc)
     * @see net.sf.jsqlparser.statement.select.SelectItemVisitor#visit(net.sf.jsqlparser.statement.select.SelectExpressionItem)
     */
    @Override
    public void visit(SelectExpressionItem arg0) {
        // 検索項目分析(バイナリ変数を除外)
        SelectExpressionAnalyzer expressionVisitor = new SelectExpressionAnalyzer(paramList, analyzerPolicy);
        arg0.getExpression().accept(expressionVisitor);
        ISelectItem selectItem = expressionVisitor.getSelectItem();
        if (arg0.getAlias() != null) {
            if (selectItem == null) {
                selectItem = new Source();
            }
            String sourceName = arg0.getAlias().getName();
            if (StringUtils.startsWith(sourceName, "\"")
                    && StringUtils.endsWith(sourceName, "\"")) {
                sourceName = StringUtils.removeStart(sourceName, "\"");
                sourceName = StringUtils.removeEnd(sourceName, "\"");
            }
            selectItem.setName(sourceName);
        }
        // 検索項目を取得できた場合
        if (selectItem != null) {
            selectItem.setType(this.type);
            this.selectItems.add(selectItem);
        }
    }
}
