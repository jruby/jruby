require 'test/unit'
require 'drb'
require 'java'

class TestWeakDrbIdConv < Test::Unit::TestCase
  def test_weak_drb_id_conv
    conv = DRb::DRbIdConv.new
    obj_ary = []
    id_ary = []

    # populate
    100.times do
      obj = Object.new
      obj_ary << obj
      id_ary << conv.to_id(obj)
    end

    # confirm they're there
    id_ary.each do |id|
      assert conv.to_obj(id)
    end

    # dereference objects and force GC
    obj_ary = nil
    2.times {java.lang.System.gc}
    
    # confirm they're gone
    id_ary.each do |id|
      assert !conv.to_obj(id)
    end
  end
end
