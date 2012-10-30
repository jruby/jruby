require File.expand_path('../spec_helper', __FILE__)

describe "Function with variadic arguments" do
  [ 0, 127, -128, -1 ].each do |i|
    it "call variadic with (:char (#{i})) argument" do
      buf = FFI::Buffer.new :long_long
      FFISpecs::LibTest.pack_varargs(buf, "c", :char, i)
      buf.get_int64(0).should == i
    end
  end

  [ 0, 0x7f, 0x80, 0xff ].each do |i|
    it "call variadic with (:uchar (#{i})) argument" do
      buf = FFI::Buffer.new :long_long
      FFISpecs::LibTest.pack_varargs(buf, "C", :uchar, i)
      buf.get_int64(0).should == i
    end
  end

  [ 0, 1.234567, 9.87654321 ].each do |v|
    it "call variadic with (:float (#{v})) argument" do
      buf = FFI::Buffer.new :long_long
      FFISpecs::LibTest.pack_varargs(buf, "f", :float, v.to_f)
      buf.get_float64(0).should == v
    end
  end

  [ 0, 1.234567, 9.87654321 ].each do |v|
    it "call variadic with (:double (#{v})) argument" do
      buf = FFI::Buffer.new :long_long
      FFISpecs::LibTest.pack_varargs(buf, "f", :double, v.to_f)
      buf.get_float64(0).should == v
    end
  end

  def self.verify(p, off, v)
    if v.kind_of?(Float)
      p.get_float64(off).should == v
    else
      p.get_int64(off).should == v
    end
  end

  FFISpecs::Varargs::PACK_VALUES.keys.each do |t1|
    FFISpecs::Varargs::PACK_VALUES.keys.each do |t2|
      FFISpecs::Varargs::PACK_VALUES.keys.each do |t3|
        FFISpecs::Varargs::PACK_VALUES[t1].each do |v1|
          FFISpecs::Varargs::PACK_VALUES[t2].each do |v2|
            FFISpecs::Varargs::PACK_VALUES[t3].each do |v3|
              fmt = "#{t1}#{t2}#{t3}"
              params = [ FFISpecs::Varargs::TYPE_MAP[t1], v1, FFISpecs::Varargs::TYPE_MAP[t2], v2, FFISpecs::Varargs::TYPE_MAP[t3], v3 ]
              it "call(#{fmt}, #{params.join(',')})" do
                buf = FFI::Buffer.new :long_long, 3
                FFISpecs::LibTest.pack_varargs(buf, fmt, *params)
                verify(buf, 0, v1)
                verify(buf, 8, v2)
                verify(buf, 16, v3)
              end
            end
          end
        end
      end
    end
  end
end
