require 'java'
require 'jruby'

describe "A Ruby object" do
  it "can serialize through ObjectOutputStream" do
    baos = java.io.ByteArrayOutputStream.new
    oos = java.io.ObjectOutputStream.new(baos)
    
    obj = Object.new
    
    oos.writeObject(obj)
    bytes = baos.toByteArray
    
    bais = java.io.ByteArrayInputStream.new(bytes)
    ois = java.io.ObjectInputStream.new(bais)

    org.jruby.Ruby.setThreadLocalRuntime(JRuby.runtime)
    
    obj2 = ois.readObject
    
    obj2.class.should == Object
  end
  
  # TODO: Need more specs and fixes for core JRuby classes that don't serialize yet
end
