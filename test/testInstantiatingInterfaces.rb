require 'test/minirunit'
test_check "Anonymous Interface instantiation"

if defined? Java
  require 'java'

  class A
    include java.lang.Runnable
  end
  test_exception(NoMethodError) do
    A.new.run
  end

  foo = nil
  ev = java.lang.Runnable.impl do
    foo = "ran"
  end
  ev.run rescue nil
  test_equal("ran", foo)

  cs = java.lang.CharSequence.impl(:charAt) do |sym, index|
    test_equal(:charAt, sym)
    test_equal(0, index)
  end

  test_no_exception { cs.charAt(0) }
  test_exception(NoMethodError) do
    cs.length
  end
end
