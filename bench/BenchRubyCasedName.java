public class BenchRubyCasedName {


    public static void main(String[] args) {
        String[] names = new String[] {"fooBarBAZMah", "fooBarBAZMag", "fooBarBAZMaf", "fooBarBAZMad", "fooBarBAZMas"};
        for (int c = 0; c < 100; c++) {
            long t = System.currentTimeMillis();
            for (int i = 0; i < 1000000; i++) {
                String name = org.jruby.javasupport.JavaUtil.getRubyCasedName(names[i % 5]);
                if (i % 100001 == 0) System.out.println(name);
            }
            System.out.println(System.currentTimeMillis() - t);
        }
    }
}
