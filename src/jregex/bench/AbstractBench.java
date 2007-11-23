package jregex.bench;

import jregex.Pattern;

public abstract class AbstractBench {
    protected void bench(String _reg, String _str, int warmup, int times) throws Exception {
        String reg = _reg;
        char[] str = _str.toCharArray();

        Pattern p = new Pattern(reg);

        System.err.println("::: /" + _reg + "/ =~ \"" + _str + "\", " + warmup + " * " + times + " times");

        for(int j=0;j<warmup;j++) {
            long before = System.currentTimeMillis();
            for(int i = 0; i < times; i++) {
                p.matcher(str,0,str.length).find();
            }
            long time = System.currentTimeMillis() - before;
            System.err.println(":  " + time + "ms");
        }
    }

    protected void benchBestOf(String _reg, String _str, int warmup, int times) throws Exception {
        String reg = _reg;
        char[] str = _str.toCharArray();

        Pattern p = new Pattern(reg);

        System.err.println("::: /" + _reg + "/ =~ \"" + _str + "\", " + warmup + " * " + times + " times");

        long best = Long.MAX_VALUE;

        for(int j=0;j<warmup;j++) {
            long before = System.currentTimeMillis();
            for(int i = 0; i < times; i++) {
                p.matcher(str,0,str.length).find();
            }
            long time = System.currentTimeMillis() - before;
            if(time < best) {
                best = time;
            }
            System.err.print(".");
        }
        System.err.println(":  " + best + "ms");
    }
}
