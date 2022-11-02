/*
 * Copyright (C) 2017 The elasticsearch-sql-converter Authors
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
package test;

import java.util.ArrayList;
import java.util.List;

public class Tester {

    // Complete the moves function below.
    static int moves(int[] a) {
        int ret = 0;
        List<Integer> oddIndexes = new ArrayList<Integer>();
        List<Integer> evenIndexes = new ArrayList<Integer>();
        for (int i1=0, i2=a.length-1; i1<i2; i1++, i2--) {
            if (a[i1]%2 == 1) oddIndexes.add(i1);
            if (a[i2]%2 == 0) evenIndexes.add(i2);
            if (evenIndexes.size() > 0 && oddIndexes.size() > 0) {
                Integer oi1 = oddIndexes.remove(0);
                Integer ei2 = evenIndexes.remove(0);
                int oVal = a[oi1];
                a[oi1] = a[ei2];
                a[ei2] = oVal;
                ret++;
            }
        }
        return ret;
    }
    
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	    int[] val = {5, 8, 5, 11, 4, 6};
	    System.out.println(moves(val));
	}
}
