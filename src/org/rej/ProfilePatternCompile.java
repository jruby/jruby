package org.rej;

public class ProfilePatternCompile {
    public static void main(String[] args) {
        byte[] bs = new byte[]{'a','.','*','?','[','b','-','z',']','{','2',',','4','}','a','a','a','a','a','a'};

        for(int j=0;j<10;j++) {
            long before = System.currentTimeMillis();
            for(int i = 0; i < 500000; i++) {
                Pattern.compile(bs);
            }
            long time = System.currentTimeMillis() - before;
            System.err.println(":  " + time + "ms");
        }
    }
}
