package org.rej;

public class ProfilePatternCompile {
    public static void main(String[] args) {
        byte[] bs = new byte[]{'a'};
        byte[] bs2 = new byte[]{' ','a'};
        Pattern p = Pattern.compile(bs);
        Registers regs = new Registers();
        for(int j=0;j<10;j++) {
            long before = System.currentTimeMillis();
            for(int i = 0; i < 4000000; i++) {
                p.search(bs2,0,2,0,2,regs);
            }
            long time = System.currentTimeMillis() - before;
            System.err.println(":  " + time + "ms");
        }
    }
}
