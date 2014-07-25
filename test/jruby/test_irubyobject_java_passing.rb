require 'test/unit'
require 'java'
require 'pp'

java_import 'org.jruby.test.JRUBY_2480_A'
java_import 'org.jruby.test.JRUBY_2480_B'

# JRUBY-2480, uncoercible Ruby objects getting wrapped when passing through Java code
class TestIrubyobjectJavaPassing < Test::Unit::TestCase
  class C
    include JRUBY_2480_B
  
    def foo(o)
      o.color
    end
  end
  
  class Color
    attr_reader :color
  
    def initialize(color)
      @color = color
    end
  end

  def test_passing_irubyobject_through
    a = JRUBY_2480_A.new(C.new)
    result = a.doIt(Color.new("red"))
    assert_equal("red", result)
  end
end
