package org.rej.bench;

import org.rej.Registers;
import org.rej.Pattern;

public abstract class AbstractBench {
    protected void bench(String _reg, String _str, int warmup, int times) throws Exception {
        byte[] reg = _reg.getBytes();
        byte[] str = _str.getBytes();
        
        Pattern p = Pattern.compile(reg);

        System.err.println("::: /" + _reg + "/ =~ \"" + _str + "\", " + warmup + " * " + times + " times");

        for(int j=0;j<warmup;j++) {
            long before = System.currentTimeMillis();
            for(int i = 0; i < times; i++) {
                p.search(str,0,str.length,0,str.length,new Registers());
            }
            long time = System.currentTimeMillis() - before;
            System.err.println(":  " + time + "ms");
        }
    }

    protected void benchBestOf(String _reg, String _str, int warmup, int times) throws Exception {
        byte[] reg = _reg.getBytes();
        byte[] str = _str.getBytes();
        
        Pattern p = Pattern.compile(reg);

        System.err.println("::: /" + _reg + "/ =~ \"" + _str + "\", " + warmup + " * " + times + " times");

        long best = Long.MAX_VALUE;

        for(int j=0;j<warmup;j++) {
            long before = System.currentTimeMillis();
            for(int i = 0; i < times; i++) {
                p.search(str,0,str.length,0,str.length,new Registers());
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
