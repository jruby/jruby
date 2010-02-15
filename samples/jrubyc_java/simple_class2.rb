require 'java'

class MyRubyClass
  java_signature [] => :void
  def helloWorld
    puts "Hello from Ruby"
  end
  
  java_signature [String] => :void
  def goodbyeWorld(a)
    puts a
  end
end
