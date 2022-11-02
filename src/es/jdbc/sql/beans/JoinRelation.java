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
import java.util.List;

public class JoinRelation implements Serializable {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = -2541067631380710352L;

    private String name;
    
    private JoinRelation parent;
    
    private List<JoinRelation> children;

    public JoinRelation(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public JoinRelation getParent() {
        return parent;
    }
    public void setParent(JoinRelation parent) {
        if (parent != null) {
            parent.addChild(this);
        } else if (this.parent != null) {
            List<JoinRelation> children = this.parent.getChildren();
            if (children != null) {
                children.remove(this);
            }
            this.parent = parent;
        }
    }
    
    public List<JoinRelation> getChildren() {
        return children;
    }
    public void setChildren(List<JoinRelation> children) {
        if (children != null) {
            for (JoinRelation child : children) {
                child.parent = this;
            }
        }
        this.children = children;
    }
    public void addChild(JoinRelation child) {
        if (children == null) {
            children = new ArrayList<JoinRelation>();
        }
        if (!children.contains(child)) {
            child.parent = this;
            children.add(child);
        }
    }
    public boolean removeChild(JoinRelation child) {
        boolean retValue = false;
        if (children == null) {
            return retValue;
        }
        if (retValue = children.remove(child)) {
            child.setParent(null);
        }
        return retValue;
    }
}
