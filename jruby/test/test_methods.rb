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
