require 'test/minirunit'
test_check "High-level Java Support"

if defined? Java

  require 'java'

  module TestJavaSupport
    include_package "java.util"

    # Java class loading
    test_exception(NameError) { System }
    include_package "java.lang"
    test_no_exception { System }

    # Class name collisions
    java_alias :JavaInteger, :Integer
    test_equal(10, JavaInteger.new(10).intValue)
    test_exception(NameError) { Integer.new(10) }

    # Constructors
    r = Random.new
    test_equal(Random, r.type)
    r = Random.new(1001)
    test_equal(10.0, Double.new(10).doubleValue())
    test_equal(10.0, Double.new("10").doubleValue())
#    module Swing
#      include_package "javax.swing"
#    end
#    Swing::JFrame.new(nil)

    # Instance methods
    test_equal(Random, r.type)
    test_equal(Fixnum, r.nextInt.type)
    test_equal(Fixnum, r.nextInt(10).type)

    # Instance methods differing only on argument type
    l1 = Long.new(1234)
    l2 = Long.new(1000)
    test_ok(l1.compareTo(l2) > 0)
    s1 = Short.new(1234)
    test_ok(s1.compareTo(l2) > 0)

    # Dispatching on nil
    include_package "org.jruby.util"
    sb = TestHelper.getInterfacedInstance()
    test_equal(nil , sb.dispatchObject(nil))

    # Calling with ruby array arguments
    java_alias :JavaString, :String
    a = [104, 101, 108, 108, 111]
#    test_equal("hello", # (char[]) matches here
#               JavaString.new(a, "iso-8859-1").toString)
    test_equal("104101108108111", # append(Object) triumphs here
               StringBuffer.new.append(a).toString) 

    # FIXME: easy method for importing java class with colliding name
    # (Since String would be nice to test on)

    # Class methods
    result = System.currentTimeMillis()
    test_equal(Fixnum, result.type)

    # Class methods differing only on argument type
    test_equal(true, Boolean.valueOf("true"))
    test_equal(false, Boolean.valueOf(false))

    # Constants
    test_equal(9223372036854775807, Long::MAX_VALUE)
    test_ok(! defined? Character::Y_DATA)  # Known private field in Character

    # Using arrays
    list = ArrayList.new
    list.add(10)
    list.add(20)
    array = list.toArray
    test_equal(10, array[0])
    test_equal(20, array[1])
    test_equal(2, array.length)
    array[1] = 1234
    test_equal(10, array[0])
    test_equal(1234, array[1])
    test_equal([10, 1234], array.entries)
    test_equal(10, array.min)

    # Creating arrays
    array = Double[].new(3)
    test_equal(3, array.length)
    array[0] = 3.14
    array[2] = 17.0
    test_equal(3.14, array[0])
    test_equal(17.0, array[2])

    # Inner classes
    test_equal("java.lang.Character$UnicodeBlock",
               Character::UnicodeBlock.java_class.name)
    test_ok(Character::UnicodeBlock.methods.include?("of"))

    # Subclasses and their return types
    l = ArrayList.new
    r = Random.new
    l.add(10)
    test_equal(10, l.get(0))
    l.add(r)
    r_returned = l.get(1)
    # Since Random is a public class we should get the value casted as that
    test_equal("java.util.Random", r_returned.java_class.name)
    test_ok(r_returned.nextInt.kind_of?(Fixnum))

    # Private classes, interfaces and return types
    h = HashMap.new
    test_equal(HashMap, h.type)
    h.put("a", 1)
    iter = h.entrySet.iterator
    test_equal("java.util.Iterator", iter.java_class.name)
    inner_instance_entry = iter.next
    # The class implements a public interface, MapEntry, so the methods
    # on that should be available, even though the instance is of a
    # private class.
    test_equal("a", inner_instance_entry.getKey)

    # Extending Java classes
    class FooArrayList < ArrayList
      def foo
        size
      end
    end
    l = FooArrayList.new
    test_equal(0, l.foo)
    l.add(100)
    test_equal(1, l.foo)
  end



end
