require 'java'

include_class 'org.jruby.test.Worker'

class TestJavaExtension < Test::Unit::TestCase
  class TestParent < org.jruby.test.Parent
    attr_accessor :result
    def run(a)
      @result = "TEST PARENT: #{a}"
    end
  end

  def test_overriding_method
    w = Worker.new
    p = TestParent.new
    w.run_parent(p)
    assert_equal "TEST PARENT: WORKER", p.result
  end
end
