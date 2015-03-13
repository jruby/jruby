require_relative "../../java_integration/spec_helper"
require 'jruby'
require 'jruby/compiler'

describe "A Ruby class generating a Java stub" do
  def generate(script)
    node = JRuby.parse(script)
    # we use __FILE__ so there's something for it to read
    JRuby::Compiler::JavaGenerator.generate_java node, __FILE__
  end
  
  OBJECT_VOID_BAR_PATTERN =
    /public *(.*) *Object bar\(\) {\s+.*IRubyObject ruby_result = Helpers\.invoke\(.*, this, "bar"\);\s+return \(Object\)ruby_result\.toJava\(Object\.class\);/
  OBJECT_OBJECT_BAR_PATTERN =
    /public *(.*) *Object bar\(Object \w+\) {\s+IRubyObject \S+ = JavaUtil\.convertJavaToRuby\(__ruby__, \S+\);\s+IRubyObject ruby_result = Helpers\.invoke\(.*, this, "bar", .*\);\s+return \(Object\)ruby_result\.toJava\(Object\.class\);/
  BYTE_VOID_BAR_PATTERN =
    /public *(.*) *byte bar_byte\(\) {\s+.*IRubyObject ruby_result = Helpers\.invoke\(.*, this, "bar_byte"\);\s+return \(Byte\)ruby_result\.toJava\(byte\.class\);/
  SHORT_VOID_BAR_PATTERN =
    /public *(.*) *short bar_short\(\) {\s+.*IRubyObject ruby_result = Helpers\.invoke\(.*, this, "bar_short"\);\s+return \(Short\)ruby_result\.toJava\(short\.class\);/
  CHAR_VOID_BAR_PATTERN =
    /public *(.*) *char bar_char\(\) {\s+.*IRubyObject ruby_result = Helpers\.invoke\(.*, this, "bar_char"\);\s+return \(Character\)ruby_result\.toJava\(char\.class\);/
  INT_VOID_BAR_PATTERN =
    /public *(.*) *int bar_int\(\) {\s+.*IRubyObject ruby_result = Helpers\.invoke\(.*, this, "bar_int"\);\s+return \(Integer\)ruby_result\.toJava\(int\.class\);/
  LONG_VOID_BAR_PATTERN =
    /public *(.*) *long bar_long\(\) {\s+.*IRubyObject ruby_result = Helpers\.invoke\(.*, this, "bar_long"\);\s+return \(Long\)ruby_result\.toJava\(long\.class\);/
  FLOAT_VOID_BAR_PATTERN =
    /public *(.*) *float bar_float\(\) {\s+.*IRubyObject ruby_result = Helpers\.invoke\(.*, this, "bar_float"\);\s+return \(Float\)ruby_result\.toJava\(float\.class\);/
  DOUBLE_VOID_BAR_PATTERN =
    /public *(.*) *double bar_double\(\) {\s+.*IRubyObject ruby_result = Helpers\.invoke\(.*, this, "bar_double"\);\s+return \(Double\)ruby_result\.toJava\(double\.class\);/
  BOOLEAN_VOID_BAR_PATTERN =
    /public *(.*) *boolean bar_boolean\(\) {\s+.*IRubyObject ruby_result = Helpers\.invoke\(.*, this, "bar_boolean"\);\s+return \(Boolean\)ruby_result\.toJava\(boolean\.class\);/
  VOID_STRING_BAR_PATTERN =
    /public *(.*) *void bar\(String \w+\) {\s+IRubyObject \S+ = JavaUtil\.convertJavaToRuby\(__ruby__, \S+\);\s+IRubyObject ruby_result = Helpers\.invoke\(.*, this, "bar\S*", .*\);\s+return;/
  VOID_INT_BAR_PATTERN =
    /public *(.*) *void bar\(int \w+\) {\s+IRubyObject \S+ = JavaUtil\.convertJavaToRuby\(__ruby__, \S+\);\s+IRubyObject ruby_result = Helpers\.invoke\(.*, this, "bar\S*", .*\);\s+return;/
  DOUBLE_ARY_VOID_BAR_PATTERN =
    /public *(.*) *double\[\] bar_double_ary\(\) {\s+.*IRubyObject ruby_result = Helpers\.invoke\(.*, this, "bar_double_ary"\);\s+return \(double\[\]\)ruby_result\.toJava\(double\[\]\.class\);/
  BAZ_ARY_VOID_BAR_PATTERN =
    /public *(.*) *baz\[\] bar_baz_ary\(\) {\s+.*IRubyObject ruby_result = Helpers\.invoke\(.*, this, "bar_baz_ary"\);\s+return \(baz\[\]\)ruby_result\.toJava\(baz\[\]\.class\);/
  BAZ_GENERIC_VOID_BAR_PATTERN =
    /public *(.*) *Baz\<Generic\> bar_generic_baz\(\) {\s+.*IRubyObject ruby_result = Helpers\.invoke\(.*, this, "bar_generic_baz"\);\s+return \(Baz\)ruby_result\.toJava\(Baz\.class\);/  

  describe "with a method" do
    describe "with no java_signature" do
      describe "and no arguments" do
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

      describe "and one argument" do
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

        describe "defined on self (the Ruby class)" do
          it "generates a static Object(Object) method" do
            cls = generate("class Foo; def self.bar(a); end; end").classes[0]

            method = cls.methods[0]
            method.name.should == "bar"
            method.static.should == true

            java = method.to_s
            java.should match /static/
          end
        end
      end
    end

    describe "with a java_signature" do
      describe "defined on self (the Ruby class)" do
        it "generates a static Java method" do
          cls = generate("class Foo; java_signature 'void bar(String)'; def self.bar(a); end; end").classes[0]

          method = cls.methods[0]
          method.name.should == "bar"
          method.static.should == true

          java = method.to_s
          java.should match /static/
        end
      end
      
      describe "and no arguments" do
        describe "and a byte return type" do
          it "generates a byte-returning method" do
            cls = generate("
      class Foo
        java_signature 'byte bar_byte()'; def bar_byte; end
      end").classes[0]

            method = cls.methods[0]
            method.should_not be nil
            method.name.should == "bar_byte"
            method.constructor?.should == false
            method.java_signature.to_s.should == "byte bar_byte()"
            method.args.length.should == 0
            java = method.to_s
            java.should match BYTE_VOID_BAR_PATTERN
          end
        end

        describe "and a short return type" do
          it "generates a short-returning method" do
            cls = generate("
      class Foo
        java_signature 'short bar_short()'; def bar_short; end
      end").classes[0]

            method = cls.methods[0]
            method.should_not be nil
            method.name.should == "bar_short"
            method.constructor?.should == false
            method.java_signature.to_s.should == "short bar_short()"
            method.args.length.should == 0
            java = method.to_s
            java.should match SHORT_VOID_BAR_PATTERN
          end
        end

        describe "and a char return type" do
          it "generates a char-returning method" do
            cls = generate("
      class Foo
        java_signature 'char bar_char()'; def bar_char; end
      end").classes[0]

            method = cls.methods[0]
            method.should_not be nil
            method.name.should == "bar_char"
            method.constructor?.should == false
            method.java_signature.to_s.should == "char bar_char()"
            method.args.length.should == 0
            java = method.to_s
            java.should match CHAR_VOID_BAR_PATTERN
          end
        end

        describe "and an int return type" do
          it "generates a int-returning method" do
            cls = generate("
      class Foo
        java_signature 'int bar_int()'; def bar_int; end
      end").classes[0]

            method = cls.methods[0]
            method.should_not be nil
            method.name.should == "bar_int"
            method.constructor?.should == false
            method.java_signature.to_s.should == "int bar_int()"
            method.args.length.should == 0
            java = method.to_s
            java.should match INT_VOID_BAR_PATTERN
          end
        end

        describe "and a long return type" do
          it "generates a long-returning method" do
            cls = generate("
      class Foo
        java_signature 'long bar_long()'; def bar_long; end
      end").classes[0]

            method = cls.methods[0]
            method.should_not be nil
            method.name.should == "bar_long"
            method.constructor?.should == false
            method.java_signature.to_s.should == "long bar_long()"
            method.args.length.should == 0
            java = method.to_s
            java.should match LONG_VOID_BAR_PATTERN
          end
        end

        describe "and a float return type" do
          it "generates a float-returning method" do
            cls = generate("
      class Foo
        java_signature 'float bar_float()'; def bar_float; end
      end").classes[0]

            method = cls.methods[0]
            method.should_not be nil
            method.name.should == "bar_float"
            method.constructor?.should == false
            method.java_signature.to_s.should == "float bar_float()"
            method.args.length.should == 0
            java = method.to_s
            java.should match FLOAT_VOID_BAR_PATTERN
          end
        end

        describe "and a double return type" do
          it "generates a double-returning method" do
            cls = generate("
      class Foo
        java_signature 'double bar_double()'; def bar_double; end
      end").classes[0]

            method = cls.methods[0]
            method.should_not be nil
            method.name.should == "bar_double"
            method.constructor?.should == false
            method.java_signature.to_s.should == "double bar_double()"
            method.args.length.should == 0
            java = method.to_s
            java.should match DOUBLE_VOID_BAR_PATTERN
          end
        end

        describe "and a boolean return type" do
          it "generates a boolean-returning method" do
            cls = generate("
      class Foo
        java_signature 'boolean bar_boolean()'; def bar_boolean; end
      end").classes[0]

            method = cls.methods[0]
            method.should_not be nil
            method.name.should == "bar_boolean"
            method.constructor?.should == false
            method.java_signature.to_s.should == "boolean bar_boolean()"
            method.args.length.should == 0
            java = method.to_s
            java.should match BOOLEAN_VOID_BAR_PATTERN
          end
        end

        describe "and a double[] return type" do
          it "generates a double[]-returning method" do
            cls = generate("
      class Foo
        java_signature 'double[] bar_double_ary()'; def bar_double_ary; end
      end").classes[0]

            method = cls.methods[0]
            method.should_not be nil
            method.name.should == "bar_double_ary"
            method.constructor?.should == false
            method.java_signature.to_s.should == "double[] bar_double_ary()"
            method.args.length.should == 0
            java = method.to_s
            java.should match DOUBLE_ARY_VOID_BAR_PATTERN
          end
        end
        
        describe "and a baz[] return type" do
          it "generates a baz[]-returning method" do
            cls = generate("
          class Foo
            java_signature 'baz[] bar_baz_ary()'; def bar_baz_ary; end
          end").classes[0]
          
            method = cls.methods[0]
            method.should_not be nil
            method.name.should == "bar_baz_ary"
            method.constructor?.should == false
            method.java_signature.to_s.should == "baz[] bar_baz_ary()"
            method.args.length.should == 0
            java = method.to_s
            java.should match BAZ_ARY_VOID_BAR_PATTERN
          end
        end
        
        describe "and a Baz<Generic> return type" do
          it "generates a Baz<Generic>-returning method" do
            cls = generate("
          class Foo
            java_signature 'Baz<Generic> bar_generic_baz()'; def bar_generic_baz; end
          end").classes[0]
            
            method = cls.methods[0]
            method.should_not be nil
            method.name.should == "bar_generic_baz"
            method.constructor?.should == false
            method.java_signature.to_s.should == "Baz<Generic> bar_generic_baz()"
            method.args.length.should == 0
            java = method.to_s
            java.should match BAZ_GENERIC_VOID_BAR_PATTERN
          end
        end
      
      end

      describe "with a void return and String arg" do
        it "generates a void method(String)" do
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

      describe "with an Object(Object) and void(String) overload" do
        it "generates Object(Object) and void(String) overloaded methods" do
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

      describe "with Java modifiers" do
        it "generates a method with those modifiers" do
          cls = generate("class Foo; java_signature 'protected static abstract final strictfp native synchronized void bar()'; def self.bar; end; end").classes[0]

          method = cls.methods[0]
          method.name.should == "bar"
          method.static.should == true

          java = method.to_s
          java.should match /protected/
          java.should match /static/
          java.should match /abstract/
          java.should match /final/
          java.should match /strictfp/
          java.should match /native/
          java.should match /synchronized/
          java.should match /void/
        end

        describe "with multiple visibilities" do
          it "uses the first visibility only" do
            cls = generate("class Foo; java_signature 'private protected public void bar()'; def bar; end; end").classes[0]

            method = cls.methods[0]
            method.name.should == "bar"
            method.static.should == false

            java = method.to_s
            java.should match /private/
            java.should_not match /protected/
            java.should_not match /public/
          end
        end
      end
    end

    describe "with throws clause" do
      it "generates a throws clause" do
        cls = generate("class Foo; java_signature 'public void bar() throws FooBarException'; def bar; end; end").classes[0]

        method = cls.methods[0]
        method.java_signature.to_s.should == 'public void bar() throws FooBarException'
      end

      it 'generates a throws clause for more than one exception' do
        cls = generate("class Foo; java_signature 'public void bar() throws FooBarException,QuxBazException'; def bar; end; end").classes[0]

        method = cls.methods[0]
        method.java_signature.to_s.should == 'public void bar() throws FooBarException, QuxBazException'   
      end        
    end
  end
end
