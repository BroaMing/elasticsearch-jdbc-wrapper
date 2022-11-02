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
package es.jdbc.utils;

import java.util.Map;

import org.elasticsearch.client.RestHighLevelClient;

public class VariablesFactory {

    /**
     * Elasticsearch Client.
     */
    private static final ThreadLocal<RestHighLevelClient> ES_CLIENT_THREAD_LOCAL = new ThreadLocal<RestHighLevelClient>();
    /**
     * SQL Nodesストア.
     */
    private static final ThreadLocal<Map<?, ?>> SQL_NODES_THREAD_LOCAL = new ThreadLocal<Map<?, ?>>();
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static enum VARIABLES_STORE implements AutoCloseable {
        /**
         * Elasticsearch Client.
         */
        ES_CLIENT_STORE(ES_CLIENT_THREAD_LOCAL, RestHighLevelClient.class) {
            @Override
            public void close() throws Exception {
                ES_CLIENT_THREAD_LOCAL.remove();
            }
        },
        /**
         * SQL Nodesストア.
         */
        SQL_NODES_STORE(SQL_NODES_THREAD_LOCAL, Map.class) {
            @Override
            public void close() throws Exception {
                SQL_NODES_THREAD_LOCAL.remove();
            }
        };
        
        private final ThreadLocal variablesStore;
        private final Class<?> variablesType;
        
        /**
         * コンストラクター.
         * @param codeValue コード
         */
        private VARIABLES_STORE(final ThreadLocal variablesStore, final Class<?> variablesType) {
            this.variablesStore = variablesStore;
            this.variablesType = variablesType;
        }
        /**
         * ストア値をセット.
         * @param value 値
         */
        public void set(final Object value) {
            variablesStore.set(value);
        }
        /**
         * ストア値を取得.
         * @param type データ型
         * @return ストア値
         */
        public <T> T get(Class<T> type) {
            Object value = variablesStore.get();
            if (value == null) {
                return null;
            } else {
                return (T) value;
            }
        }
        /**
         * ストア内値データ型を取得.
         * @return ストア内値データ型
         */
        public Class<?> getVariablesType() {
            return this.variablesType;
        }
        /**
         * 全ストアをクリアする.
         * ★★重要:使用後必ずこのメソッドを呼び出す★★
         */
        public static void clearStore() {
            for (VARIABLES_STORE store : VARIABLES_STORE.values()) {
                try {
                    store.close();
                } catch(Exception e) { }
            }
        }
    }
}
