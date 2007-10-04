require 'test/unit'

class TestVariables < Test::Unit::TestCase

  #
  # TODO: test for new variables code will go here as well.
  # 

  # test fix for JRUBY-1295 (invalid var names permitted in set/get/remove)
  def test_instance_variable_validation
    assert_raises(NameError) { Object.new.instance_variable_set '@123', 1 }
    assert_raises(NameError) { Object.new.instance_variable_set '@$', 1 }
    assert_raises(NameError) { Object.new.instance_variable_set '@ a', 1 }
    assert_raises(NameError) { Object.new.instance_variable_get '@123' }
    assert_raises(NameError) { Object.new.instance_variable_get '@$' }
    assert_raises(NameError) { Object.new.instance_variable_get '@ a' }

    # we'll support this MRI behavior, though it seems like a bug (Pickaxe
    # says it's illegal):
    obj = Object.new
    assert_nothing_raised { obj.instance_variable_set('@', 1) }
    assert_equal('@', obj.instance_variables[0])
    assert_equal(1, obj.instance_variable_get('@'))
  end

  # test fix for JRUBY-1295 (invalid var names permitted in set/get/remove)
  def test_class_variable_validation
    assert_raises(NameError) { Class.new { class_variable_set '@@123', 1 } }
    assert_raises(NameError) { Class.new { class_variable_set '@@$', 1 } }
    assert_raises(NameError) { Class.new { class_variable_set '@@ ', 1 } }
    assert_raises(NameError) { Class.new { class_variable_get '@@123' } }
    assert_raises(NameError) { Class.new { class_variable_get '@@$' } }
    assert_raises(NameError) { Class.new { class_variable_get '@@ ' } }

    # we'll support this MRI behavior, though it seems like a bug (Pickaxe
    # says it's illegal):
    cls = Class.new
    assert_nothing_raised { cls.send :class_variable_set, '@@', 1 }
    assert_equal('@@', cls.class_variables[0] )
    assert_equal(1, cls.send(:class_variable_get, '@@'))
  end

  # test fix for JRUBY-1295 (invalid var names permitted in set/get/remove)
  def test_constant_validation
    assert_raises(NameError) { Class.new.const_set 'C no evil', 1 }
    assert_raises(NameError) { Class.new.const_get "C'est la vie!" }
    assert_raises(NameError) { Class.new { remove_const "Ciao, Marcello!" } }
  end

end
