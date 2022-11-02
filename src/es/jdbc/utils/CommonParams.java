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

import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;

public class CommonParams {

    private CommonParams() { }
    
    /**
     * Record  Count.
     */
    public static final int RECORD_COUNT_SIZE_PER_SELECT = 10000;
    /**
     * Scroll検索時データ保持時間.
     */
    public static final int SEARCH_TIMEOUT_TIME_SECOND = 10;
    
    /** ハッシュキー正規表現. */
    public static final String AGGREGATION_KEYS_HASH_KEY_REGEX = "(.*?)\\((.*?)\\)";
    /** ハッシュキー正規表現パターン. */
    public static final Pattern AGGREGATION_KEYS_HASH_KEY_PATTERN = Pattern.compile(AGGREGATION_KEYS_HASH_KEY_REGEX);
    
    /**
     * Define Search Date Parser.
     */
    public static final String DATE_FORMAT_YYYYMMDD_HHMMSS = "yyyyMMdd HHmmss";
    public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat(DATE_FORMAT_YYYYMMDD_HHMMSS);
    public static final DateTimeFormatter ELASTICSEARCH_DATE_FORMATTER_NORMAL;
    static {
        DateTimeFormatterBuilder dateFormatBuilder = new DateTimeFormatterBuilder();
        DateTimeParser notmalParser = DateTimeFormat.forPattern(DATE_FORMAT_YYYYMMDD_HHMMSS).withZone(DateTimeZone.forID("Asia/Tokyo")).getParser();
        dateFormatBuilder.append(notmalParser);
        ELASTICSEARCH_DATE_FORMATTER_NORMAL = dateFormatBuilder.toFormatter();
    }
    
    /**
     * Define Result Date Parser.
     */
    private static final String ELASTIC_DATE_FORMAT_STRICT_DATE_OPTIONAL_TIME = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final DateTimeFormatter ELASTICSEARCH_DATE_FORMATTER_ELASTIC;
    static {
        DateTimeFormatterBuilder dateFormatBuilder = new DateTimeFormatterBuilder();
        DateTimeParser elasticParser = 
                DateTimeFormat.forPattern(ELASTIC_DATE_FORMAT_STRICT_DATE_OPTIONAL_TIME).withZone(DateTimeZone.forID("Asia/Tokyo")).getParser();
        dateFormatBuilder.append(elasticParser);
        ELASTICSEARCH_DATE_FORMATTER_ELASTIC = dateFormatBuilder.toFormatter();
    }
    
    /**
     * Boolクエリタイプ.
     */
    public static enum BOOL_QUERY_TYPE{
        MUST,
        MUST_NOT,
        SHOULD
    }
    
    /**
     * Aggregationキー情報.
     */
    public static enum ELASTICSEARCH_AGGREGATION_KEYS {
        /** key. */
        KEY("KEY"),
        /** value. */
        VALUE("VALUE"),
        /** type. */
        TYPE("[_TYPE]"),
        /** サブAggregations. */
        SUB_AGGS("SUB_AGGS"),
        /** 該当レベルの種類（KEY）. */
        LEVEL_CATEGORY("SYS_CATEGORY"),
        /** 該当レベルの種類:arregation. */
        LEVEL_CATEGORY_AGGREGATION("AGGREGATION"),
        /** 該当レベルの種類:arregation. */
        LEVEL_CATEGORY_INNER_AGGREGATION("INNER_AGGREGATION"),
        /** 該当レベルの種類:agregation値. */
        LEVEL_CATEGORY_AGGREGATION_VALUE("AGGREGATION_VALUE"),
        /** カラムタイプ：aggregation. */
        COLUMN_TYPE_AGGS("[_AGGS]"),
        /** カラムタイプ：aggregation. */
        HASH_KEY_FORMAT("%1$s(%2$s)");
        
        private final String key;
        
        ELASTICSEARCH_AGGREGATION_KEYS(final String key) {
            this.key = key;
        }

        /**
         * @return the typeName
         */
        public String getKey() {
            return key;
        }
    }
    ///**
    // * Elasticsearch定数.
    // */
    //public static enum ELASTICSEARCH_CONST {
    //    /**
    //     * TYPE.
    //     */
    //    TYPE("_type");
    //    /**
    //     * 定数名称.
    //     */
    //    private final String constName;
    //    /**
    //     * コンストラクター.
    //     * @param constName
    //     */
    //    ELASTICSEARCH_CONST(final String constName) {
    //        this.constName = constName;
    //    }
    //    /**
    //     * 定数名称を取得.
    //     * @return 定数名称
    //     */
    //    public String getConstName() {
    //        return this.constName;
    //    }
    //}
    
//    /**
//     * Boolクエリタイプ.
//     */
//    public static enum RELATIONSHIP {
//        CHILD,
//        PARENT;
//    }
}
