/**
 * Botaoyx.com Inc.
 * Copyright (c) 2021-2023 All Rights Reserved.
 */

import java.util.LinkedHashSet;

/**
 *
 * @author Martin.C
 * @version 2023/06/15 16:20
 */
public class LenOfLongSub {

    public static void main(String[] args) {

        var ss = new String[]{"abcabcbb","bbbbb","pwwkew","abcdcbaddaddcdfrteg"};
        for (var s :ss){
            longSub(s);
        }

    }

    public static void longSub(String s) {


        //var s = "abcdcbaddaddcdfrteg";

        int max = 1;

        assert s.length()>1;
        var lSet = new LinkedHashSet<>();


        lSet.add(s.charAt(0));
        int skip = 0;
        for (int i = 1; i < s.length(); i++) {
            var c = s.charAt(i);
            //var preIndex = s.indexOf(c,skip);
            if(lSet.contains(c)){

                if(lSet.size()>max){
                    max = lSet.size();
                    System.out.println(lSet);
                }

                var itor = lSet.iterator();
                while (itor.hasNext()){
                    var old = itor.next();
                    if(old.equals(c)){
                        itor.remove();
                        break;
                    }
                    itor.remove();
                }

                //fo
                //
                //for (int j = 0; j <= (preIndex-skip); j++) {
                //    if(itor.hasNext()) {
                //        itor.next();
                //        itor.remove();
                //    }
                //}
                //
                //skip = preIndex + 1 ;
            }

            lSet.add(c);

        }
        if(lSet.size()>max){
            max = lSet.size();
            System.out.println(lSet);
        }

        System.out.println(s + " max is :"+max);


    }

    public static void longSubIndex(String s) {


        //var s = "abcdcbaddaddcdfrteg";

        int max = 1;

        assert s.length()>1;
        var lSet = new LinkedHashSet<>();
        var itor = lSet.iterator();

        lSet.add(s.charAt(0));
        int skip = 0;
        for (int i = 1; i < s.length(); i++) {
            var c = s.charAt(i);
            var preIndex = s.indexOf(c,skip);
            if(preIndex != i){

                if(lSet.size()>max){
                    max = lSet.size();
                    System.out.println(lSet);
                }

                //fo

                for (int j = 0; j <= (preIndex-skip); j++) {
                    if(itor.hasNext()) {
                        itor.next();
                        itor.remove();
                    }
                }

                skip = preIndex + 1 ;
            }

            lSet.add(c);

        }
        if(lSet.size()>max){
            max = lSet.size();
            System.out.println(lSet);
        }

        System.out.println(s + " max is :"+max);


    }
}