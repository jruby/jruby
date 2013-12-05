require File.dirname(__FILE__) + "/../spec_helper"

describe "Classes that implement Collection" do
  before do
    @first_item = 'first'
    @second_item = 'second'
    @items = [@first_item, @second_item]
    @collection = Java::JavaUtil::ArrayList.new(@items)
  end

  it "should support multiple assignment" do
    first_item, second_item = @collection
    first_item.should eql @first_item
    second_item.should eql @second_item
  end
end
