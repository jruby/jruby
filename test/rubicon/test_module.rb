require 'test/unit'

$m0 = Module.nesting


class TestModule < Test::Unit::TestCase
  IS19 = RUBY_VERSION =~ /1\.9/

  #
  # Check that two arrays contain the same "bag" of elements.
  # A mathematical bag differs from a "set" by counting the
  # occurences of each element. So as a bag [1,2,1] differs from
  # [2,1] (but is equal to [1,1,2]).
  #
  # The method only relies on the == operator to match objects
  # from the two arrays. The elements of the arrays may contain
  # objects that are not "Comparable".
  #
  # FIXME: This should be moved to common location.
  def assert_bag_equal(expected, actual)
    # For each object in "actual" we remove an equal object
    # from "expected". If we can match objects pairwise from the
    # two arrays we have two equal "bags". The method Array#index
    # uses == internally. We operate on a copy of "expected" to
    # avoid destructively changing the argument.
    #
    expected_left = expected.dup
    actual.each do |x|
      if j = expected_left.index(x)
        expected_left.slice!(j)
      end
    end
    assert( expected.length == actual.length && expected_left.length == 0,
           "Expected: #{expected.inspect}, Actual: #{actual.inspect}")
  end

  # Support stuff

  module Mixin
    MIXIN = 1
    def mixin
    end
  end

  module User
    USER = 2
    include Mixin
    def user
    end
  end

  module Other
    def other
    end
  end

  module Unrelated1
    def unrelated1_method
    end
  end

  module Unrelated2
    def unrelated2_method
    end
  end

  class AClass
    def AClass.cm1
      "cm1"
    end
    def AClass.cm2
      cm1 + "cm2" + cm3
    end
    def AClass.cm3
      "cm3"
    end
    
    private_class_method :cm1, "cm3"

    def aClass
    end

    def aClass1
    end

    def aClass2
    end

    private :aClass1
    protected :aClass2
  end

  class BClass < AClass
    def bClass1
    end

    private

    def bClass2
    end

    protected
    def bClass3
    end
  end

  MyClass = AClass.clone
  class MyClass
    public_class_method :cm1
  end

  # -----------------------------------------------------------

  def test_CMP # '<=>'
    assert_equal( 0, Mixin <=> Mixin)
    assert_equal(-1, User <=> Mixin)
    assert_equal( 1, Mixin <=> User)

    assert_equal( 0, Object <=> Object)
    assert_equal(-1, String <=> Object)
    assert_equal( 1, Object <=> String)

    # unrelated modules give 'nil'
    assert_equal(nil,  Unrelated1 <=> Unrelated2)
    assert_equal(nil,  Unrelated2 <=> Unrelated1)
  end

  def test_GE # '>='
    assert_equal(true,  Mixin >= User)
    assert_equal(true,  Mixin >= Mixin)
    assert_equal(false, User >= Mixin)

    assert_equal(true,  Object >= String)
    assert_equal(true,  String >= String)
    assert_equal(false, String >= Object)

    # unrelated modules give 'nil'
    assert_equal(nil,  Unrelated1 >= Unrelated2)
    assert_equal(nil,  Unrelated2 >= Unrelated1)
  end

  def test_GT # '>'
    assert_equal(true,   Mixin > User)
    assert_equal(false,  Mixin > Mixin)
    assert_equal(false,  User  > Mixin)

    assert_equal(true,   Object > String)
    assert_equal(false,  String > String)
    assert_equal(false,  String > Object)

    # unrelated modules give 'nil'
    assert_equal(nil,  Unrelated1 > Unrelated2)
    assert_equal(nil,  Unrelated2 > Unrelated1)
  end

  def test_LE # '<='
    assert_equal(true,  User <= Mixin)
    assert_equal(true,  Mixin <= Mixin)
    assert_equal(false,  Mixin <= User)

    assert_equal(true,  String <= Object)
    assert_equal(true,  String <= String)
    assert_equal(false, Object <= String)

    # unrelated modules give 'nil'
    assert_equal(nil,  Unrelated1 <= Unrelated2)
    assert_equal(nil,  Unrelated2 <= Unrelated1)
  end

  def test_LT # '<'
    assert_equal(true,   User  < Mixin)
    assert_equal(false,  Mixin < Mixin)
    assert_equal(false,  Mixin < User)

    assert_equal(true,   String < Object)
    assert_equal(false,  String < String)
    assert_equal(false,  Object < String)

    # unrelated modules give 'nil'
    assert_equal(nil,  Unrelated1 < Unrelated2)
    assert_equal(nil,  Unrelated2 < Unrelated1)
  end

  def test_EQUAL # '=='
    assert_equal(true,   User  == User)
    assert_equal(false,  User  == Mixin)
    assert_equal(false,  Mixin == User)
    assert_equal(true,   Mixin == Mixin)

    assert_equal(true,   String == String)
    assert_equal(false,  String == Object)
    assert_equal(false,  Object == String)
    assert_equal(true,   Object == Object)

    # unrelated modules give 'false' too
    assert_equal(true,   Unrelated1 == Unrelated1)
    assert_equal(false,  Unrelated1 == Unrelated2)
    assert_equal(false,  Unrelated2 == Unrelated1)
    assert_equal(true,   Unrelated2 == Unrelated2)
  end

  def test_VERY_EQUAL # '==='
    assert(Object === self)
    assert(Test::Unit::TestCase === self)
    assert(TestModule === self)
    assert(!(String === self))
  end

  def test_ancestors
    assert_equal([User, Mixin],      User.ancestors)
    assert_equal([Mixin],            Mixin.ancestors)

    ancestors_obj = Object.ancestors
    ancestors_str = String.ancestors
    if defined?(PP) # when issuing 'AllTests.rb' then PP gets added
      ancestors_obj.delete(PP::ObjectMixin)
      ancestors_str.delete(PP::ObjectMixin)
    end
    if defined?(Rake::DeprecatedObjectDSL)
      subtract = [Rake::DeprecatedObjectDSL]
    else
      subtract = []
    end
    assert_equal(IS19 ? [Object, Kernel, BasicObject] : [Object, Kernel],   ancestors_obj - subtract)
    if IS19
      assert_equal([String,
                    Comparable,
                    Object, Kernel, BasicObject],  ancestors_str - subtract)
    else
      assert_equal([String,
                    Enumerable,
                    Comparable,
                    Object, Kernel],  ancestors_str - subtract)
    end
  end

  def test_class_eval
    Other.class_eval("CLASS_EVAL = 1")
    assert_equal(1, Other::CLASS_EVAL)
    assert(Other.constants.include?(IS19 ? :CLASS_EVAL : "CLASS_EVAL"))
  end

  def test_const_defined?
    assert(Math.const_defined?(:PI))
    assert(Math.const_defined?("PI"))
    assert(!Math.const_defined?(:IP))
    assert(!Math.const_defined?("IP"))
  end

  def test_const_get
    assert_equal(Math::PI, Math.const_get("PI"))
    assert_equal(Math::PI, Math.const_get(:PI))
  end

  def test_const_set
    assert(!Other.const_defined?(:KOALA))
    Other.const_set(:KOALA, 99)
    assert(Other.const_defined?(:KOALA))
    assert_equal(99, Other::KOALA)
    Other.const_set("WOMBAT", "Hi")
    assert_equal("Hi", Other::WOMBAT)
  end

  def test_constants
    assert_equal(IS19 ? [:MIXIN] : ["MIXIN"], Mixin.constants)
    assert_equal(IS19 ? [:MIXIN, :USER] : ["MIXIN", "USER"], User.constants.sort)
  end

  def test_included_modules
    assert_equal([], Mixin.included_modules)
    assert_equal([Mixin], User.included_modules)
    incmod_obj = Object.included_modules
    incmod_str = String.included_modules
    if defined?(PP)  # when issuing 'AllTests.rb' then PP gets added
      incmod_obj.delete(PP::ObjectMixin)
      incmod_str.delete(PP::ObjectMixin)
    end
    if defined?(Rake::DeprecatedObjectDSL)
      subtract = [Rake::DeprecatedObjectDSL]
    else
      subtract = []
    end
    assert_equal([Kernel], incmod_obj - subtract)
    assert_equal(IS19 ? [Comparable, Kernel] : [Enumerable, Comparable, Kernel], incmod_str - subtract)
  end

  def test_instance_methods
      # default value is true
      ary_user = User.instance_methods
      assert_equal(true, ary_user.include?(IS19 ? :user : "user"))
      # we expect more than our 'user' method to be returned.
      assert_equal(true, ary_user.size > 1) 
      
      # we expect ONLY than our 'mixin' method to be returned.
      ary_mixin = Mixin.instance_methods
      assert_equal(IS19 ? [:mixin] : ["mixin"], ary_mixin)
      
      ary_class = AClass.instance_methods
      assert_equal(true, ary_class.include?(IS19 ? :aClass : "aClass"))
      # we expect more than our 'aClass' method to be returned.
      assert_equal(true, ary_class.size > 1) 
    assert_equal(IS19 ? [:mixin, :user] : ["mixin", "user"], User.instance_methods(true).sort)
    assert_equal(IS19 ? [:mixin] : ["mixin"], Mixin.instance_methods(true))
      assert_bag_equal(IS19 ? [:aClass, :aClass2] : ["aClass", "aClass2"],
                       AClass.instance_methods(true) - 
                       Object.instance_methods(true)
                       )
  end

  def test_method_defined?
    assert(!User.method_defined?(:wombat))
    assert(User.method_defined?(:user))
    assert(User.method_defined?(:mixin))
    assert(!User.method_defined?("wombat"))
    assert(User.method_defined?("user"))
    assert(User.method_defined?("mixin"))
  end

  def test_module_eval
    User.module_eval("MODULE_EVAL = 1")
    assert_equal(1, User::MODULE_EVAL)
    assert(User.constants.include?(IS19 ? :MODULE_EVAL : "MODULE_EVAL"))
    User.instance_eval("remove_const(:MODULE_EVAL)")
    assert(!User.constants.include?("MODULE_EVAL"))
  end

  def test_name
    assert_equal("Fixnum", Fixnum.name)
    assert_equal("TestModule::Mixin",  Mixin.name)
    assert_equal("TestModule::User",   User.name)
  end

  def test_private_class_method
    assert_raise(NoMethodError) { AClass.cm1 }
    assert_raise(NoMethodError) { AClass.cm3 }
    assert_equal("cm1cm2cm3", AClass.cm2)
  end

  def test_private_instance_methods
      # default value is true
      a = AClass.private_instance_methods
      assert_equal(true, a.include?(IS19 ? :aClass1 : "aClass1"))
      # we expect more than our 'aClass1' method to be returned.
      assert_equal(true, a.size > 1) 
      
      b = BClass.private_instance_methods
      assert_equal(true, b.include?(IS19 ? :bClass2 : "bClass2"))
      # we expect more than our 'bClass2' method to be returned.
      assert_equal(true, b.size > 1) 
    assert_bag_equal(IS19 ? [:bClass2, :aClass1] : ["bClass2", "aClass1"],
                     BClass.private_instance_methods(true) -
                     Object.private_instance_methods(true))
  end

  def test_protected_instance_methods
      # default value is true
      a = AClass.protected_instance_methods
      assert_equal(true, a.include?(IS19 ? :aClass2 : "aClass2"))
      # we expect more than our 'aClass2' method to be returned.
      assert_equal(true, a.size == 1) 
      
      b = BClass.protected_instance_methods
      assert_equal(true, b.include?(IS19 ? :bClass3 : "bClass3"))
      # we expect more than our 'bClass3' method to be returned.
      assert_equal(true, b.size > 1) 
    assert_bag_equal(IS19 ? [:bClass3, :aClass2] : ["bClass3", "aClass2"],
                     BClass.protected_instance_methods(true) -
                     Object.protected_instance_methods(true))
  end

  def test_public_class_method
    assert_equal("cm1",       MyClass.cm1)
    assert_equal("cm1cm2cm3", MyClass.cm2)
    assert_raise(NoMethodError) { eval "MyClass.cm3" }
  end

  def test_public_instance_methods
      # default value is true
      a = AClass.public_instance_methods
      assert_equal(true, a.include?(IS19 ? :aClass : "aClass"))
      # we expect more than our 'aClass' method to be returned.
      assert_equal(true, a.size > 1) 
      
      b = BClass.public_instance_methods
      assert_equal(true, b.include?(IS19 ? :bClass1 : "bClass1"))
      # we expect more than our 'bClass1' method to be returned.
      assert_equal(true, b.size > 1) 
  end

  def test_s_constants
    c1 = Module.constants
    Object.module_eval "WALTER = 99"
    c2 = Module.constants
    assert_equal([IS19 ? :WALTER : "WALTER"], c2 - c1)
  end

  module M1
    $m1 = Module.nesting
    module M2
      $m2 = Module.nesting
    end
  end

  def test_s_nesting
    assert_equal([],                               $m0)
    assert_equal([TestModule::M1, TestModule],     $m1)
    assert_equal([TestModule::M1::M2,
                  TestModule::M1, TestModule],     $m2)
  end

  def test_s_new
    m = Module.new
    assert_instance_of(Module, m)
  end

end
