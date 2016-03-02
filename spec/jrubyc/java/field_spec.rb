require_relative "../../java_integration/spec_helper"
require 'jruby'
require 'jruby/compiler'

describe "A Ruby class generating a Java stub" do
  def generate(script)
    node = JRuby.parse(script)
    # we use __FILE__ so there's something for it to read
    JRuby::Compiler::JavaGenerator.generate_java node, __FILE__
  end

  describe "with additional java_import lines" do
    it "generates a field into the java source" do
      script = generate("class Foo; java_field 'String abc'; end")

      cls = script.classes[0]
      expect( cls.fields.length ).to eql 1
      expect( cls.fields.first.first ).to eql "String abc"
      expect( cls.fields.first.last ).to be_empty

      java = script.to_s
      expect( java ).to match /String abc;/n
    end

    it "generates fields into the java source" do
      script = generate("class Foo; java_field 'String abc'; java_field 'Integer xyz'; end")

      cls = script.classes[0]
      expect( cls.fields.length ).to eql 2
      expect( cls.fields.first.first ).to eql "String abc"
      expect( cls.fields.first.last ).to be_empty
      expect( cls.fields.last.first ).to eql "Integer xyz"
      expect( cls.fields.last.last ).to be_empty

      java = script.to_s
      expect( java ).to match /String abc;/n
      expect( java ).to match /Integer xyz;/n
    end

    it "generates fields with annotations into the java source" do
      script = generate("class Foo; java_annotation 'Deprecated'; java_field 'String abc'; java_field 'String xyx'; end")

      cls = script.classes[0]
      expect( cls.fields.length ).to eql 2
      expect( cls.fields.first.first ).to eql "String abc"
      expect( cls.fields.first.last ).to include "Deprecated"

      java = script.to_s
      expect( java ).to match /@Deprecated\s+String abc;/n

      cls = script.classes[0]
      expect( cls.fields.last.last ).to be_empty

      java = script.to_s
      expect( java ).to_not match /String abc;\s+@Deprecated\s+String xyz;/n
    end
  end
end
