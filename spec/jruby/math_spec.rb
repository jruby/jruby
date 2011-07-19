describe "#tanh" do
  context "with infinity" do
    it "returns 1" do
      Math.tanh(1.0/0.0).should == 1
    end
  end
end

describe "#frexp" do
  context "with infinity" do
    before { @inf = 1.0 / 0 }
    it "should not raise error" do
      lambda { Math.frexp(@inf) }.should_not raise_error
    end
    
    it "returns infinity for first element" do
      Math.frexp(@inf).first.should == @inf
    end
    
    it "returns 0 for last element" do
      Math.frexp(@inf).last.should == 0
    end
  end
  
  context "with nan" do
    before { @nan = 0.0 / 0 }
      it "should not raise error" do
        lambda { Math.frexp(@nan) }.should_not raise_error
      end
      
      it "returns nan for first element" do
        Math.frexp(@nan).first.nan?.should be_true
      end
      
      it "returns 0 for last element" do
        Math.frexp(@nan).last.should == 0
      end
  end
end