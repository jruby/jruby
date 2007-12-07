require 'test/unit'

class TestClass < Test::Unit::TestCase

  class Top
    def Top.inherited(sub)
       $hierarchy << sub
    end
  end

  $hierarchy = [Top]
  class Middle < Top
  end

  class Bottom < Middle
  end

  def test_class_inheritance_for_a_global_variable
    assert_equal([Top, Middle, Bottom] , $hierarchy)
  end

  class AttrTest
    attr :attribute1
    attr_writer :attribute1
  end

  def test_attr_and_attr_writer_behavior
    attrTest = AttrTest.new
    attrTest.attribute1 = 1
    assert_equal(1 , attrTest.attribute1)
  end

  def test_attr_methods_have_optional_arity
    assert_nothing_raised do
      Module.new do
        attr_reader
        attr_writer
        attr_accessor
      end
    end
  end

  class Froboz
    include Enumerable
  end

  def test_class_ancestry_is_correct_when_a_module_is_mixed_in
    f = Froboz.new
    assert_equal([Froboz, Enumerable, Object, Kernel], f.class.ancestors)
    assert(f.kind_of?(Froboz))
    assert(f.kind_of?(Enumerable))
  end

  class CM1; end

  class CM2
    def CM2::const_missing (constant)
      constant.to_s
    end
  end

  def test_missing_constant_raises_name_error
    assert_raise(NameError) {CM1::A}
  end

  def test_const_missing_is_available_for_a_class
    assert_equal(CM2::A, "A")
  end

  class GV1
    def initialize
      @@a = 1;
    end
    def modifyAfterFreeze
      freeze
      @aa = 2;
    end
    def createAfterFreeze
      @@b = 2;
    end
  end

  def test_freeze_halts_execution
    g = GV1.new
    assert_raise(TypeError) { g.modifyAfterFreeze }
    assert_nothing_raised {g = GV1.new}
    g.class.freeze
    assert_raise(TypeError) {g.createAfterFreeze}
  end

  module A
    class Failure
      def Failure.bar()
        print "bar\n"
      end
    end
  end

  module B
    class Failure
      def Failure.foo()
        print "foo\n"
      end
    end
  end

  def test_scope_within_modules
    # minirunit test passed asseting this was a NameError?
    assert_raise(NoMethodError) {B::Failure.bar}
  end

  E = Hash.new
  class << E
    def a() "A" end
  end

  def test_singleton_class_adds_methods_to_an_object_not_superclasses
    assert_equal("A", E.a)
    assert_raise(NoMethodError) { Hash.new.a }
    assert_raise(NoMethodError) { Object.new.a }
  end

  # test singleton method scoping
  class C
    VAR1 = 1

    def C.get_var1
      VAR1
    end

    class << self
      VAR2 = 2

      def other_get_var1
        VAR1
      end

      def get_var2
        VAR2
      end
    end
  end

  def test_singleton_method_scoping
    assert_equal(1, C.get_var1)
    assert_equal(1, C.other_get_var1)
    assert_equal(2, C.get_var2)
  end

  class << C; end

  def test_scoping_of_above_methods_does_not_change_with_new_singleton_class_declaration
    assert_equal(1, C.get_var1)
    assert_equal(1, C.other_get_var1)
    assert_equal(2, C.get_var2)
  end



  def test_singleton_decaration_as_a_block
    a = Class.new do
     def method_missing(name, *args)
       self.class.send(:define_method, name) do |*a|
         "#{name}"
       end
       send(name)
     end
    end
    b = a.new
    assert_equal("foo", b.foo)
  end

  class ClassVarTest
    @@foo = "foo"

    def self.x
      @@bar = "bar"
      @@foo = "foonew"
    end

    def z
      @@baz = "baz"
    end

    def y
      @@foo
    end

    def self.w
      @@foo
    end

    def self.zz
      @@baz
    end
  end

  def test_class_var_declaration
    assert_equal(ClassVarTest.new.y, "foo")
    assert_nothing_raised {ClassVarTest.x }
    assert_raise(NameError) {ClassVarTest.zz }
    assert_nothing_raised {ClassVarTest.new.z }
    assert_equal(ClassVarTest.zz, "baz")
    assert_equal(ClassVarTest.new.y, "foonew")
  end

  def test_class_var_get_and_set
    ##### JRUBY-793 test class var get/set #####
    assert_raise(NameError) { ClassVarTest.send(:class_variable_get, :@foo) }
    assert_raise(NameError) { ClassVarTest.send(:class_variable_set, :@foo, "foodoo") }
    assert_nothing_raised { ClassVarTest.send(:class_variable_get, :@@foo) }
    assert_nothing_raised { ClassVarTest.send(:class_variable_set, :@@foo, "fooset") }
    assert_equal(ClassVarTest.send(:class_variable_get, :@@foo), "fooset")
    assert_equal(ClassVarTest.w, "fooset")
    assert_equal(ClassVarTest.new.y, "fooset")
  end

  # test class variable assignment in a singleton method
  class TestClassVarAssignmentInSingleton
    @@a = nil

    class << self
      def bar
        # test_equal(nil, @@a)
        @@a = 1 unless @@a
        # test_equal(1, @@a)
      end
    end

    def a
      @@a
    end
  end

  def test_class_var_assignment_in_singleton
    test_instance = TestClassVarAssignmentInSingleton.new
    assert_equal nil, test_instance.a
    TestClassVarAssignmentInSingleton.bar
    assert_equal 1, test_instance.a
  end

  # test define_method behavior to be working properly
  $foo_calls = []
  class BaseClass
    def foo
      $foo_calls << BaseClass
    end
  end

  class SubClass < BaseClass
    define_method(:foo) do
      $foo_calls << SubClass
      super
    end
  end

  def test_define_method_behavior
    x = SubClass.new
    assert_nothing_raised { x.foo }
    assert_equal([SubClass, BaseClass], $foo_calls)
  end

  # test constants do not appear in instance variables
  class NoConstantInInstanceVariables
    @@b = 4
    B = 2
  end

  def test_constants_should_not_be_instance_vars
    assert_equal([], NoConstantInInstanceVariables.new.class.instance_variables)
    # this was in the minirunti tests but I cannot see why?
    assert_equal(3, @@a = 3)
  end

  class C7; end

  class C7A; end

  module M7A; end

  def test_eval_class_as_module_should_raise_type_exception
    assert_raise(TypeError) do
      C7A.module_eval { include C7 }
    end

    assert_raise(TypeError) do
      M7A.module_eval { include C7 }
    end
  end

  class Foo
    def self.action(name, &block)
      define_method(name) {
        instance_eval &block
      }
    end

    action(:boo) { baz }

    protected
    def baz
      'here'
    end
  end

  # JRUBY-1381
  def test_define_method_with_instance_eval_has_correct_self
    assert_equal('here', Foo.new.boo)
  end

  class AliasMethodTester
    METHODS = []
    def AliasMethodTester.method_added(name)
      METHODS << name
    end
    alias_method :puts2, :puts
  end

  # JRUBY-1419
  def test_alias_method_calls_method_added
    assert_equal([:puts2], AliasMethodTester::METHODS)
  end
end
