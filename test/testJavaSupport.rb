require 'test/minirunit'
test_check "High-level Java Support"

if defined? Java

  module TestJavaSupport
    include_package "java.util"

    r = Random.new
    test_equal(TestJavaSupport::Random, r.type)
    r = Random.new(1001)
    test_equal(TestJavaSupport::Random, r.type)
    test_equal(Fixnum, r.nextInt.type)
    test_equal(Fixnum, r.nextInt(10).type)

    # FIXME: easy method for importing java class with colliding name
    # (Since String would be nice to test on)

    #include_package "java.lang"
    #test_equal(true, Boolean.valueOf("true"))
  end
end
