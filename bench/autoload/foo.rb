class Foo
  autoload :Bar, "bar"
  module Baz
    Y = Foo::Bar::X + 1
  end
end
