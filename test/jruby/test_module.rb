require 'test/unit'
require 'test/jruby/test_helper'

class ModuleTest < Test::Unit::TestCase
  # include TestHelper

  def test_prepend_features_type_error
    assert_raise(TypeError) do
      Module.new.instance_eval { prepend_features(1) }
    end
  end

  module M
    C = 'public';  public_constant  :C
    D = 'private'; private_constant :D
  end

  def test_frozen_raises_on_constant_visibility_change
    M.freeze
    assert_raises { M.public_constant :D }
    begin
      M.private_constant :C
    rescue => e
      assert_match /can\'t modify frozen /, e.message
    end
    assert_equal [ :C ], M.constants
  end

end
