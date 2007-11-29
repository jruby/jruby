require 'test/minirunit'
test_check "Test Source Positions:"

class Object
    def meta
        class<<self;self;end
    end
end

class Foo

    meta.instance_eval do
        def bar
        end
    end
    
    meta.class_eval do
        def bar2
        end
    end

end

test_no_exception{Foo.bar2}
test_no_exception{Foo.meta.bar}
test_no_exception{Foo.meta.meta.bar}

FooC = Foo.clone

test_no_exception{FooC.bar2}
test_exception(NoMethodError){FooC.meta.bar}
test_exception(NoMethodError){FooC.meta.meta.bar}

FooD = Foo.dup

test_no_exception{FooD.bar2}
test_exception(NoMethodError){FooD.meta.bar}
test_exception(NoMethodError){FooD.meta.meta.bar}

foo = Foo.new

foo.meta.instance_eval do
    def bar3
    end
end

foo.meta.class_eval do
    def bar4
    end
end

foo.meta.meta.class_eval do
    def bar5
    end
end

test_no_exception{foo.bar4}
test_no_exception{foo.meta.bar3}
test_no_exception{foo.meta.meta.bar3}

test_no_exception{foo.meta.bar5}
test_no_exception{foo.meta.meta.bar5}

def pm m
    p m
    p m.dup
    p m.clone
end

test_equal(Class.to_s,"Class")
test_equal(Module.to_s,"Module")
test_equal(Object.to_s,"Object")
test_equal(Kernel.to_s,"Kernel")

test_ok(Class.dup.to_s =~ /^#<Class/)
test_ok(Module.dup.to_s =~ /^#<Class/)
test_ok(Object.dup.to_s =~ /^#<Class/)
test_ok(Kernel.dup.to_s =~ /^#<Module/)

test_ok(Class.clone.to_s =~ /^#<Class/)
test_ok(Module.clone.to_s =~ /^#<Class/)
test_ok(Object.clone.to_s =~ /^#<Class/)
test_ok(Kernel.clone.to_s =~ /^#<Module/)

class Object
    def meta
        class<<self;self;end
    end
end

class Foo
end

foo = Foo.new

test_ok(foo.meta.meta.to_s =~ /^(#<Class:){2}#</)
test_ok(foo.meta.meta.meta.to_s =~ /^(#<Class:){3}#</)
test_ok(foo.meta.meta.meta.meta.to_s =~ /^(#<Class:){4}#</)

# test of JRUBY-583
require 'stringio'

$orig_stdout = $stdout
$orig_stderr = $stderr

out_io = StringIO.new
$stdout = out_io
$stderr = out_io

begin
  puts class Foo583
    @@a = 1
  end
  
  f = Foo583.new
  class << f
    @@a = 3
    p @@a
  end
rescue
  $stdout = $orig_stdout
  $stderr = $orig_stderr
  # fail the test
  test_ok(false)
end


$stdout = $orig_stdout
$stderr = $orig_stderr
in_io = StringIO.new out_io.string
test_equal(1, in_io.gets.chomp.to_i)
test_ok(in_io.gets.chomp =~ /warning: class variable access from toplevel singleton method$/)
test_ok(in_io.gets.chomp =~ /warning: class variable access from toplevel singleton method$/)
test_equal(3, in_io.gets.chomp.to_i)
test_ok(in_io.eof?)


class Numeric
  def singleton_method_added a # turn off the default behaviour
  end
end

a = 1.0

class << a
  def hello
    "hello"
  end
end

test_equal("hello", a.hello)
