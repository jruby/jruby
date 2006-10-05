require 'test/minirunit'
test_check "Anonymous Interface instantiation"

if defined? Java
  require 'java'

  # Tests unimplemented interface methods
  class A < java.awt.event.ActionListener
  end
  test_exception(NoMethodError) do
    A.new.actionPerformed(nil)
  end

  foo = nil
  ev = java.awt.event.ActionListener.impl do
    foo = "ran"
  end
  ev.actionPerformed(nil) rescue nil
  test_equal("ran", foo)

  fl = java.awt.event.FocusListener.impl(:focusGained) do |sym, event|
    test_equal(:focusGained, sym)
    test_ok(event != nil)
  end

  test_no_exception { fl.focusGained(java.awt.event.FocusEvent.new(java.awt.TextField.new, 0)) }
  test_exception(NoMethodError) do
    fl.focusLost(nil)
  end
end
