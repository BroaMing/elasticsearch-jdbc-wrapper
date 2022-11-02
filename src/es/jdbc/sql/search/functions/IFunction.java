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

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregation;

import es.jdbc.sql.search.ISelectItem;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;

public interface IFunction extends ISelectItem {
    /** 関数リスト. */
    public static enum FUNCTION_LIST {
        COUNT("COUNT") {
            @Override
            public IFunction newInstance() {
                return new Count();
            }
        },
        SUM("SUM") {
            @Override
            public IFunction newInstance() {
                return new Sum();
            }
        },
        MAX("MAX") {
            @Override
            public IFunction newInstance() {
                return new Max();
            }
        },
        MIN("MIN") {
            @Override
            public IFunction newInstance() {
                return new Min();
            }
        },
        COUNT_ALL("COUNT_ALL") {
            @Override
            public IFunction newInstance() {
                return new CountAll();
            }
        },
        FILTER_TERMS("FILTER_TERMS") {
            @Override
            public IFunction newInstance() {
                return new FilterTerms();
            }
        },
        NESTED("NESTED") {
            @Override
            public IFunction newInstance() {
                return new Nested();
            }
        },
        CONTAINS("CONTAINS") {
            @Override
            public IFunction newInstance() {
                return new Contains();
            }
        },
        OVER_PARTITION("OVER_PARTITION") {
            @Override
            public IFunction newInstance() {
                return new OverPartition();
            }
        },
        APPEND_PARTITION("APPEND_PARTITION") {
            @Override
            public IFunction newInstance() {
                return new AppendPartition();
            }
        },
        FIRST_VALUE("FIRST_VALUE") {
            @Override
            public IFunction newInstance() {
                return new FirstValue();
            }
        },
        INNER_SIZE("INNER_SIZE") {
            @Override
            public IFunction newInstance() {
                return new InnerSize();
            }
        },
        INNER_ORDER("INNER_ORDER") {
            @Override
            public IFunction newInstance() {
                return new InnerOrder();
            }
        },
        ORDER_ITEM("ORDER_ITEM") {
            @Override
            public IFunction newInstance() {
                return new OrderItem();
            }
        },
        ALIAS("ALIAS") {
            @Override
            public IFunction newInstance() {
                return new Alias();
            }
        },
        CONTAINS_MATCH("CONTAINS_MATCH") {
            @Override
            public IFunction newInstance() {
                return new ContainsMatch();
            }
        };
        
        private final String name;
        public abstract IFunction newInstance();
        
        private FUNCTION_LIST(final String name) {
            this.name = name;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }
        /**
         * 関数を取得.
         * @param func SQL関数
         * @return 関数
         */
        public static IFunction findFunction(Function func) {
            IFunction retValue = null;
            if (func == null) {
                return retValue;
            }
            for (FUNCTION_LIST value : values()) {
                if (value.getName().equalsIgnoreCase(func.getName())) {
                    retValue = value.newInstance();
                }
            }
            return retValue;
        }
    }
    
    /**
     * 関数パラメーターリスト取得.
     * @return 関数パラメーターリスト
     */
    public List<ISelectItem> getParameters();
    /**
     * 関数パラメーターを追加.
     * @param param 関数パラメーター
     */
    public void addParameter(ISelectItem param);
    /**
     * 関数パラメーターを追加.
     * @param expression 関数パラメーター
     */
    public void addParameter(Expression expression, List<Object> jdbcValues);
    /**
     * 関数パラメーターをセット.
     * @param jdbcValues
     * @param expressions 
     */
    public void setParameters(ExpressionList expressions, List<Object> jdbcValues);
    /**
     * 関数パラメーターリストをゲット.
     * @return 関数パラメーターリスト
     */
    public List<ISelectItem> getParameterList();
    /**
     * Query情報作成.
     * @return Query情報
     */
    public QueryBuilder buildQuery();
    /**
     * 点数Query情報であるか.
     * @return true:点数Queryである false：点数Queryでない
     */
    public boolean isScoreQuery();
    /**
     * サブQueryコンテナー情報作成.
     * @return サブQueryコンテナー
     */
    public BoolQueryBuilder subQueryContainer();
    /**
     * Aggregation情報作成.
     * @return Aggregation情報
     */
    public AbstractAggregationBuilder<?> buildAggregation();
    /**
     * BaseAggregation情報作成.
     * @return BaseAggregation情報
     */
    public AbstractAggregationBuilder<?> findBaseAggregation();
    /**
     * Aggregation結果をコンテナーへ填充する.
     * @param aggsContainer コンテナー(戻り値)
     * @param aggsList Aggregation情報
     */
    //public void fillResult(List<Map<String, Object>> aggsContainer, List<Aggregation> aggsList) throws Exception;
    public void fillResult(List<Map<String, Object>> aggsContainer, List<Aggregation> aggsList);
    /**
     * 全体統計であるか.
     * @return true:全体統計である false:全体統計でない
     */
    public boolean isTotalAggregation();
    /**
     * Nested存在状態をセットする.
     * @param isNestedExists Nested存在するか
     */
    public void setNestedExists(boolean isNestedExists);
    /**
     * Nested存在状態をゲットする.
     * @return Nested存在状態
     */
    public boolean isNestedParamExists();
    /**
     * 既存SQL Node使用であるか.
     * @return true:使用である false:使用でない
     */
    public boolean isSqlNodeUsed();
}
