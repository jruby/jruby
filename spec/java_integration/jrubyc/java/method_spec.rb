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
    /public *(.*) *Object bar\(\) {\s+.*IRubyObject ruby_result = RuntimeHelpers\.invoke\(.*, this, "bar"\);\s+return \(Object\)ruby_result\.toJava\(Object\.class\);/
  OBJECT_OBJECT_BAR_PATTERN =
    /public *(.*) *Object bar\(Object \w+\) {\s+IRubyObject \S+ = JavaUtil\.convertJavaToRuby\(__ruby__, \S+\);\s+IRubyObject ruby_result = RuntimeHelpers\.invoke\(.*, this, "bar", .*\);\s+return \(Object\)ruby_result\.toJava\(Object\.class\);/
  VOID_STRING_BAR_PATTERN =
    /public *(.*) *void bar\(String \w+\) {\s+IRubyObject \S+ = JavaUtil\.convertJavaToRuby\(__ruby__, \S+\);\s+IRubyObject ruby_result = RuntimeHelpers\.invoke\(.*, this, "bar\S*", .*\);\s+return;/
  VOID_INT_BAR_PATTERN =
    /public *(.*) *void bar\(int \w+\) {\s+IRubyObject \S+ = JavaUtil\.convertJavaToRuby\(__ruby__, \S+\);\s+IRubyObject ruby_result = RuntimeHelpers\.invoke\(.*, this, "bar\S*", .*\);\s+return;/

  describe "with a no-arg method" do
    it "generates an Object bar() method" do
      cls = generate("class Foo; def bar; end; end").classes[0]

      method = cls.methods[0]
      method.should_not be nil
      method.name.should == "bar"
      method.constructor?.should == false
      method.java_signature.to_s.should == "Object bar()"
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
      method.constructor?.should == false
      method.java_signature.to_s.should == "Object bar(Object a)"
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
      method.constructor?.should == false
      method.java_signature.should_not == nil
      method.java_signature.to_s.should == "void bar(String)"
      method.args.length.should == 1
      method.args[0].should == 'a'

      java = method.to_s
      java.should match VOID_STRING_BAR_PATTERN
    end
  end

  describe "with an Object(Object) and void(String) overloaded method" do
    it "generates type-appropriate overloads" do
      cls = generate("
        class Foo
          java_signature 'void bar(String)'
          def bar_str(a); end

          java_signature 'void bar(int)'
          def bar_int(a); end
        end").classes[0]

      cls.methods.length.should == 2
      vs_method = cls.methods[0]
      vs_method.name.should == "bar_str"
      vs_method.constructor?.should == false
      vs_method.java_signature.should_not == nil
      vs_method.java_signature.to_s.should == "void bar(String)"
      vs_method.args.length.should == 1
      vs_method.args[0].should == 'a'
      vs_method.to_s.should match VOID_STRING_BAR_PATTERN

      oo_method = cls.methods[1]
      oo_method.name.should == "bar_int"
      oo_method.constructor?.should == false
      oo_method.java_signature.should_not == nil
      oo_method.java_signature.to_s.should == "void bar(int)"
      oo_method.args.length.should == 1
      oo_method.args[0].should == 'a'
      oo_method.to_s.should match VOID_INT_BAR_PATTERN
    end
  end

  describe "with a method defined on self (the Ruby class)" do
    it "generates a static Java method" do
      cls = generate("class Foo; java_signature 'void bar(String)'; def self.bar(a); end; end").classes[0]

      method = cls.methods[0]
      method.name.should == "bar"
      method.static.should == true

      java = method.to_s
      java.should match /static/
    end
  end
end
