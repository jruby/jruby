require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is '2.3' do
  describe "Array#dig" do

    it "returns #at with one arg" do
      ['a'].dig(0).should == 'a'
      ['a'].dig(1).should be_nil
    end

    it "recurses array elements" do
      a = [ [ 1, [2, '3'] ] ]
      a.dig(0, 0).should == 1
      a.dig(0, 1, 1).should == '3'
      a.dig(0, -1, 0).should == 2
    end

    it "raises without any args" do
      lambda { [10].dig() }.should raise_error(ArgumentError)
    end

    it "calls #dig on the result of #at with the remaining arguments" do
      h = [[nil, [nil, nil, 42]]]
      h[0].should_receive(:dig).with(1, 2).and_return(42)
      h.dig(0, 1, 2).should == 42
    end

  end
end
