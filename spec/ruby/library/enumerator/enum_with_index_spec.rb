require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do
  require File.expand_path('../../../fixtures/enumerator/classes', __FILE__)
  require 'enumerator'

  describe "Enumerator#enum_with_index" do
    it "returns an enumerator of the receiver with an iteration of each_with_index" do
      a = []
      enum = EnumSpecs::Numerous.new.enum_with_index
      enum.should be_an_instance_of(enumerator_class)
      enum.each { |e| a << e }
      a.should == [[2, 0], [5, 1], [3, 2], [6, 3], [1, 4], [4, 5]]
    end
  end
end
