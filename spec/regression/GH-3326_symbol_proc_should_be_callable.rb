require 'rspec'

describe "sym.to_proc in define_method" do
  it "should not raise an ArgumentError" do
    expect do
      # Weirdly using the proc generated will end up changing
      # something about the block which would in turn call a
      # java version of yield in SymbolProcBody which was passing
      # in its block param instead of itself for signature arity check.
      s = :to_s.to_proc
      define_method :gh3326, s
      s[1]
    end.not_to raise_error
  end
end
