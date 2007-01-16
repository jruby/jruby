$: << File.dirname($0) << File.join(File.dirname($0), "..")
require 'rubicon'


class TestObjectSpace < Rubicon::TestCase

  def test_s__id2ref
    s = "hello"
    t = ObjectSpace._id2ref(s.__id__)
    assert_equal(s, t)
    assert_equal(s.__id__, t.__id__)
  end


  # finalizer manipulation
  def test_s_finalizers
    tf = Tempfile.new("tf")
    begin
      tf.puts %{
	a = "string"
	ObjectSpace.define_finalizer(a) { puts "OK" }
      }
      tf.close
      IO.popen("#$interpreter #{tf.path}") do |p|
	assert_equal("OK", p.gets.chomp)
      end
    ensure
      tf.close(true)
    end
  end

  class A;      end
  class B < A;  end
  class C < A;  end
  class D < C;  end

  def test_s_each_object
    a = A.new
    b = B.new
    c = C.new
    d = D.new

    res = []
    ObjectSpace.each_object(TestObjectSpace::A) { |o| res << o }
    assert_bag_equal([a, b, c, d], res)

    res = []
    ObjectSpace.each_object(B) { |o| res << o }
    assert_bag_equal([b], res)

    res = []
    ObjectSpace.each_object(C) { |o| res << o }
    assert_bag_equal([c, d], res)
  end

  def test_s_garbage_collect
    skipping("how to test")
  end

#  Tested in finalizers
#  def test_s_finalizers
#    fail("untested")
#  end

# Tested in finalizers
#  def test_s_remove_finalizer
#    fail("untested")
#  end

end

Rubicon::handleTests(TestObjectSpace) if $0 == __FILE__
