public class BenchJavaPropName {


    public static void main(String[] args) {
        String[] names = new String[] {"getBarBAZMah", "setBarBAZMag", "isBarBAZMaf"};
        for (int c = 0; c < 100; c++) {
            long t = System.currentTimeMillis();
            for (int i = 0; i < 1000000; i++) {
                String name = org.jruby.javasupport.JavaUtil.getJavaPropertyName(names[i % 3]);
                if (i % 100001 == 0) System.out.println(name);
            }
            System.out.println(System.currentTimeMillis() - t);
        }
    }
}
