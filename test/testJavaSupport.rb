require 'test/minirunit'
test_check "High-level Java Support"

if defined? Java

  module TestJavaSupport
    include_package "java.util"

    # Constructors
    r = Random.new
    test_equal(Random, r.type)
    r = Random.new(1001)

    # Instance methods
    test_equal(Random, r.type)
    test_equal(Fixnum, r.nextInt.type)
    test_equal(Fixnum, r.nextInt(10).type)

    # FIXME: easy method for importing java class with colliding name
    # (Since String would be nice to test on)

    # Java class loading
    test_exception(NameError) { System }
    include_package "java.lang"
    System

    # Class methods
    result = System.currentTimeMillis()
    test_equal(Fixnum, result.type)

    # Constants
    test_equal(9223372036854775807, Long.MAX_VALUE)

    # Inner classes
    test_equal("java.lang.Character$UnicodeBlock",
               Character::UnicodeBlock.class_eval("@java_class.name"))
    test_ok(Character::UnicodeBlock.methods.include?("of"))

    h = HashMap.new
    test_equal(HashMap, h.type)
    h.put("a", 1)
    iter = h.entrySet.iterator
    test_equal("java.util.Iterator", iter.type.class_eval("@java_class.name"))
    inner_instance_entry = iter.next
    #FIXME: Need to downcast to MapEntry before the test below works...
    #test_equal("a", inner_instance_entry.getKey)
  end
end
