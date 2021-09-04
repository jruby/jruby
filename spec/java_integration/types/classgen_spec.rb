require File.dirname(__FILE__) + "/../spec_helper"
require 'jruby/core_ext'

java_import "java.io.FilterReader"
java_import "java.io.BufferedReader"

describe "A Ruby subclass of a Java concrete class with custom methods" do
  before :all do
    @my_reader_class = Class.new(java.io.Reader) do
      def initialize(list)
          super()
          @start=1
          @calls = list
      end

      configure_java_class methods: :explicit

      # no signature
      def read(cbuf, off, len)
        str = "ztest\nreaders\n"
        chars = str[@start..(@start+len)]
        return -1 if chars.length == 0
        chars.each_char.each_with_index do |c, i|
            cbuf[off+i] = c
        end
        @start += chars.length
        return chars.length
      end
      java_signature "void close() throws IOException"
      def close
        @calls << :close
      end
      # non-abstract, no signature, java override
      def markSupported
          @calls << :mark
          false
      end
      # plain ruby
      def java_invisible(a)
          @calls << [:java_invisible, a]
          100.1
      end
      
      # new method exposed to java
      java_signature "int java_visible(java.io.Reader)"
      def java_visible(r)
          @calls << [:java_visible, r]
          100.1
      end
      
      java_field "java.io.Reader readerField"
    end
    
    @my_filter_class = Class.new(FilterReader) do
      def initialize(parent_clz, list)
          super(@child = parent_clz.new(list))
          @calls = list
      end
      java_signature "boolean markSupported()"
      def markSupported
          @calls << :mark_support
          super
          @calls << :mark_support_end
          false
      end
    end
    @my_reader_class.new [] # generate java proxy
    @my_reader_class.become_java! # create field reader
    @my_filter_class.new @my_reader_class, [] # generate java proxy
    @my_filter_class.become_java! # create field reader
  end
  
  def getClass(javaish)
      # avoid jruby trying to outsmart us, we want what java sees
      java.lang.Class.forName("java.lang.Object").getMethod("getClass").invoke(javaish)
  end
  
  it "generates normal overloads usable by java" do
    lst = []
    my_reader = @my_reader_class.new lst
    buf_read = BufferedReader.new(my_reader)
    
    expect(buf_read.read_line).to eq("test")
    expect(buf_read.read_line).to eq("readers")
    expect(buf_read.read_line).to eq(nil)
    expect(buf_read.read_line).to eq(nil)
    buf_read.close
    expect(lst).to eq([:close])
  end
  
  it "generates new methods usable by ruby & java" do

    lst = []
    my_reader = @my_reader_class.new lst
    my_filter = @my_filter_class.new @my_reader_class, lst
    
    
    expect(my_reader.java_invisible(:test1)).to eq(100.1)
    expect(my_reader.java_visible(:test2)).to eq(100.1)
    
    jc = getClass(my_reader)
    found = jc.methods.find_all{|x|x.name.include? "java_"}
    foundstr = found.map(&:to_s).sort
    expect(foundstr.length).to eq(1)
    expect(foundstr.first).to match(/public int [a-zA-Z0-9\.\$_]+\.java_visible\(java\.io\.Reader\)/)
    
    expect(found.first.invoke(my_reader.to_java, my_filter.to_java(java.io.Reader))).to eq(100)
    expect(lst).to eq([[:java_invisible, :test1], [:java_visible, :test2], [:java_visible, my_filter.to_java]])
  end
  
  
  it "generates new fields usable by ruby & java" do

    lst = []
    my_reader = @my_reader_class.new lst
    my_filter = @my_filter_class.new @my_reader_class, lst
    my_reader.readerField = my_filter
    
    expect(my_reader.readerField).to eq(my_filter)
    
    found = getClass(my_reader).get_field("readerField")
    expect(found).not_to eq(nil)
    expect(found.to_s).to match(/public java\.io\.Reader [a-zA-Z0-9\.\$_]+\.readerField/)
    
    expect(found.get(my_reader)).to eq(my_filter.to_java)
  end
  
  it "routes methods through proper proxies" do

    lst = []
    my_reader = @my_reader_class.new lst
    my_filter = @my_filter_class.new @my_reader_class, lst
    
    # ruby calls java calls ruby (via java proxy)
    my_filter.markSupported
    
    expect(lst).to eq([:mark_support, :mark, :mark_support_end])
  end
end


describe "A Ruby subclass of a Java concrete class with custom methods and annotaiton via #java_signature" do
  class JavaProxyClassWithAnnotatedMethods < java.io.FilterReader
    java_signature("@java_integration.fixtures.EveryTypeAnnotations.Annotated("+
      "astr=\"Hello\", abyte=0xde, ashort=0xEF_FF, anint=0xFFff_EeeE, along=0xFFFF_EEEE_0000_9999,"+
      "afloat=3.5, adouble=1024.1024, abool=true, anbool=false, achar='?',"+
      "anenum=java.lang.annotation.RetentionPolicy.RUNTIME, aClass=java.lang.String.java_class,"+
      "Darray={@jakarta.annotation.Resource(description=\"first\"), @jakarta.annotation.Resource(description=\"second\")})"+
      " void foo()")
    def foo; end

    become_java!
  end

  it "has hex-set values" do
    output = Java::java_integration.fixtures.EveryTypeAnnotations.decodeAnnotatedMethods(ClassWithAnnotatedMethods3)["foo"].to_a
    easy_out = output[0..-2]
    arry = output[-1]
    expect(easy_out).to eq(["Hello", -34, -4097,-4370, -18769007044199, 3.5, 1024.1024,true, false,'?'.ord, java.lang.annotation.RetentionPolicy::RUNTIME, java.lang.String.java_class.to_java])
    expect(arry).to_not be_nil
expect(arry.map  &:description).to eq(%w{first second})
  end
end


