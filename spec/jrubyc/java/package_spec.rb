require_relative '../spec_helper'

describe "A Ruby class generating a Java stub" do
  def generate(script)
    node = JRuby.parse(script)
    # we use __FILE__ so there's something for it to read
    JRuby::Compiler::JavaGenerator.generate_java node, __FILE__
  end

  describe "with a java_package line" do
    it "generates a package line into the Java source" do
      script = generate("java_package 'org.bar'; class Foo; end")

      expect( script.package ).to eql 'org.bar'
      cls = script.classes[0]
      expect( cls.package ).to eql 'org.bar'

      expect( cls.to_s ).to match /package org\.bar;/n
    end
  end
end