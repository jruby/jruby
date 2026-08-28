require_relative '../spec_helper'

describe "A Ruby class generating a Java stub" do
  def generate(script)
    node = JRuby.parse(script)
    # we use __FILE__ so there's something for it to read
    JRuby::Compiler::JavaGenerator.generate_java node, __FILE__
  end

  describe "with a java_implements line" do
    it "generates a class that implements the given interfaces" do
      cls = generate("class Foo; java_implements 'Runnable'; end").classes[0]

      expect( cls.interfaces.length ).to be 1
      expect( cls.interfaces ).to eql [ 'Runnable' ]

      java = cls.to_s
      expect( java ).to match /^public class Foo extends RubyObject implements Runnable/

      cls = generate("class Foo; java_implements 'Runnable', 'Serializable'; end").classes[0]

      expect( cls.interfaces.length ).to be 2
      expect( cls.interfaces ).to eql [ 'Runnable', 'Serializable' ]

      java = cls.to_s
      expect( java ).to match /^public class Foo extends RubyObject implements Runnable, Serializable/
    end
  end
end