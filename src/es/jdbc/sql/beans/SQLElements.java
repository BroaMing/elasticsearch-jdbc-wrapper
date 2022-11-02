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
package es.jdbc.sql.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchRequest;

import es.jdbc.sql.SQLInfo;
import es.jdbc.sql.search.functions.IFunction;

public class SQLElements implements Serializable {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 2357839600797684524L;
    
    /**
     * SQL info.
     */
    private SQLInfo sqlInfo;
    /**
     * Select Item.
     */
    private List<String> sources;
    /**
     * Select Function.
     */
    private Map<String, IFunction> functions;
    /**
     * Elasticsearch Request.
     */
    private SearchRequest request;
    
    public SQLInfo getSqlInfo() {
        return sqlInfo;
    }
    public void setSqlInfo(SQLInfo sqlInfo) {
        this.sqlInfo = sqlInfo;
    }
    public List<String> getSources() {
        return sources;
    }
    public void setSources(List<String> sources) {
        this.sources = sources;
    }
    public void addSource(String source) {
        if (sources == null) {
            sources = new ArrayList<String>();
        }
        sources.add(source);
    }
    public Map<String, IFunction> getFunctions() {
        return functions;
    }
    public void setFunctions(Map<String, IFunction> functions) {
        this.functions = functions;
    }
    public void addFunction(String key, IFunction value) {
        if (functions == null) {
            functions = new LinkedHashMap<String, IFunction>();
        }
        functions.put(key, value);
    }
    public SearchRequest getRequest() {
        return request;
    }
    public void setRequest(SearchRequest request) {
        this.request = request;
    }
}
