require 'date'

describe "java.lang.Object#compareTo" do
  it "returns the appropriate value when invoked on a to_java'd object" do
    # note: this bug only appeared for objects which were not represented internally with
    # RubyBignum, RubyFixnum, RubyFloat or RubyString since they override the compareTo in RubyBasicObject
    expect(Date.today.to_java.compareTo((Date.today - 1).to_java)).to eql(1)
    expect(Date.today.to_java.compareTo((Date.today).to_java)).to eql(0)
    expect(Date.today.to_java.compareTo((Date.today + 1).to_java)).to eql(-1)
  end
end