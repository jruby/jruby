require File.expand_path('../../spec_helper', __FILE__)
require File.expand_path('../fixtures/match_operators', __FILE__)

ruby_version_is ""..."1.9.2" do
  describe "The !~ operator" do
    before :each do
      @obj = OperatorImplementor.new
    end
  
    it "evaluates as not(=~)" do
      expected = "hello world"
  
      opval = (@obj !~ expected)
      opval.should == !(@obj =~ expected)
    end
  end
end

ruby_version_is "1.9.2" do
  require File.expand_path('../fixtures/match_operators19', __FILE__)
  
  describe "The !~ operator" do
    before :each do
      @obj = OperatorImplementor.new
    end
  
    it "evaluates as a call to !~" do
      expected = "hello world"
  
      opval = (@obj !~ expected)
      methodval = @obj.send(:"!~", expected)
  
      opval.should == expected
      methodval.should == expected
    end
  end
end

describe "The =~ operator" do
  before :each do
    @impl = OperatorImplementor.new
  end

  it "calls the =~ method" do
    expected = "hello world"

    opval = (@obj =~ expected)
    methodval = @obj.send(:"=~", expected)

    opval.should == expected
    methodval.should == expected
  end
end
