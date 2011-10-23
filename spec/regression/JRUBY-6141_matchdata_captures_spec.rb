describe "JRUBY-6141: Matchdata#captures" do

  before :all do
    "first, last".scan(Regexp.new('(first|last)')) do
      @firstmatch ||= Regexp.last_match
    end
    @lastmatch = Regexp.last_match
  end

  it "returns first value from Regexp.last_match after all String#scan iterations" do
    @firstmatch.captures[0].should == "first"
  end
  
  it "returns last value from Regexp.last_match after all String#scan iterations" do
    @lastmatch.captures[0].should == "last"
  end
end
