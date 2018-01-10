require 'rspec'

describe "symbol encoding" do
  describe "for ASCII-compatible symbols" do
    it "should be US-ASCII" do
      expect(:foo.encoding.name).to eq("US-ASCII")
    end

    it "should be US-ASCII after converting to string" do
      expect(:foo.to_s.encoding.name).to eq("US-ASCII")
    end
  end

  describe "for bare symbols with ISO-8859-1 compatible non-ASCII codepoints" do
    it "should be UTF-8" do
      expect(:×.encoding.name).to eq("UTF-8")
    end

    it "should be UTF-8 after converting to string" do
      expect(:×.to_s.encoding.name).to eq("UTF-8")
    end

    it "should preserve characters after converting to string" do
      expect(:×.to_s).to eq("×")
    end

    it "should preserve characters when inspected" do
      expect(:×.inspect).to eq(":×")
    end
  end

  describe "for bare symbols with non-ISO-8859-1 codepoints" do
    it "should be UTF-8" do
      expect(:λ.encoding.name).to eq("UTF-8")
    end

    it "should be UTF-8 after converting to string" do
      expect(:λ.to_s.encoding.name).to eq("UTF-8")
    end

    it "should preserve characters after converting to string" do
      expect(:λ.to_s).to eq("λ")
    end

    it "should preserve characters when inspected" do
      expect(:λ.inspect).to eq(":λ")
    end
  end

  describe "for quoted symbols with ISO-8859-1 compatible non-ASCII codepoints" do
    it "should be UTF-8" do
      expect(:"×".encoding.name).to eq("UTF-8")
    end

    it "should be UTF-8 after converting to string" do
      expect(:"×".to_s.encoding.name).to eq("UTF-8")
    end

    it "should preserve characters after converting to string" do
      expect(:"×".to_s).to eq("×")
    end

    it "should preserve characters when inspected" do
      expect(:"×".inspect).to eq(":×")
    end
  end

  describe "for quoted symbols with non-ISO-8859-1 codepoints" do
    it "should be UTF-8" do
      expect(:"λ".encoding.name).to eq("UTF-8")
    end

    it "should be UTF-8 after converting to string" do
      expect(:"λ".to_s.encoding.name).to eq("UTF-8")
    end

    it "should preserve characters after converting to string" do
      expect(:"λ".to_s).to eq("λ")
    end

    it "should preserve characters when inspected" do
      expect(:"λ".inspect).to eq(":λ")
    end
  end

  it "symbol with accents should preserve accents when converted to string" do
    expect(:Renè.to_s).to eq("Renè")
  end

  it "symbol with accents should inspect to appropriate string" do
    expect(:Renè.inspect).to eq(":Renè")
  end

  it "symbol of lambda character should convert to string" do
    expect(:λ.to_s).to eq("λ")
  end

  it "symbol of lambda character should inspect properly" do
    expect(:λ.inspect).to eq(":λ")
  end
  
end

