require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.ClassWithEnums"
java_import "java_integration.fixtures.JavaFields"
java_import "java_integration.fixtures.InnerClasses"

describe "Kernel\#java_import" do
  describe "given a default-package class" do
    it "imports the class appropriately" do
      m = Module.new do
        java_import Java::DefaultPackageClass
      end
      expect(m::DefaultPackageClass).to eq(Java::DefaultPackageClass)
    end
  end
end

describe "Java::JavaClass.for_name" do
  # NOTE: 'breaking compatibility' used to work for JavaClass, we could make it
  # work for java.lang.Class by patching for_name but it's counter-intuitive.
  it "should return primitive classes for Java primitive type names" do
    class_for_name = -> (name) { JRuby.load_java_class(name) } # Java::JavaClass.for_name 'replacement'
    expect(class_for_name.("byte")).to eq(Java::byte.java_class)
    expect(class_for_name.("boolean")).to eq(Java::boolean.java_class)
    expect(class_for_name.("short")).to eq(Java::short.java_class)
    expect(class_for_name.("char")).to eq(Java::char.java_class)
    expect(class_for_name.("int")).to eq(Java::int.java_class)
    expect(class_for_name.("long")).to eq(Java::long.java_class)
    expect(class_for_name.("float")).to eq(Java::float.java_class)
    expect(class_for_name.("double")).to eq(Java::double.java_class)
  end

  # NOTE: breaking change in 9.3 with JavaClass being deprecated
  it "should return Java class from JRuby class-path" do
    # Java::JavaClass.for_name('java_integration.fixtures.Reflector') is now a :
    # java.lang.Class.forName invocation
    expect(JRuby.load_java_class('java_integration.fixtures.Reflector')).to_not be nil
  end

  it "should also accept Java string argument" do
    str = 'java.util.Base64'.to_java
    expect(Java::JavaClass.for_name(str)).to_not be nil
  end
end

describe "Java classes with nested enums" do
  it "should allow access to the values() method on the enum" do
    expect(ClassWithEnums::Enums.values.map{|e|e.to_s}).to eq(["A", "B", "C"]);
  end
end

describe "A Java class" do
  describe "in a package with a leading underscore" do
    it "can be accessed directly using the Java:: prefix" do
      myclass = Java::java_integration.fixtures._funky.MyClass
      expect(myclass.new.foo).to eq("MyClass")
    end
  end
end

describe "A JavaClass wrapper around a java.lang.Class" do
  it "provides a nice String output for inspect" do
    myclass = java.lang.String.java_class
    expect( myclass ).to be_a java.lang.Class
    expect(myclass.to_s).to eq("java.lang.String")
  end
end

describe "A JavaClass with fields containing leading and trailing $" do
  it "should be accessible" do
    expect(JavaFields.send('$LEADING')).to eq("leading")
    expect(JavaFields.send('TRAILING$')).to eq(true)
  end
end

