# Enumerable#tally had a Java-based block that did not clear callInfo, potentially leaving CALL_KEYWORD_EMPTY bit stuck
# for a subsequent call.
#
# jruby/jruby#8382

class TallyEnumWithEmptyKwargs
  include Enumerable
  def each(&block)
    block.call(**{})
  end
end

def kwarg_method_after_tally(kw:); end

describe "Enumerable#to_a" do
  it "should clear callInfo when invoking its internal block" do
    TallyEnumWithEmptyKwargs.new.to_a
    kwarg_method_after_tally(kw: 1)
  end
end