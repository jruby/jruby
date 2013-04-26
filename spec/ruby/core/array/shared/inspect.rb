require File.expand_path('../../fixtures/encoded_strings', __FILE__)

describe :array_inspect, :shared => true do
  it "returns a string" do
    [1, 2, 3].send(@method).should be_an_instance_of(String)
  end

  it "returns '[]' for an empty Array" do
    [].send(@method).should == "[]"
  end

  it "calls inspect on its elements and joins the results with commas" do
    items = Array.new(3) do |i|
      obj = mock(i.to_s)
      obj.should_receive(:inspect).and_return(i.to_s)
      obj
    end
    items.send(@method).should == "[0, 1, 2]"
  end

  it "represents a recursive element with '[...]'" do
    ArraySpecs.recursive_array.send(@method).should == "[1, \"two\", 3.0, [...], [...], [...], [...], [...]]"
    ArraySpecs.head_recursive_array.send(@method).should == "[[...], [...], [...], [...], [...], 1, \"two\", 3.0]"
    ArraySpecs.empty_recursive_array.send(@method).should == "[[...]]"
  end

  it "taints the result if the Array is non-empty and tainted" do
    [1, 2].taint.send(@method).tainted?.should be_true
  end

  it "does not taint the result if the Array is tainted but empty" do
    [].taint.send(@method).tainted?.should be_false
  end

  it "taints the result if an element is tainted" do
    ["str".taint].send(@method).tainted?.should be_true
  end

  ruby_version_is "1.9" do
    it "untrusts the result if the Array is untrusted" do
      [1, 2].untrust.send(@method).untrusted?.should be_true
    end

    it "does not untrust the result if the Array is untrusted but empty" do
      [].untrust.send(@method).untrusted?.should be_false
    end

    it "untrusts the result if an element is untrusted" do
      ["str".untrust].send(@method).untrusted?.should be_true
    end
  end

  ruby_version_is "1.9" do
    it "returns a US-ASCII string for an empty Array" do
      [].send(@method).encoding.should == Encoding::US_ASCII
    end

    it "copies the ASCII-compatible encoding of the result of inspecting the first element" do
      euc_jp = mock("euc_jp")
      euc_jp.should_receive(:inspect).and_return("euc_jp".encode!(Encoding::EUC_JP))

      utf_8 = mock("utf_8")
      utf_8.should_receive(:inspect).and_return("utf_8".encode!(Encoding::UTF_8))

      result = [euc_jp, utf_8].send(@method)
      result.encoding.should == Encoding::EUC_JP
      result.should == "[euc_jp, utf_8]".encode(Encoding::EUC_JP)
    end

    ruby_version_is "2.0" do
      it "raises if inspected result is not default external encoding" do
        utf_16be = mock("utf_16be")
        utf_16be.should_receive(:inspect).and_return("utf_16be".encode!(Encoding::UTF_16BE))

        lambda { [utf_16be].send(@method) }.should raise_error(Encoding::CompatibilityError)
      end
    end

    it "raises if inspecting two elements produces incompatible encodings" do
      utf_8 = mock("utf_8")
      utf_8.should_receive(:inspect).and_return("utf_8".encode!(Encoding::UTF_8))

      utf_16be = mock("utf_16be")
      utf_16be.should_receive(:inspect).and_return("utf_16be".encode!(Encoding::UTF_16BE))

      lambda { [utf_8, utf_16be].send(@method) }.should raise_error(Encoding::CompatibilityError)
    end
  end
end
