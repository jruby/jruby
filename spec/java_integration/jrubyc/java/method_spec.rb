require File.dirname(__FILE__) + "/../../spec_helper"
require 'jruby'
require 'jruby/compiler'

describe "A Ruby class generating a Java stub" do
  def generate(script)
    node = JRuby.parse(script)
    # we use __FILE__ so there's something for it to read
    JRuby::Compiler::JavaGenerator.generate_java node, __FILE__
  end
  
  OBJECT_VOID_BAR_PATTERN =
    /public Object bar\(\) {\s+.*IRubyObject ruby_result = RuntimeHelpers\.invoke\(.*, this, "bar"\);\s+return ruby_result\.toJava\(Object\.class\);/
  OBJECT_OBJECT_BAR_PATTERN =
    /public Object bar\(Object \w+\) {\s+IRubyObject \S+ = JavaUtil\.convertJavaToRuby\(__ruby__, \S+\);\s+IRubyObject ruby_result = RuntimeHelpers\.invoke\(.*, this, "bar", .*\);\s+return ruby_result\.toJava\(Object\.class\);/
  VOID_STRING_BAR_PATTERN =
    /public void bar\(String \w+\) {\s+IRubyObject \S+ = JavaUtil\.convertJavaToRuby\(__ruby__, \S+\);\s+IRubyObject ruby_result = RuntimeHelpers\.invoke\(.*, this, "bar", .*\);\s+return;/

  describe "with a no-arg method" do
    it "generates an Object bar() method" do
      cls = generate("class Foo; def bar; end; end").classes[0]

      method = cls.methods[0]
      method.should_not be nil
      method.name.should == "bar"
      method.constructor.should == false
      method.java_signature.should == nil
      method.args.length.should == 0

      java = method.to_s
      java.should match OBJECT_VOID_BAR_PATTERN
    end
  end

  describe "with a one-argument, no-signature method" do
    it "generates an Object(Object) method" do
      cls = generate("class Foo; def bar(a); end; end").classes[0]

      method = cls.methods[0]
      method.should_not be nil
      method.name.should == "bar"
      method.constructor.should == false
      method.java_signature.should == nil
      method.args.length.should == 1

      java = method.to_s
      java.should match OBJECT_OBJECT_BAR_PATTERN
    end
  end

  describe "with a one-argument, signature method" do
    it "generates a type-appropriate method" do
      cls = generate("class Foo; java_signature 'void bar(String)'; def bar(a); end; end").classes[0]

      method = cls.methods[0]
      method.name.should == "bar"
      method.constructor.should == false
      method.java_signature.should_not == nil
      method.java_signature.to_s.should == "void bar(String)"
      method.args.length.should == 1
      method.args[0].should == 'a'

      java = method.to_s
      java.should match VOID_STRING_BAR_PATTERN
    end
  end
end
