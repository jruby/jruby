
require 'test/unit'

class TestBindingEvalYield < Test::Unit::TestCase
  def test_binding_eval_yield_caller
    b = create_test_binding { 'from caller' }
    result = eval('yield', b)
    assert_equal('from caller', result)
  end

  # Declared by ruby-core to be unspecified, and does not work in 1.9
=begin
  def test_binding_eval_yield_binding
    b = create_test_binding_w_block { 'from caller' }
    result = eval('yield', b)
    assert_equal('from binding block', result)
  end
=end

  def test_binding_eval_yield
    assert_equal('from caller', eval_yield { 'from caller' })
  end

  private
  def create_test_binding
    binding
  end

  def create_test_binding_w_block
    binding { 'from binding block' }
  end

  def eval_yield
    b = binding
    eval('yield', b)
  end

  # I question whether this is reasonable to support, since 1.9 does not
  def test_eval_on_send_binding
    obj = Object.new
    obj2 = eval "self", obj.send(:binding)
    assert_equal(obj, obj2)
  end
end
