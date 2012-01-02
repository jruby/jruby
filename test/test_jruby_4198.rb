require 'test/unit'
require 'java'
require File.join(File.dirname(__FILE__), 'jruby-4198.jar')

class TestJruby4198 < Test::Unit::TestCase
  def test_jruby_4198_a
    java_import 'A'
    a = A.new
    
    assert_equal 'private', a.private
    assert_equal 'protected', a.protected
    assert_equal 'public', a.public
  end
  
  def test_jruby_4198_b
    java_import 'B'
    
    assert_equal 'static private', B.private
    assert_equal 'static protected', B.protected
    assert_equal 'static public', B.public
  end
  
end

__END__

jruby-4198.jar contains two class files, A.class and B.class

public class A {
    public String getPublic() {
        return "public";
    }
    public String getPublic1() {
        return "public";
    }
    public String getPrivate() {
        return "private";
    }
    public String getPrivate1() {
        return "private";
    }
    public String getProtected() {
        return "protected";
    }
    public String getProtected1() {
        return "protected";
    }
}


public class B {
    public static String getPublic() {
        return "static public";
    }
    public static String getPublic1() {
        return "static public";
    }
    public static String getPrivate() {
        return "static private";
    }
    public static String getPrivate1() {
        return "static private";
    }
    public static String getProtected() {
        return "static protected";
    }
    public static String getProtected1() {
        return "static protected";
    }
    
}
