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
    expect(first_item).to eql @first_item
    expect(second_item).to eql @second_item
  end
end
