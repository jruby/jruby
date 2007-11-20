require 'test/unit'
require 'java'

class OuterTuple
    attr_accessor :a
    attr_accessor :b
    def initialize(a, b); @a = a; @b = b; end
    def ==(other); other != nil && self.a == other.a && self.b == other.b; end
    def inspect
      "Tuple [#{self.a.inspect}, #{self.b.inspect}]"
    end
  end

class TestObjectPersistence < Test::Unit::TestCase
  import java.io.ObjectOutputStream
  import java.io.ByteArrayOutputStream
  import java.io.ObjectInputStream
  import java.io.ByteArrayInputStream
  
  def output_streams
    baos = ByteArrayOutputStream.new
    oos = ObjectOutputStream.new(baos)
    
    return baos, oos
  end
  
  def input_streams(bytes)
    bais = ByteArrayInputStream.new(bytes)
    ois = ObjectInputStream.new(bais)
    
    return bais, ois
  end
  
  def test_serialize_deserialize
    
    obj = 'foo'
    obj2 = nil
    
    bytes_out, obj_out = output_streams
    
    assert_nothing_raised { obj_out.writeObject(obj) }
    
    bytes_in, obj_in = input_streams(bytes_out.to_byte_array)
    
    assert_nothing_raised { obj2 = obj_in.readObject() }
    
    assert_equal(obj, obj2)
  end
  
  def test_array_graph
    a = ['foo']
    b = ['bar']
    c = [a, b]
    d = [c, b]
    e = [a, c]
    f = [c, d, e]
    g = [a, b, c, d, e, f]
    
    bytes_out, obj_out = output_streams
    obj_out.writeObject(g)
    
    bytes_in, obj_in = input_streams(bytes_out.to_byte_array)
    g2 = obj_in.readObject()
    
    assert_equal(g, g2)
    g2.inspect
    assert_equal(g.inspect, g2.inspect)
  end
  
  class InnerTuple < OuterTuple; end
  
  def test_custom_toplevel_type_object_graph
    a = OuterTuple.new('foo', 'bar')
    b = OuterTuple.new(a, 'baz')
    c = OuterTuple.new(a, b)
    d = OuterTuple.new(a, c)
    e = OuterTuple.new(b, c)
    f = OuterTuple.new(d, e)
    g = OuterTuple.new(e, f)
    
    bytes_out, obj_out = output_streams
    obj_out.writeObject(g)
    
    bytes_in, obj_in = input_streams(bytes_out.to_byte_array)
    g2 = obj_in.readObject()
    
    assert_equal(g, g2)
    assert_equal(g.inspect, g2.inspect)
  end
  
=begin This test doesn't work yet because RubyObject.metaClassName is only the base name of the class
  def test_custom_nested_type_object_graph
    a = InnerTuple.new(:foo, :bar)
    b = InnerTuple.new(a, :baz)
    c = InnerTuple.new(a, b)
    d = InnerTuple.new(a, c)
    e = InnerTuple.new(b, c)
    f = InnerTuple.new(d, e)
    g = InnerTuple.new(e, f)
    
    bytes_out, obj_out = output_streams
    obj_out.writeObject(g)
    
    bytes_in, obj_in = input_streams(bytes_out.to_byte_array)
    g2 = obj_in.readObject()
    
    assert_equal(g, g2)
  end
=end
end