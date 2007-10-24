package org.rej;

public class ProfilePatternCompile3 {
    public static void main(String[] args) throws Exception {
        byte[] reg = ".*?=".getBytes();
        byte[] str = "_petstore_session_id=1b341ffe23b5298676d535fcabd3d0d7; path=/".getBytes();
        Pattern p = Pattern.compile(reg);
        int times1 = 10;
        int times2 = 1000000;

        if(args.length > 0) {
            times1 = Integer.parseInt(args[0]);
            times2 = Integer.parseInt(args[1]);
        }

        for(int j=0;j<times1;j++) {
            long before = System.currentTimeMillis();
            for(int i = 0; i < times2; i++) {
                p.search(str,0,str.length,0,str.length,new Registers());
            }
            long time = System.currentTimeMillis() - before;
            System.err.println(":  " + time + "ms");
        }
    }
}
