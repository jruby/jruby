require_relative "../../java_integration/spec_helper"
require 'jruby'
require 'jruby/compiler'

describe "A Ruby class generating a Java stub" do
  def generate(script)
    node = JRuby.parse(script)
    # we use __FILE__ so there's something for it to read
    JRuby::Compiler::JavaGenerator.generate_java node, __FILE__
  end

  describe "with an annotated class" do
    it "generates an annotation in the Java source" do
      cls = generate("java_annotation 'java.lang.SuppressWarnings(name = \"blah\")'; class Foo; end").classes[0]

      cls.annotations.length.should == 1
      anno = cls.annotations[0]
      anno.should == 'java.lang.SuppressWarnings(name = "blah")'

      java = cls.to_s
      java.should match /@java\.lang\.SuppressWarnings\(name = "blah"\)\s+public class Foo/
    end
  end

  describe "with an annotated method" do
    it "generates an annotation in the Java source" do
      cls = generate("class Foo; java_annotation 'java.lang.SuppressWarnings(name = \"blah\")'; def bar; end; end").classes[0]

      cls.annotations.length.should == 0
      method = cls.methods[0]
      method.annotations.length.should == 1
      anno = method.annotations[0]
      anno.should == 'java.lang.SuppressWarnings(name = "blah")'

      java = method.to_s
      java.should match /@java\.lang\.SuppressWarnings\(name = "blah"\)\s+public Object bar/
    end
  end
end