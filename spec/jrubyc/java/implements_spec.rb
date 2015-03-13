require_relative "../../java_integration/spec_helper"
require 'jruby'
require 'jruby/compiler'

describe "A Ruby class generating a Java stub" do
  def generate(script)
    node = JRuby.parse(script)
    # we use __FILE__ so there's something for it to read
    JRuby::Compiler::JavaGenerator.generate_java node, __FILE__
  end

  describe "with a java_implements line" do
    it "generates a class that implements the given interfaces" do
      cls = generate("class Foo; java_implements 'Runnable'; end").classes[0]

      cls.interfaces.length.should == 1
      cls.interfaces[0].should == "Runnable"

      java = cls.to_s
      java.should match /public class Foo implements Runnable/

      cls = generate("class Foo; java_implements 'Runnable', 'Serializable'; end").classes[0]

      cls.interfaces.length.should == 2
      cls.interfaces[0].should == "Runnable"
      cls.interfaces[1].should == "Serializable"

      java = cls.to_s
      java.should match /public class Foo implements Runnable, Serializable/
    end
  end
end