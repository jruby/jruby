describe "#tanh" do
  context "with infinity" do
    it "returns 1" do
      expect(Math.tanh(1.0/0.0)).to eq(1)
    end
  end
end

describe "#frexp" do
  context "with infinity" do
    before { @inf = 1.0 / 0 }
    it "should not raise error" do
      expect { Math.frexp(@inf) }.not_to raise_error
    end
    
    it "returns infinity for first element" do
      expect(Math.frexp(@inf).first).to eq(@inf)
    end
    
    it "returns 0 for last element" do
      expect(Math.frexp(@inf).last).to eq(0)
    end
  end
  
  context "with nan" do
    before { @nan = 0.0 / 0 }
      it "should not raise error" do
        expect { Math.frexp(@nan) }.not_to raise_error
      end
      
      it "returns nan for first element" do
        expect(Math.frexp(@nan).first).to be_nan
      end
      
      it "returns 0 for last element" do
        expect(Math.frexp(@nan).last).to eq(0)
      end
  end
end