describe "A Java class with inner classes" do
  it "should define constants for constantable classes" do
    expect(InnerClasses.constants).to have_strings_or_symbols 'CapsInnerClass'
    expect(InnerClasses::CapsInnerClass.value).to eq(1)

    expect(InnerClasses::CapsInnerClass.constants).to have_strings_or_symbols "CapsInnerClass2"
    expect(InnerClasses::CapsInnerClass::CapsInnerClass2.value).to eq(1)

    expect(InnerClasses::CapsInnerClass.constants).to have_strings_or_symbols "CapsInnerInterface2"

    expect(InnerClasses::CapsInnerClass.constants).not_to have_strings_or_symbols 'lowerInnerClass2'
    expect(InnerClasses::CapsInnerClass.constants).not_to have_strings_or_symbols 'lowerInnerInterface2'

    expect(InnerClasses.constants).to have_strings_or_symbols 'CapsInnerInterface'

    expect(InnerClasses::CapsInnerInterface.constants).to have_strings_or_symbols "CapsInnerClass4"
    expect(InnerClasses::CapsInnerInterface::CapsInnerClass4.value).to eq(1)

    expect(InnerClasses::CapsInnerInterface.constants).to have_strings_or_symbols "CapsInnerInterface4"

    expect(InnerClasses::CapsInnerInterface.constants).not_to have_strings_or_symbols 'lowerInnerClass4'
    expect(InnerClasses::CapsInnerInterface.constants).not_to have_strings_or_symbols 'lowerInnerInterface4'
  end

  it "should define methods for lower-case classes" do
    expect(InnerClasses.methods).to have_strings_or_symbols 'lowerInnerClass'
    expect(InnerClasses::lowerInnerClass.value).to eq(1)
    expect(InnerClasses.lowerInnerClass.value).to eq(1)
    expect(InnerClasses.lowerInnerClass).to eq(InnerClasses::lowerInnerClass)

    expect(InnerClasses.lowerInnerClass.constants).to have_strings_or_symbols 'CapsInnerClass3'
    expect(InnerClasses.lowerInnerClass.constants).to have_strings_or_symbols 'CapsInnerInterface3'

    expect(InnerClasses.lowerInnerClass::CapsInnerClass3.value).to eq(1)

    expect(InnerClasses.lowerInnerClass.methods).to have_strings_or_symbols 'lowerInnerInterface3'
    expect(InnerClasses.lowerInnerClass.methods).to have_strings_or_symbols 'lowerInnerClass3'

    expect(InnerClasses.lowerInnerClass::lowerInnerClass3.value).to eq(1)
    expect(InnerClasses.lowerInnerClass.lowerInnerClass3.value).to eq(1)

    expect(InnerClasses.methods).to have_strings_or_symbols 'lowerInnerInterface'
    expect(InnerClasses.lowerInnerInterface).to eq(InnerClasses::lowerInnerInterface)

    expect(InnerClasses.lowerInnerInterface.constants).to have_strings_or_symbols 'CapsInnerClass5'
    expect(InnerClasses.lowerInnerInterface.constants).to have_strings_or_symbols 'CapsInnerInterface5'

    expect(InnerClasses.lowerInnerInterface::CapsInnerClass5.value).to eq(1)

    expect(InnerClasses.lowerInnerInterface.methods).to have_strings_or_symbols 'lowerInnerInterface5'
    expect(InnerClasses.lowerInnerInterface.methods).to have_strings_or_symbols 'lowerInnerClass5'

    expect(InnerClasses.lowerInnerInterface::lowerInnerClass5.value).to eq(1)
    expect(InnerClasses.lowerInnerInterface.lowerInnerClass5.value).to eq(1)
  end

  it "defines constant for public inner classes" do
    expect( java.awt.font.TextLayout.constants.map(&:to_sym) ).to include :CaretPolicy
    java.awt.font.TextLayout::CaretPolicy
  end

  it "does not define constants for non-public inner classes" do
    constants = InnerClasses.constants.map(&:to_sym)
    expect( constants ).to_not include :PackageInner
    expect( constants ).to_not include :ProtectedInner
    expect( constants ).to_not include :PrivateInner
  end

  it "allows to retrieve non-public inner classes" do
    # InnerClasses::PackageInner
    expect( InnerClasses.const_get :PackageInner ).to_not be nil
    class InnerClasses
      PACKAGE_INNER = PackageInner
    end
    expect( InnerClasses.constants ).to_not include :PackageInner
    expect( InnerClasses::PACKAGE_INNER ).to eql JavaUtilities.get_proxy_class('java_integration.fixtures.InnerClasses$PackageInner')

    expect( InnerClasses.const_get :PrivateInner ).to_not be nil
    expect( InnerClasses.const_get :ProtectedInner ).to_not be nil

    class InnerClasses
      PROTECTED_INNER = ProtectedInner
    end

    expect( InnerClasses::PROTECTED_INNER.const_get :Nested ).to_not be nil

    expect { InnerClasses::MissingInner }.to raise_error(NameError)
    begin
      InnerClasses.const_get :MissingInner
    rescue NameError => e
      expect( e.message ).to start_with 'uninitialized constant Java::Java_integrationFixtures::InnerClasses::MissingInner'
    else
      fail 'did not raise!'
    end
  end

  it "delegates const_missing" do # crucial for ActiveSupport::Dependencies
    const_missing = Module.instance_method(:const_missing)
    begin
      Module.module_eval do
        remove_method(:const_missing)
        def const_missing(name)
          @_const_missing_names ||= []
          @_const_missing_names << name
        end
      end

      InnerClasses::MissingInner
      InnerClasses::AnotherMissingInner
      InnerClasses::MissingInner

      missing_names = InnerClasses.instance_variable_get(:@_const_missing_names)
      expect( missing_names ).to eql [ :MissingInner, :AnotherMissingInner, :MissingInner ]

    ensure
      Module.module_eval do
        define_method :const_missing, const_missing
      end
    end
  end

  # jruby/jruby#5835 and ruboto/JRuby9K_POC#7
  it "allows const_missing on a Java class to trigger properly" do
    expect {
      InnerClasses::NonExistentClass
    }.to raise_error(NameError, "uninitialized constant Java::Java_integrationFixtures::InnerClasses::NonExistentClass")
  end

  describe "with static final fields of the same name" do
    it "defines a constant pointing at the field" do
      err = with_stderr_captured do
        expect(InnerClasses::ConflictsWithStaticFinalField.ok()).to be true
      end

      err.should be_empty
    end
  end
end
