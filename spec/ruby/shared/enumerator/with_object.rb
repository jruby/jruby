require File.expand_path('../../../spec_helper', __FILE__)

describe :enum_with_object, :shared => true do
  it "returns an enumerator when not given a block" do
    [].to_enum.send(@method, '').should be_an_instance_of(enumerator_class)
  end

  it "returns the given object when given a block" do
    object = [].to_enum.send(@method, 'wadus') {|i, o| o = o + o}
    object.should == 'wadus'
  end

  it "iterates over the array adding the given object" do
    expected = ''
    %w|wadus wadus|.each.send(@method, ' ') {|e, o| expected += e + o}

    expected.should == 'wadus wadus '
  end
end
