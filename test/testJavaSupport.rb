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
  end
end
