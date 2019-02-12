describe org.jruby.util.ByteList do
  describe "append(byte[], int, int)" do
    it "throws AssertionError on invalid start or length" do
      bytelist = org.jruby.util.ByteList.new
      more_bytes = "foo".bytes.to_java(:byte)
      expect { bytelist.append(more_bytes, 0, -1) }.to raise_error(java.lang.AssertionError)
      expect { bytelist.append(more_bytes, 0, 4) }.to raise_error(java.lang.AssertionError)
      expect { bytelist.append(more_bytes, 1, 3) }.to raise_error(java.lang.AssertionError)
      expect { bytelist.append(more_bytes, -1, 0) }.to raise_error(java.lang.AssertionError)
    end

    it "does not throw for valid start and length" do
      bytelist = org.jruby.util.ByteList.new
      more_bytes = "foo".bytes.to_java(:byte)
      expect { bytelist.append(more_bytes, 0, 3) }.not_to raise_error
      expect { bytelist.append(more_bytes, 1, 1) }.not_to raise_error
      expect { bytelist.append(more_bytes, 1, 2) }.not_to raise_error
      expect { bytelist.append(more_bytes, 0, 0) }.not_to raise_error
      expect { bytelist.append(more_bytes, 2, 0) }.not_to raise_error
      expect { bytelist.append(more_bytes, 3, 0) }.not_to raise_error  # this is the case that was broken
    end
  end
end

describe Dir do
  it "does not crash if a glob pattern ends in a curly brace" do
    expect { Dir.glob("foo{bar}") }.not_to raise_error
    expect { Dir["foo{bar}"] }.not_to raise_error
    # make sure it also works when the pattern string is not an interned literal
    expect { Dir.glob("foo" + "{bar}") }.not_to raise_error
    expect { Dir["foo" + "{bar}"] }.not_to raise_error
  end
end
