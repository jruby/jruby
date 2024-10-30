# Enumerable#to_h had a Java-based block that did not clear callInfo, potentially leaving CALL_KEYWORD_EMPTY bit stuck
# for a subsequent call.
#
# jruby/jruby#8382

class ToHEnumWithEmptyKwargs
  include Enumerable
  def each(&block)
    block.call(1, 2, **{})
  end
end

def kwarg_method_after_to_h(kw:); end

describe "Enumerable#to_h" do
  it "should clear callInfo when invoking its internal block" do
    ToHEnumWithEmptyKwargs.new.to_a
    kwarg_method_after_to_h(kw: 1)
  end
end