package org.rej;

public class BenchSeveralRegexps {
    public static void main(String[] args) throws Exception {
        byte[] reg = new byte[]{'a'};
        byte[] str = new byte[]{' ','a'};

        Pattern p = Pattern.compile(reg);

        int BASE = 1000000;

        System.err.println("Simple pattern: /a/ =~ \" a\", 10*4,000,000 times");
        for(int j=0;j<10;j++) {
            long before = System.currentTimeMillis();
            for(int i = 0; i < 4*BASE; i++) {
                p.search(str,0,str.length,0,str.length,new Registers());
            }
            long time = System.currentTimeMillis() - before;
            System.err.println(":  " + time + "ms");
        }

        reg = ".*?=".getBytes();
        str = "_petstore_session_id=1b341ffe23b5298676d535fcabd3d0d7; path=/".getBytes();

        p = Pattern.compile(reg);

        System.err.println("Simple backtracking non-greedy pattern: /.*?=/ =~ \"_petstore_session_id=1b341ffe23b5298676d535fcabd3d0d7; path=/\", 10*1,000,000 times");
        for(int j=0;j<10;j++) {
            long before = System.currentTimeMillis();
            for(int i = 0; i < BASE; i++) {
                p.search(str,0,str.length,0,str.length,new Registers());
            }
            long time = System.currentTimeMillis() - before;
            System.err.println(":  " + time + "ms");
        }

        reg = "^(.*?)=(.*?);".getBytes();
        str = "_petstore_session_id=1b341ffe23b5298676d535fcabd3d0d7; path=/".getBytes();

        p = Pattern.compile(reg);

        System.err.println("Complex backtracking non-greedy pattern: /^(.*?)=(.*?);/ =~ \"_petstore_session_id=1b341ffe23b5298676d535fcabd3d0d7; path=/\", 10*1,000,000 times");
        for(int j=0;j<10;j++) {
            long before = System.currentTimeMillis();
            for(int i = 0; i < BASE; i++) {
                p.search(str,0,str.length,0,str.length,new Registers());
            }
            long time = System.currentTimeMillis() - before;
            System.err.println(":  " + time + "ms");
        }

        reg = ".*_p".getBytes();
        str = "_petstore_session_id=1b341ffe23b5298676d535fcabd3d0d7; path=/".getBytes();

        p = Pattern.compile(reg);

        System.err.println("Simple backtracking greedy pattern: /.*_p/ =~ \"_petstore_session_id=1b341ffe23b5298676d535fcabd3d0d7; path=/\", 10*4,000,000 times");
        for(int j=0;j<10;j++) {
            long before = System.currentTimeMillis();
            for(int i = 0; i < 4*BASE; i++) {
                p.search(str,0,str.length,0,str.length,new Registers());
            }
            long time = System.currentTimeMillis() - before;
            System.err.println(":  " + time + "ms");
        }

        reg = ".*=".getBytes();
        str = "_petstore_session_id=1b341ffe23b5298676d535fcabd3d0d7; path=/".getBytes();

        p = Pattern.compile(reg);

        System.err.println("Another backtracking greedy pattern: /.*=/ =~ \"_petstore_session_id=1b341ffe23b5298676d535fcabd3d0d7; path=/\", 10*4,000,000 times");
        for(int j=0;j<10;j++) {
            long before = System.currentTimeMillis();
            for(int i = 0; i < 4*BASE; i++) {
                p.search(str,0,str.length,0,str.length,new Registers());
            }
            long time = System.currentTimeMillis() - before;
            System.err.println(":  " + time + "ms");
        }
    }
}
