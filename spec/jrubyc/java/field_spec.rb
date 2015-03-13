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
      cls.fields.length.should == 1
      cls.fields.first.first.should == "String abc"
      cls.fields.first.last.should be_empty

      java = script.to_s
      java.should match /String abc;/
    end

    it "generates fields into the java source" do
      script = generate("class Foo; java_field 'String abc'; java_field 'Integer xyz'; end")

      cls = script.classes[0]
      cls.fields.length.should == 2
      cls.fields.first.first.should == "String abc"
      cls.fields.first.last.should be_empty
      cls.fields.last.first.should == "Integer xyz"
      cls.fields.last.last.should be_empty

      java = script.to_s
      java.should match /String abc;/
      java.should match /Integer xyz;/
    end

    it "generates fields with annotations into the java source" do
      script = generate("class Foo; java_annotation 'Deprecated'; java_field 'String abc'; java_field 'String xyx'; end")

      cls = script.classes[0]
      cls.fields.length.should == 2
      cls.fields.first.first.should == "String abc"
      cls.fields.first.last.should include "Deprecated"

      java = script.to_s
      java.should match /@Deprecated\s+String abc;/

      cls = script.classes[0]
      cls.fields.last.last.should be_empty

      java = script.to_s
      java.should_not match /String abc;\s+@Deprecated\s+String xyz;/
    end
  end
end
