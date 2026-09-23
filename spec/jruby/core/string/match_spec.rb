# jruby/jruby#3267
# String passed to String#match was not following proper channels to compile to Regexp.
# As a result, encoding was not negotiated properly.
describe "A UTF-8 string matched against a US-ASCII string" do
  it "compiles to regexp successfully" do
    result = nil
    expect(
        lambda {result = "".force_encoding('US-ASCII').match("Període\\ de\\ retorn".force_encoding('UTF-8'))}
    ).not_to raise_error

    expect(result).to eq(nil)
  end
end
