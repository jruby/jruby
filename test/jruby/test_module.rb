require 'test/unit'
require 'test/jruby/test_helper'

class ModuleTest < Test::Unit::TestCase
  # include TestHelper

  def test_prepend_features_type_error
    assert_raise(TypeError) do
      Module.new.instance_eval { prepend_features(1) }
    end
  end
  
  module XXX
  end
  
  def test_module_name_is_not_mutated
    assert_false XXX.to_s.object_id != XXX.to_s.object_id
    XXX.to_s.downcase!
    assert_equal XXX.to_s, "XXX"
  end

  module M
    C = 'public';  public_constant  :C
    D = 'private'; private_constant :D
  end
  M.freeze

  def test_frozen_raises_on_constant_visibility_change
    assert_raises { M.public_constant :D }
    begin
      M.private_constant :C
    rescue => e
      assert_match /can\'t modify frozen /, e.message
    else fail
    end
    assert_equal [ :C ], M.constants
  end

  class C; end
  C.freeze

  def test_frozen_error_message # matching MRI 2.2 messages
    begin
      M.const_set :SOME, 42
    rescue => e
      assert_equal "can't modify frozen Module", e.message
    else fail
    end
    begin
      C.class_variable_set :@@some, 42
    rescue => e
      assert_equal "can't modify frozen #<Class:ModuleTest::C>", e.message
    else fail
    end
  end

end
