require File.expand_path('../spec_helper', __FILE__)

describe "Union" do
  before do
    @u = FFISpecs::LibTest::TestUnion.new
  end

  it "places all the fields at offset 0" do
    FFISpecs::LibTest::TestUnion.members.all? { |m| FFISpecs::LibTest::TestUnion.offset_of(m) == 0 }.should be_true
  end

  FFISpecs::LibTest::Types.each do |k, type|
    it "correctly aligns/writes a #{type[0]} value" do
      @u[type[1]] = type[2]
      if k == 'f32' or k == 'f64'
        (@u[type[1]] - FFISpecs::LibTest.send("union_align_#{k}", @u.to_ptr)).abs.should < 0.00001
      else
        @u[type[1]].should == FFISpecs::LibTest.send("union_align_#{k}", @u.to_ptr)
      end
    end
  end

  FFISpecs::LibTest::Types.each do |k, type|
    it "reads a #{type[0]} value from memory" do
      @u = FFISpecs::LibTest::TestUnion.new(FFISpecs::LibTest.send("union_make_union_with_#{k}", type[2]))
      if k == 'f32' or k == 'f64'
        (@u[type[1]] - type[2]).abs.should < 0.00001
      else
        @u[type[1]].should == type[2]
      end
    end
  end

  it "returns a size equals to the size of the biggest field" do
    FFISpecs::LibTest::TestUnion.size.should == FFISpecs::LibTest.union_size
  end
end
