require_relative "../../java_integration/spec_helper"
require 'jruby'
require 'jruby/compiler'

describe "A Ruby class generating a Java stub" do
  def generate(script)
    node = JRuby.parse(script)
    # we use __FILE__ so there's something for it to read
    JRuby::Compiler::JavaGenerator.generate_java node, __FILE__
  end

  describe "with a java_package line" do
    it "generates a package line into the Java source" do
      script = generate("java_package 'org.bar'; class Foo; end")

      script.package.should == "org.bar"
      cls = script.classes[0]
      cls.package.should == 'org.bar'

      java = cls.to_s
      java.should match /package org\.bar;/
    end
  end
end