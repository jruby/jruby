require_relative '../spec_helper'

describe "A Ruby class generating a Java stub" do
  def generate(script)
    node = JRuby.parse(script)
    # we use __FILE__ so there's something for it to read
    JRuby::Compiler::JavaGenerator.generate_java node, __FILE__
  end

  EMPTY_INITIALIZE_PATTERN =
    /public\s+Foo\(\) {\s+this\(__ruby__, __metaclass__\);\s+Helpers.invoke\(.*, this, "initialize"\);/
  OBJECT_INITIALIZE_PATTERN =
    /public\s+Foo\(Object \w+\) {\s+this\(__ruby__, __metaclass__\);\s+IRubyObject \w+ = JavaUtil.convertJavaToRuby\(__ruby__, \w+\);\s+Helpers.invoke\(.*, this, "initialize", .*\);/
  STRING_INITIALIZE_PATTERN =
    /public\s+Foo\(String \w+\) {\s+this\(__ruby__, __metaclass__\);\s+IRubyObject \w+ = JavaUtil.convertJavaToRuby\(__ruby__, \w+\);\s+Helpers.invoke\(.*, this, "initialize", .*\);/

  describe "with no initialize method" do
    it "generates a default constructor" do
      cls = generate("class Foo; end").classes[0]
      expect( cls.has_constructor? ).to be false

      java = cls.to_s
      java.should match EMPTY_INITIALIZE_PATTERN
    end
  end

  describe "with an initialize method" do
    describe "and a constructor java_signature on a another method" do
      it "generates a default constructor" do
        cls = generate("class Foo; def initialize(a); end; java_signature 'Foo()'; def default_cnstr(); end; end").classes[0]
        expect( cls.has_constructor? ).to be true

        expect( init = cls.methods[0] ).to_not be nil
        expect( init.name ).to eql "initialize"
        expect( init.constructor? ).to be true
        expect( init.java_signature.to_s ).to eql "Object initialize(Object a)"
        expect( init.args.length ).to eql 1

        java = init.to_s
        java.should match OBJECT_INITIALIZE_PATTERN

        expect( def_cnstr = cls.methods[1] ).to_not be nil
        expect( def_cnstr.constructor? ).to be true
        expect( def_cnstr.java_signature.to_s ).to eql "Foo()"
        expect( def_cnstr.args.length ).to eql 0

        java = def_cnstr.to_s
        java.should match EMPTY_INITIALIZE_PATTERN
      end
    end

    describe "with no arguments" do
      it "generates a default constructor" do
        cls = generate("class Foo; def initialize; end; end").classes[0]
        expect( cls.has_constructor? ).to be true

        expect( init = cls.methods[0] ).to_not be nil
        expect( init.name ).to eql 'initialize'
        expect( init.constructor? ).to be true
        expect( init.java_signature.to_s ).to eql 'Object initialize()'
        expect( init.args.length ).to eql 0

        java = init.to_s
        expect( java ).to match EMPTY_INITIALIZE_PATTERN
      end
    end

    describe "with one argument and no java_signature" do
      it "generates an (Object) constructor" do
        cls = generate("class Foo; def initialize(a); end; end").classes[0]
        expect( cls.has_constructor? ).to be true

        init = cls.methods[0]
        expect( init.name ).to eql 'initialize'
        expect( init.constructor? ).to be true
        expect( init.java_signature.to_s ).to eql 'Object initialize(Object a)'
        expect( init.args ).to eql ['a']

        java = init.to_s
        expect( java ).to match OBJECT_INITIALIZE_PATTERN
      end
    end

    describe "with one argument and a java_signature" do
      it "generates a type-appropriate constructor" do
        cls = generate("class Foo; java_signature 'Foo(String)'; def initialize(a); end; end").classes[0]
        expect( cls.has_constructor? ).to be true

        init = cls.methods[0]
        expect( init.name ).to eql 'initialize'
        expect( init.constructor? ).to be true
        expect( init.java_signature ).to_not be nil
        expect( init.java_signature.to_s ).to eql "Foo(String)"
        expect( init.args ).to eql ['a']

        java = init.to_s
        expect( java ).to match STRING_INITIALIZE_PATTERN
      end
    end

    describe "with throws clause" do
      it "generates a throws clause" do
        cls = generate("class Foo; java_signature 'Foo() throws FooBarException'; def initialize(); end; end").classes[0]

        method = cls.methods[0]
        expect( method.java_signature.to_s ).to eql 'Foo() throws FooBarException'
      end

      it 'generates a throws clause for more than one exception' do
        cls = generate("class Foo; java_signature 'Foo() throws FooBarException,QuxBazException'; def initialize(); end; end").classes[0]

        method = cls.methods[0]
        method.java_signature.to_s.should == 'Foo() throws FooBarException, QuxBazException'
      end
    end

  end
end
