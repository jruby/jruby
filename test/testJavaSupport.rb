require 'minirunit'
test_check "Java Support"

if defined? Java

  Java.import("java.lang")

  test_ok(ClassLoader.type)

  Java.name("Integer", "JavaInteger")

  test_ok(JavaInteger.respond_to?("parseInt"))
  test_ok(JavaInteger::MAX_VALUE > 0)
  test_equal(10, JavaInteger.parseInt("10"))
  test_equal(22, JavaInteger.new(22).intValue())
  test_equal(JavaInteger.parseInt("10"),
             JavaInteger.parseInt("10"))
end
