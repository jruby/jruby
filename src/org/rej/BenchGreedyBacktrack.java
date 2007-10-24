package org.rej;

public class BenchGreedyBacktrack {
    public static void main(String[] args) throws Exception {
        byte[] reg = ".*_p".getBytes();
        byte[] str = "_petstore_session_id=1b341ffe23b5298676d535fcabd3d0d7; path=/".getBytes();

        Pattern p = Pattern.compile(reg);

        for(int j=0;j<10;j++) {
            long before = System.currentTimeMillis();
            for(int i = 0; i < 1000000; i++) {
                p.search(str,0,str.length,0,str.length,new Registers());
            }
            long time = System.currentTimeMillis() - before;
            System.err.println(":  " + time + "ms");
        }
    }
}
