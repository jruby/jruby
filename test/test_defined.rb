require 'test/unit'

TEST_TOPLEVEL_CONST = true

class TestDefined < Test::Unit::TestCase
  class Sample
    class << self
      def set_the_var(value)
        @@some_var = value
      end
      def the_var_inited?
        defined? @@some_var
      end
    end
  end
  
  def test_defined_class_var
    assert !Sample.the_var_inited?
    Sample.set_the_var('something')
    assert Sample.the_var_inited?
  end
  
  def test_toplevel_const_defined
    assert_equal "constant", defined?(::TEST_TOPLEVEL_CONST)
  end
  
  def test_inner_const_defined
    assert_equal "constant", defined?(TestDefined::Sample)
  end

  def test_number
    assert_equal "expression", DefinedMethods::number
  end
  def test_plus
    assert_equal "method", DefinedMethods::plus
  end
  def test_unexisting_call
    assert_nil DefinedMethods::unexisting_call
  end
  def test_existing_call
    assert_equal "method", DefinedMethods::existing_call
  end
  def test_existing_call2
    assert_equal "method", DefinedMethods::existing_call2
  end
  def test_existing_call3
    assert_equal "method", DefinedMethods::existing_call3
  end
  def test_existing_call4
    assert_equal "method", DefinedMethods::existing_call4
  end
  def test_unexisting_var
    assert_nil DefinedMethods::unexisting_var
  end
  def test_attr_assignment
    assert_equal "assignment", DefinedMethods::attr_assignment
  end
  def test_assignment
    assert_equal "assignment", DefinedMethods::assignment
  end
  def test_unexisting_const_method
    assert_nil DefinedMethods::unexisting_const_method
  end
  def test_unexisting_const
    assert_nil DefinedMethods::unexisting_const
  end
  def test_existing_const
    assert_equal "constant", DefinedMethods::existing_const
  end
  def test_existing_var
    assert_equal "local-variable", DefinedMethods::existing_var
  end
  def test_unexisting_global
    assert_nil DefinedMethods::unexisting_global
  end
  def test_existing_global
    assert_equal "global-variable", DefinedMethods::existing_global
  end
  def test_unexisting_member
    assert_nil DefinedMethods::unexisting_member
  end
  def test_existing_member
    assert_equal "instance-variable", DefinedMethods::existing_member
  end
  def test_unexisting_class_var
    assert_nil DefinedMethods::unexisting_class_var
  end
  def test_existing_class_var
    assert_equal "class variable", DefinedMethods::existing_class_var
  end
  def test_unexisting_zsuper
    assert_nil DefinedMethods::unexisting_zsuper
  end
  def test_unexisting_super
    assert_nil DefinedMethods::unexisting_super
  end
  def test_unexisting_super_with_args
    assert_nil DefinedMethods::unexisting_super_with_args
  end
  def test_existing_super
    assert_equal %w(super)*3, DefinedMethods::test_existing_super
  end
  def test_existing_colon3
    assert_equal "constant", DefinedMethods::existing_colon3
  end
  def test_unexisting_colon3
    assert_nil DefinedMethods::unexisting_colon3
  end
  def test_class_var_assign
    assert_equal "assignment", DefinedMethods::class_var_assign
  end
  def test_unexisting_block_local_variable
    assert_nil DefinedMethods::unexisting_block_local_variable
  end
  def test_existing_block_local_variable
    if RUBY_VERSION =~ /1\.9/
      assert_equal "local-variable", DefinedMethods::existing_block_local_variable
    else
      assert_equal "local-variable(in-block)", DefinedMethods::existing_block_local_variable
    end
  end
  def test_global_assign
    assert_equal "assignment", DefinedMethods::global_assign
  end
  def test_unexisting_dollar_special
    assert_nil DefinedMethods::unexisting_dollar_special
  end
  def test_unexisting_dollar_number
    assert_nil DefinedMethods::unexisting_dollar_number
  end
  def test_unexisting_dollar_number2
    assert_nil DefinedMethods::unexisting_dollar_number2
  end
  def test_existing_dollar_special
    if RUBY_VERSION =~ /1\.9/
      assert_equal "global-variable",DefinedMethods::existing_dollar_special
    else
      assert_equal "$`",DefinedMethods::existing_dollar_special
    end
  end
  def test_existing_dollar_number
    if RUBY_VERSION =~ /1\.9/
      assert_equal "global-variable",DefinedMethods::existing_dollar_special
    else
      assert_equal "$1",DefinedMethods::existing_dollar_number
    end
  end
  def test_true
    assert_equal "true", DefinedMethods::test_true
  end
  def test_false
    assert_equal "false", DefinedMethods::test_false
  end
  def test_self
    assert_equal "self", DefinedMethods::test_self
  end
  def test_nil
    assert_equal "nil", DefinedMethods::test_nil
  end
  def test_yield_without_block
    assert_nil DefinedMethods::test_yield
  end
  def test_yield_with_block
    assert_equal "yield", DefinedMethods::test_yield { }
  end
  def test_match
    assert_equal "method", DefinedMethods::test_match
  end
  
  
  # These are needed because JRuby still can't compile
  # asserts.
  module DefinedMethods
    class <<self
      def foo
        nil
      end
      def foo=(val)
        val
      end
      def number
        defined?(1)
      end
      def plus
        defined?(1+1)
      end
      def unexisting_call
        defined?(gegege())
      end
      def existing_call
        defined?(number)
      end
      def existing_call2
        defined?(number())
      end
      def existing_call3
        defined?(self.number())
      end
      def existing_call4
        defined?(self.number)
      end
      def unexisting_var
        defined?(gegege)
      end
      def attr_assignment
        defined?(foo = 1+1)
      end
      def assignment
        defined?(foo2 = 1+1)
      end
      def unexisting_const_method
        defined?(A.bar("haha"))
      end
      def unexisting_const
        defined?(A)
      end
      module B;end
      def existing_const
        defined?(B)
      end
      def existing_var
        v = 1
        defined?(v)
      end
      def unexisting_global
        defined?($UNEXISTING_GLOBAL)
      end
      def existing_global
        defined?($stderr)
      end
      def unexisting_member
        defined?(@unexisting)
      end
      def existing_member
        @foo1 = 1
        defined?(@foo1)
      end
      def unexisting_class_var
        defined?(@@unexisting)
      end
      def existing_class_var
        @@foo1 = 1
        defined?(@@foo1)
      end
      def unexisting_zsuper
        defined?(super)
      end
      def unexisting_super
        defined?(super())
      end
      def unexisting_super_with_args
        defined?(super(1,2,3))
      end
      class TestExistingSuper
        def initialize(*args)
          @val = [defined?(super), defined?(super()), defined?(super(1,2,3))]
        end
        def val; @val; end
      end
      def test_existing_super
        TestExistingSuper.new.val
      end
      def existing_colon3
        defined?(File::Stat)
      end
      def unexisting_colon3
        defined?(File::State)
      end
      def class_var_assign
        defined?(@@a = 123)
      end
      def unexisting_block_local_variable
        proc{ defined?(a) }.call
      end
      def existing_block_local_variable
        proc{ aaa=1; defined?(aaa) }.call
      end
      def global_assign
        defined?($global_assign = 1)
      end
      def unexisting_dollar_special
        defined?($`)
      end
      def unexisting_dollar_number
        defined?($1)
      end
      def unexisting_dollar_number2
        /a/=~"a"
        defined?($1)
      end
      def existing_dollar_special
        /a/=~"a"
        defined?($`)
      end
      def existing_dollar_number
        /(a)/=~"a"
        defined?($1)
      end
      def test_true
        defined?(true)
      end
      def test_false
        defined?(false)
      end
      def test_self
        defined?(self)
      end
      def test_nil
        defined?(nil)
      end
      def test_yield
        defined?(yield)
      end
      def test_match
        defined?(/a/=~"a")
      end
    end
  end
end
