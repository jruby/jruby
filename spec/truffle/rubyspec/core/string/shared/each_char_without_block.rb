# -*- encoding: utf-8 -*-
require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe :string_each_char_without_block, :shared => true do
  it "returns an enumerator when no block given" do
    enum = "hello".send(@method)
    enum.should be_an_instance_of(enumerator_class)
    enum.to_a.should == ['h', 'e', 'l', 'l', 'o']
  end
end
