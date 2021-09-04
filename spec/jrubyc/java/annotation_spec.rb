require_relative '../spec_helper'

describe "A Ruby class generating a Java stub" do
  def generate(script)
    node = JRuby.parse(script)
    # we use __FILE__ so there's something for it to read
    JRuby::Compiler::JavaGenerator.generate_java node, __FILE__
  end

  describe "with an annotated class" do
    it "generates an annotation in the Java source" do
      cls = generate("java_annotation 'java.lang.SuppressWarnings(name = \"blah\")'; class Foo; end").classes[0]

      expect( cls.annotations.length ).to eql 1
      anno = cls.annotations[0]
      expect( anno ).to eql 'java.lang.SuppressWarnings(name = "blah")'

      expect( cls.to_s ).to match /@java\.lang\.SuppressWarnings\(name = "blah"\)\s+public class Foo/n
    end
  end

  describe "with an annotated method" do
    it "generates an annotation in the Java source" do
      cls = generate("class Foo; java_annotation 'java.lang.SuppressWarnings(name = \"blah\")'; def bar; end; end").classes[0]

      expect( cls.annotations.length ).to eql 0
      method = cls.methods[0]
      expect( method.annotations.length ).to eql 1
      anno = method.annotations[0]
      expect( anno ).to eql 'java.lang.SuppressWarnings(name = "blah")'

      expect( method.to_s ).to match /@java\.lang\.SuppressWarnings\(name = "blah"\)\s+public Object bar/n
    end
  end

  describe "with an annotated method, java_signature style" do
    it "generates an annotation in the Java source" do
      cls = generate("class Foo; java_signature '@java.lang.SuppressWarnings(name = \"blah\") public Object bar()'; def bar; end; end").classes[0]

      expect( cls.to_s ).to match /@java\.lang\.SuppressWarnings\(name="blah"\)\s+public Object bar/n
    end
  end
end
