require 'minijava'

# simple runnable test
import 'java.lang.Runnable'
import 'java.lang.Thread', 'JThread'

MyRunnable = new_class(Runnable)

mr = MyRunnable.new

class MyRunnable
  def run
    puts 'foo'
  end
end

JThread.new(mr, 'mythread'.to_java).start

# Overloaded method test and benchmark
=begin
Overloaded interface looks like this:
public interface Overloaded {
    public String getName(String name);
    public String getName(String name, Integer age);
    public Object foo(Object arg);
}

OverloadedTest looks lke this:
public class OverloadedTest {
    public static void testOverloaded(Overloaded o, int loops) {
        long time = System.currentTimeMillis();
        for (int i = 0; i < loops; i++) {
            o.getName("Charlie");
        }
        System.out.println("took: " + (System.currentTimeMillis() - time));
    }

    public static void testOverloaded2(Overloaded o, int loops) {
        long time = System.currentTimeMillis();
        for (int i = 0; i < loops; i++) {
            o.foo("Charlie");
        }
        System.out.println("took: " + (System.currentTimeMillis() - time));
    }
}
=end

import "org.jruby.test.Overloaded"
import "org.jruby.test.OverloadedTest"
import "java.lang.Integer", "JInteger"

MyOverloaded = new_class(Overloaded)
mine = MyOverloaded.new

# test missing  methods
begin
  mine.getName('foo'.to_java)
rescue
  puts "Method not yet implemented"
end

class MyOverloaded
  def getName(*args)
    __NOFRAME__
    args[0]
  end
  def foo(arg)
    __NOFRAME__
    arg
  end
end

5.times {
  puts 'getName test (10_000_000 loops, time in ms):'
  OverloadedTest.testOverloaded(mine, 10_000_000.to_java(JInteger))
  puts 'foo test (10_000_000 loops, time in ms):'
  OverloadedTest.testOverloaded2(mine, 10_000_000.to_java(JInteger))
}

=begin The same test using Java integration

import "org.jruby.test.Overloaded"
import "org.jruby.test.OverloadedTest"

class MyOverloaded
  include Overloaded
  def getName(*args)
    args[0]
  end
  def foo(arg)
    arg
  end
end

mine = MyOverloaded.new

5.times {
  puts "JI getName test (100k loops, time in ms):"
  OverloadedTest.testOverloaded(mine, 100_000)
  puts "JI foo test (100k loops, time in ms):"
  OverloadedTest.testOverloaded2(mine, 100_000)
}
=end