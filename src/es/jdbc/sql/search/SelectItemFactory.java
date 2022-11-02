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
package es.jdbc.sql.search;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import es.jdbc.sql.search.functions.IFunction;
import es.jdbc.sql.search.sources.ISource;
import es.jdbc.sql.search.sources.IValue;
import es.jdbc.sql.search.sources.Source;
import es.jdbc.sql.search.sources.Value;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;

public class SelectItemFactory {
    /**
     * Function項目作成.
     * @param function 関数
     * @param jdbcValues JDBC値
     * @return Function項目
     */
    public static IFunction manufacture(final Function function, final List<Object> jdbcValues) {
        //戻り値
        IFunction retValue = IFunction.FUNCTION_LIST.findFunction(function);
        if (retValue != null) {
            String functionName = function.getName();
            
            retValue.setName(functionName);
            retValue.setDistinct(function.isDistinct());
            retValue.setParameters(function.getParameters(), jdbcValues);
        }
        
        return retValue;
    }
    /**
     * Source項目作成.
     * @param column カラム
     * @return Source項目
     */
    public static ISource manufacture(final Column column) {
        ISource retValue = null;
        if (column == null) {
            return retValue;
        }
        //戻り値
        retValue = new Source();
        
        String sourceName = column.getColumnName();
        if (org.apache.commons.lang3.StringUtils.startsWith(sourceName, "\"")
                && org.apache.commons.lang3.StringUtils.endsWith(sourceName, "\"")) {
            sourceName = org.apache.commons.lang3.StringUtils.removeStart(sourceName, "\"");
            sourceName = org.apache.commons.lang3.StringUtils.removeEnd(sourceName, "\"");
        }
        retValue.setName(sourceName);
        
        return retValue;
    }
    /**
     * 値項目作成.
     * @param sqlValue 値項目
     * @return 値項目
     */
    public static IValue manufacture(final StringValue sqlValue) {
        IValue retValue = null;
        if (sqlValue == null) {
            return retValue;
        }
        //戻り値
        retValue = new Value();
        String value = sqlValue.getValue();
        retValue.setValue(value);
        
        return retValue;
    }
    /**
     * 値項目作成.
     * @param sqlValue 値項目
     * @return 値項目
     */
    public static IValue manufacture(final LongValue sqlValue) {
        IValue retValue = null;
        if (sqlValue == null) {
            return retValue;
        }
        //戻り値
        retValue = new Value();
        long value = sqlValue.getValue();
        retValue.setValue(value);
        
        return retValue;
    }
    /**
     * 値項目作成.
     * @param sqlValue 値項目
     * @return 値項目
     */
    public static IValue manufacture(final JdbcParameter sqlValue, final List<Object> jdbcValues) {
        IValue retValue = new Value();
        retValue.setValue(jdbcValues.remove(0));
        return retValue;
    }
    
    /**
     * SelectItem情報作成.
     * @param selectExpression 検索情報
     * @return SelectItem情報
     */
    public static ISelectItem manufacture(final SelectExpressionItem selectExpression,
            final List<Object> jdbcValues) {
        ISelectItem retValue = null;
        //Functionの場合
        if (selectExpression.getExpression() instanceof Function) {
            retValue = manufacture((Function) selectExpression.getExpression(), jdbcValues);
        }
        //未サポートFunction又はSourceの場合
        if (retValue == null) {
            //Source処理
            String sourceName = null;
            if (selectExpression.getAlias() != null) {
                //別名が設定されている場合は別名で設定
                sourceName = selectExpression.getAlias().getName();
            } else if (selectExpression.getExpression() instanceof Column) {
                //カラム抽出の場合はカラム名称で設定
                sourceName = ((Column) selectExpression.getExpression()).getColumnName();
            }
            
            //SourceName取得できた場合
            if (StringUtils.isNotBlank(sourceName)) {
                // サブ名称の場合（"にて囲まれる）は、「"」を削除
                if (org.apache.commons.lang3.StringUtils.startsWith(sourceName, "\"")
                        && org.apache.commons.lang3.StringUtils.endsWith(sourceName, "\"")) {
                    sourceName = org.apache.commons.lang3.StringUtils.removeStart(sourceName, "\"");
                    sourceName = org.apache.commons.lang3.StringUtils.removeEnd(sourceName, "\"");
                }
                //Source作成
                retValue = new Source();
                retValue.setName(sourceName);
            }
        }
        return retValue;
    }
    
    /**
     * Function項目作成.
     * @param function Function
     * @return Function項目
     */
    public static ISelectItem manufacture(final Expression expression, final List<Object> jdbcValues) {
        //戻り値
        ISelectItem retValue = null;
        
        if (expression instanceof Column) {
            retValue = manufacture((Column) expression);
        } else if (expression instanceof Function) {
            retValue = manufacture((Function) expression, jdbcValues);
        } else if (expression instanceof StringValue) {
            retValue = manufacture((StringValue) expression);
        } else if (expression instanceof LongValue) {
            retValue = manufacture((LongValue) expression);
        } else if (expression instanceof JdbcParameter) {
            retValue = manufacture((JdbcParameter) expression, jdbcValues);
        }
        
        return retValue;
    }
}
