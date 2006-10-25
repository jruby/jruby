require 'test/minirunit'
test_check "High-level Java Support"

if defined? Java

  require 'java'

  module TestJavaSupport
    include_class "org.jruby.test.TestHelper"
    include_package "java.util"

    java_alias :JArray, :ArrayList
    
    # call Java passing Class
    test_equal("java.util.ArrayList", TestHelper.getClassName(ArrayList))

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
    test_equal(Random, r.class)
    r = Random.new(1001)
    test_equal(10.0, Double.new(10).doubleValue())
    test_equal(10.0, Double.new("10").doubleValue())

    # Instance methods
    test_equal(Random, r.class)
    test_equal(Fixnum, r.nextInt.class)
    test_equal(Fixnum, r.nextInt(10).class)

    # Instance methods differing only on argument type
    l1 = Long.new(1234)
    l2 = Long.new(1000)
    test_ok(l1.compareTo(l2) > 0)

    # Dispatching on nil
    sb = TestHelper.getInterfacedInstance()
    test_equal(nil , sb.dispatchObject(nil))

    # Calling with ruby array arguments
    java_alias :JavaString, :String
    a = [104, 101, 108, 108, 111]
#    test_equal("hello", # (char[]) matches here
#               JavaString.new(a, "iso-8859-1").toString)
#    test_equal("104101108108111", # append(Object) triumphs here
#               StringBuffer.new.append(a).toString) 

    # FIXME: easy method for importing java class with colliding name
    # (Since String would be nice to test on)

    # Class methods
    result = System.currentTimeMillis()
    test_equal(Fixnum, result.class)

    # Class methods differing only on argument type
    test_equal(true, Boolean.valueOf("true"))
    test_equal(false, Boolean.valueOf(false))

    include_package 'org.jruby.javasupport.test'

    # Constants
    test_equal(9223372036854775807, Long::MAX_VALUE)
    test_ok(! defined? Character::Y_DATA)  # Known private field in Character
    ConstantHolder  # class definition with "_" constant causes error

    # Using arrays
    list = JArray.new
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
    test_equal(HashMap, h.class)
    h.put("a", 1)
    iter = h.entrySet.iterator
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

    # test support of other class loaders 
    test_helper_class = Java::JavaClass.for_name("org.jruby.test.TestHelper")
    test_helper_class2 = Java::JavaClass.for_name("org.jruby.test.TestHelper")
    test_ok(test_helper_class.java_class == test_helper_class2.java_class, "Successive calls return the same class")
    method = test_helper_class.java_method('loadAlternateClass')
    alt_test_helper_class = method.invoke_static()

    constructor = alt_test_helper_class.constructor();
    alt_test_helper = constructor.new_instance();
    identityMethod = alt_test_helper_class.java_method('identityTest')
    identity = Java.java_to_primitive(identityMethod.invoke(alt_test_helper))
    test_equal("ABCDEFGH",   identity)
  end

  module Foo
 	include_class("java.util.ArrayList")
  end

  include_class("java.lang.String") {|package,name| "J#{name}" }
  include_class ["java.util.Hashtable", "java.util.Vector"]

  test_ok(0, Foo::ArrayList.new.size)
  test_ok("a", JString.new("a"))
  test_ok(0, Vector.new.size)
  test_ok(0, Hashtable.new.size)
  
  a = JString.new  
  # High-level java should only deal with proxies and not low-level JavaClass
  test_ok(a.getClass().class != "Java::JavaClass")
  
  # We had a problem with accessing singleton class versus class earlier. Sanity check
  # to make sure we are not writing class methods to the same place.
  include_class 'org.jruby.test.AlphaSingleton'
  include_class 'org.jruby.test.BetaSingleton'

  test_no_exception { AlphaSingleton.getInstance.alpha }

  # Lazy proxy method tests for alias and respond_to?  
  include_class 'org.jruby.javasupport.test.Color'
  
  color = Color.new('green')

  test_equal(true, color.respond_to?(:setColor))
  test_equal(false, color.respond_to?(:setColorBogus))

  class MyColor < Color
  	alias_method :foo, :getColor
  	
  	def alias_test
  	  test_exception(NoMethodError) { alias_method :foo2, :setColorReallyBogus }
  	end
  end
  my_color = MyColor.new('blue')
  
  test_equal('blue', my_color.foo)
  my_color.alias_test
  my_color.color = 'red'
  test_equal('red', my_color.color)
  my_color.setDark(true)
  test_equal(true, my_color.dark?)
  my_color.dark = false
  test_equal(false, my_color.dark?)
  
  # No explicit test, but implicitly EMPTY_LIST.each should not blow up interpreter
  # Old error was EMPTY_LIST is a private class implementing a public interface with public methods
  include_class 'java.util.Collections'
  Collections::EMPTY_LIST.each {|element| }
  
  # Already loaded proxies should still see extend_proxy
  JavaUtilities.extend_proxy('java.util.List') {
    def foo
      true
    end
  }
  
  test_equal(true, Foo::ArrayList.new.foo)
  
  test_exception(ConstantAlreadyExistsError) { include_class 'java.lang.String' }
  # JString already included and it is the same proxy, so do not throw an error
  # (e.g. intent of include_class already satisfied)
  test_no_exception() do
  	include_class("java.lang.String") {|package,name| "J#{name}" }
  end
  
  # Test java.util.Date <=> Time implicit conversion
  include_class 'java.util.Calendar'
  calendar = Calendar.getInstance
  calendar.setTime(Time.at(0))
  java_date = calendar.getTime
  
  test_equal(java_date.getTime, Time.at(0).to_i)
  
  # test that the list of JString methods contains selected methods from Java
  jstring_methods = ["bytes", "charAt", "char_at","compareTo", "compareToIgnoreCase", "compare_to",
    "compare_to_ignore_case", "concat", "contentEquals", "content_equals", "endsWith",
    "ends_with", "equals", "equalsIgnoreCase", "equals_ignore_case", "getBytes", "getChars",
    "getClass", "get_bytes", "get_chars", "get_class", "hashCode", "hash_code", "indexOf",
    "index_of", "intern", "java_class", "java_object", "java_object=", "lastIndexOf", "last_index_of",
    "length", "matches", "notify", "notifyAll", "notify_all", "regionMatches", "region_matches", "replace",
    "replaceAll", "replaceFirst", "replace_all", "replace_first", "split", "startsWith", "starts_with",
    "subSequence", "sub_sequence", "substring", "taint", "tainted?", "toCharArray", "toLowerCase",
    "toString", "toUpperCase", "to_char_array", "to_java_object", "to_lower_case", "to_string",
    "to_upper_case", "trim", "wait"]
  
  jstring_methods.each { |method| test_ok(JString.public_instance_methods.include?(method)) }
  
  include_class 'java.math.BigDecimal'
  test_equal(BigDecimal, BigDecimal.new("1.23").add(BigDecimal.new("2.34")).class)
  
  a = java.util.ArrayList.new
  test_equal(0, a.size)
  
  Properties = Java::java.util.Properties
  p = Properties.new
  p.setProperty("a", "b")
  test_equal("b", p.getProperty("a"))
  
  class MyBadActionListener < java.awt.event.ActionListener
  end
  
  test_exception(NoMethodError) { MyBadActionListener.new.actionPerformed }

  #test that mis-spelt fq class names don't stop future fq class names
  #with same inner-most package
  test_exception(NameError) { Java::java.til.zip.ZipFile }
  test_no_exception { Java::java.util.zip.ZipFile }

  #test that sub-packages haven't leaked into other packages
  test_equal(false, Java::java.respond_to?(:zip))
  test_equal(false, Java::com.respond_to?(:util))

  #test that sub-packages called [java, javax, com, org] aren't short-circuited
  #to their top-level conterparts
  test_ok(!com.equal?(java.flirble.com))

  #test that we get the same package instance on subsequent calls
  test_ok(com.flirble.equal?(com.flirble))
end
