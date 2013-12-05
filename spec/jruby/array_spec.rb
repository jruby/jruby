# JRuby adds optimized versions of #any?, #all? and #find to Array which only delegate to Enumerable if Array#each has
# been overridden.  This spec ensures we don't regress on any of the customizations.

class ArrayExtender < Array
  def each
    yield "eachElem"
  end
end

array_extender = ArrayExtender.new
array_extender << "arrayElem"

describe "Array" do
  it "uses the #each method's override for #any? if one exists" do
    array_extender.any? { |elem| elem.should == "eachElem" }
  end

  it "uses the #each method's override for #all? if one exists" do
    array_extender.all? { |elem| elem.should == "eachElem" }
  end

  it "uses the #each method's override for #find? if one exists" do
    array_extender.find { |elem| elem.should == "eachElem" }
  end
end
