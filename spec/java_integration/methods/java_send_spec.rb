require 'java'

describe "A Java object's java_send method" do
  before :each do
    @list = java.util.ArrayList.new
    @integer = java.lang.Integer.new(1)
  end

  it "works with name only for no-arg methods" do
    @list.java_send(:toString).should == "[]"
    java.lang.System.java_send(:currentTimeMillis).class.should == Fixnum
  end

  it "works with name plus empty arg list for no-arg methods" do
    @list.java_send(:toString, []).should == "[]"
    java.lang.System.java_send(:currentTimeMillis, []).class.should == Fixnum
  end

  it "works with a signature" do
    @list.java_send :add, [Java::int, java.lang.Object], 0, 'foo'
    @list.to_s.should == "[foo]"

    java_home = java.lang.System.java_send(:getProperty, [java.lang.String], 'java.home')
    java_home.should == java.lang.System.getProperty('java.home')

    str = java.lang.StringBuilder.new
    str.java_send :append, [Java::long], 1234567890
    str.to_s.should == '1234567890'
    str.java_send :append, [java.lang.String], " str1"
    str.to_s.should == '1234567890 str1'
    str.java_send :append, [java.lang.CharSequence], " str2"
    str.to_s.should == '1234567890 str1 str2'
    str.java_send :append, [Java::char[], Java::int, Java::int], " str3".to_java.to_char_array, 0, 4
    str.to_s.should == '1234567890 str1 str2 str'

    buf = java::lang::StringBuffer.new(' buf ')
    begin
      str.java_send :append, [Java::JavaLang::String], buf
    rescue TypeError
    else; fail end
    str.java_send :append, [java.lang.CharSequence.java_class], buf
    str.to_s.should == '1234567890 str1 str2 str buf '
  end

  it "works with package-level classes" do
    array = Java::int[16].new
    array[1] = 10; array[2] = 20

    buffer = java.nio.IntBuffer.wrap array # returns a Java::JavaNio::HeapIntBuffer
    buffer.java_send(:get, [ Java::int ], 1).should == 10
    buffer.java_send(:get).should == 0
    buffer.java_send(:get).should == 10
    buffer.java_send(:get, []).should == 20
  end

  it "works with private classes" do
    array = Java::int[16].new
    array[1] = 10; array[2] = 20

    map = java.util.HashMap.new
    key_type = java.lang.String.java_class
    val_type = java.lang.Number.java_class

    map = java.util.Collections.checkedMap(map, key_type, val_type) # returns a private CheckedMap instance
    map.java_send(:clear)
    map.java_send(:put, [ Java::JavaLang::Object, Java::JavaLang::Object ], '1', 1.to_java)
    map.java_send(:get, [ Java::JavaLang::Object ], '').should == nil
    map.java_send(:get, [ Java::JavaLang::Object ], '1').should == 1
  end

  it "raises NameError if the method can't be found" do
    lambda do
      @list.java_send :foobar
    end.should raise_error(NameError)

    lambda do
      @list.java_send :add, [Java::long, java.lang.Object], 0, 'foo'
    end.should raise_error(NameError)

    lambda do
      java.lang.System.java_send :foobar
    end.should raise_error(NameError)

    lambda do
      java.lang.System.java_send :getProperty, [Java::long, Java::long], 0, 0
    end.should raise_error(NameError)
  end

  it "raises ArgumentError if type count doesn't match arg count" do
    lambda do
      @list.java_send :add, [Java::int, java.lang.Object], 0, 'foo', 'bar'
    end.should raise_error(ArgumentError)

    lambda do
      java.lang.System.java_send :getProperty, [java.lang.String], "foo", "bar"
    end.should raise_error(ArgumentError)
  end
end