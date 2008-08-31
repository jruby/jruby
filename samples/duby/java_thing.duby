import "System", "java.lang.System"

def foo
  home = System.getProperty "java.home"
  System.setProperty "hello.world", "something"
  hello = System.getProperty "hello.world"

  puts home
  puts hello
end

puts "Hello world!"
foo
