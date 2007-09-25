require 'test/unit'

class TestMethods < Test::Unit::TestCase
  class A
    undef_method :id
    (class<<self;self;end).send(:undef_method,:id)
  end
  Adup = A.dup

  def test_undef_id
    assert_raise(NoMethodError) { A.id }
    assert_raise(NoMethodError) { A.new.id }
    assert_raise(NoMethodError) { Adup.id }
    assert_raise(NoMethodError) { Adup.new.id }
  end
  
  class Foo
    private
    def foo; end
  end
  
  def test_foo
    assert_raise(NoMethodError) {Foo.class_eval "new.foo"}
    begin
      Foo.class_eval "new.foo"
    rescue NoMethodError
      $!.to_s =~ /private/
    end
  end
end

class TestCaching < Test::Unit::TestCase
  module Foo
    def the_method
      $THE_METHOD = 'Foo'
    end
  end

  def setup
    $a = Class.new do 
      def the_method
        $THE_METHOD = 'A'
      end
    end.new
  end
  
  def test_extend
    40.times do 
      $a.the_method
      assert_equal "A", $THE_METHOD
    end

    $a.extend Foo

    40.times do 
      $a.the_method
      assert_equal "Foo", $THE_METHOD
    end
  end

  def test_alias
    40.times do 
      $a.the_method
      assert_equal "A", $THE_METHOD
    end

    $a.class.class_eval do 
      def the_bar_method
        $THE_METHOD = "Bar"
      end

      alias_method :the_method, :the_bar_method
    end
    
    $a.the_method
    assert_equal "Bar", $THE_METHOD
  end
end
