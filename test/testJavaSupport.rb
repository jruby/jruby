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
  end
end

test_print_report
