require File.expand_path('../spec_helper', __FILE__)

describe "A library with no enum defined" do
  it "returns nil when asked for an enum" do
    FFISpecs::TestEnum0.enum_type(:foo).should == nil
  end
end

describe "An untagged enum" do
  it "constants can be used as function parameters and return value" do
    FFISpecs::TestEnum1.test_untagged_enum(:c1).should == 0
    FFISpecs::TestEnum1.test_untagged_enum(:c2).should == 1
    FFISpecs::TestEnum1.test_untagged_enum(:c3).should == 2
    FFISpecs::TestEnum1.test_untagged_enum(:c4).should == 3
    FFISpecs::TestEnum1.test_untagged_enum(:c5).should == 42
    FFISpecs::TestEnum1.test_untagged_enum(:c6).should == 43
    FFISpecs::TestEnum1.test_untagged_enum(:c7).should == 44
    FFISpecs::TestEnum1.test_untagged_enum(:c8).should == 45
    FFISpecs::TestEnum1.test_untagged_enum(:c9).should == 42
    FFISpecs::TestEnum1.test_untagged_enum(:c10).should == 43
    FFISpecs::TestEnum1.test_untagged_enum(:c11).should == 4242
    FFISpecs::TestEnum1.test_untagged_enum(:c12).should == 4243
    FFISpecs::TestEnum1.test_untagged_enum(:c13).should == 42
    FFISpecs::TestEnum1.test_untagged_enum(:c14).should == 4242
    FFISpecs::TestEnum1.test_untagged_enum(:c15).should == 424242
    FFISpecs::TestEnum1.test_untagged_enum(:c16).should == 42424242
  end
end

describe "A tagged typedef enum" do
  it "is accessible through its tag" do
    FFISpecs::TestEnum3.enum_type(:enum_type1).should_not == nil
    FFISpecs::TestEnum3.enum_type(:enum_type2).should_not == nil
    FFISpecs::TestEnum3.enum_type(:enum_type3).should_not == nil
    FFISpecs::TestEnum3.enum_type(:enum_type4).should_not == nil
  end

  it "contains enum constants" do
    FFISpecs::TestEnum3.enum_type(:enum_type1).symbols.length.should == 4
    FFISpecs::TestEnum3.enum_type(:enum_type2).symbols.length.should == 4
    FFISpecs::TestEnum3.enum_type(:enum_type3).symbols.length.should == 4
    FFISpecs::TestEnum3.enum_type(:enum_type4).symbols.length.should == 4
  end

  it "constants can be used as function parameters and return value" do
    FFISpecs::TestEnum3.test_tagged_typedef_enum1(:c1).should == :c1
    FFISpecs::TestEnum3.test_tagged_typedef_enum1(:c2).should == :c2
    FFISpecs::TestEnum3.test_tagged_typedef_enum1(:c3).should == :c3
    FFISpecs::TestEnum3.test_tagged_typedef_enum1(:c4).should == :c4
    FFISpecs::TestEnum3.test_tagged_typedef_enum2(:c5).should == :c5
    FFISpecs::TestEnum3.test_tagged_typedef_enum2(:c6).should == :c6
    FFISpecs::TestEnum3.test_tagged_typedef_enum2(:c7).should == :c7
    FFISpecs::TestEnum3.test_tagged_typedef_enum2(:c8).should == :c8
    FFISpecs::TestEnum3.test_tagged_typedef_enum3(:c9).should == :c9
    FFISpecs::TestEnum3.test_tagged_typedef_enum3(:c10).should == :c10
    FFISpecs::TestEnum3.test_tagged_typedef_enum3(:c11).should == :c11
    FFISpecs::TestEnum3.test_tagged_typedef_enum3(:c12).should == :c12
    FFISpecs::TestEnum3.test_tagged_typedef_enum4(:c13).should == :c13
    FFISpecs::TestEnum3.test_tagged_typedef_enum4(:c14).should == :c14
    FFISpecs::TestEnum3.test_tagged_typedef_enum4(:c15).should == :c15
    FFISpecs::TestEnum3.test_tagged_typedef_enum4(:c16).should == :c16
  end
end

describe "All enums" do
  it "have autonumbered constants when defined with names only" do
    FFISpecs::TestEnum1.enum_value(:c1).should == 0
    FFISpecs::TestEnum1.enum_value(:c2).should == 1
    FFISpecs::TestEnum1.enum_value(:c3).should == 2
    FFISpecs::TestEnum1.enum_value(:c4).should == 3

    FFISpecs::TestEnum3.enum_value(:c1).should == 0
    FFISpecs::TestEnum3.enum_value(:c2).should == 1
    FFISpecs::TestEnum3.enum_value(:c3).should == 2
    FFISpecs::TestEnum3.enum_value(:c4).should == 3
  end

  it "can have an explicit first constant and autonumbered subsequent constants" do
    FFISpecs::TestEnum1.enum_value(:c5).should == 42
    FFISpecs::TestEnum1.enum_value(:c6).should == 43
    FFISpecs::TestEnum1.enum_value(:c7).should == 44
    FFISpecs::TestEnum1.enum_value(:c8).should == 45

    FFISpecs::TestEnum3.enum_value(:c5).should == 42
    FFISpecs::TestEnum3.enum_value(:c6).should == 43
    FFISpecs::TestEnum3.enum_value(:c7).should == 44
    FFISpecs::TestEnum3.enum_value(:c8).should == 45
  end

  it "can have a mix of explicit and autonumbered constants" do
    FFISpecs::TestEnum1.enum_value(:c9).should  == 42
    FFISpecs::TestEnum1.enum_value(:c10).should == 43
    FFISpecs::TestEnum1.enum_value(:c11).should == 4242
    FFISpecs::TestEnum1.enum_value(:c12).should == 4243

    FFISpecs::TestEnum3.enum_value(:c9).should  == 42
    FFISpecs::TestEnum3.enum_value(:c10).should == 43
    FFISpecs::TestEnum3.enum_value(:c11).should == 4242
    FFISpecs::TestEnum3.enum_value(:c12).should == 4243
  end

  it "can have all its constants explicitely valued" do
    FFISpecs::TestEnum1.enum_value(:c13).should == 42
    FFISpecs::TestEnum1.enum_value(:c14).should == 4242
    FFISpecs::TestEnum1.enum_value(:c15).should == 424242
    FFISpecs::TestEnum1.enum_value(:c16).should == 42424242

    FFISpecs::TestEnum3.enum_value(:c13).should == 42
    FFISpecs::TestEnum3.enum_value(:c14).should == 4242
    FFISpecs::TestEnum3.enum_value(:c15).should == 424242
    FFISpecs::TestEnum3.enum_value(:c16).should == 42424242
  end

  it "return the constant corresponding to a specific value" do
    enum = FFISpecs::TestEnum3.enum_type(:enum_type1)
    enum[0].should == :c1
    enum[1].should == :c2
    enum[2].should == :c3
    enum[3].should == :c4

    enum = FFISpecs::TestEnum3.enum_type(:enum_type2)
    enum[42].should == :c5
    enum[43].should == :c6
    enum[44].should == :c7
    enum[45].should == :c8

    enum = FFISpecs::TestEnum3.enum_type(:enum_type3)
    enum[42].should == :c9
    enum[43].should == :c10
    enum[4242].should == :c11
    enum[4243].should == :c12

    enum = FFISpecs::TestEnum3.enum_type(:enum_type4)
    enum[42].should == :c13
    enum[4242].should == :c14
    enum[424242].should == :c15
    enum[42424242].should == :c16
  end
end
