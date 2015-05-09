require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel#freeze" do
  it "prevents self from being further modified" do
    o = mock('o')
    o.frozen?.should be_false
    o.freeze
    o.frozen?.should be_true
  end

  it "returns self" do
    o = Object.new
    o.freeze.should equal(o)
  end

  describe "on integers" do
    it "has no effect since they are already frozen" do
      1.frozen?.should be_true
      1.freeze

      bignum = bignum_value
      bignum.frozen?.should be_true
      bignum.freeze
    end
  end

  describe "on a Float" do
    it "has no effect since it is already frozen" do
      1.2.frozen?.should be_true
      1.2.freeze
    end
  end

  describe "on a Symbol" do
    ruby_version_is ""..."2.1" do
      it "actually freezes it" do
        :sym.frozen?.should be_false
        :sym.freeze
        :sym.frozen?.should be_true
      end
    end

    ruby_version_is "2.1" do
      it "has no effect since it is already frozen" do
        :sym.frozen?.should be_true
        :sym.freeze
      end
    end
  end

  describe "on true, false and nil" do
    ruby_version_is ""..."2.2" do
      it "actually freezes them" do
        true.frozen?.should be_false
        false.frozen?.should be_false
        nil.frozen?.should be_false

        # Test in a separate process so as to avoid polluting
        # the spec process with frozen true, false and nil.
        ruby_exe("print [true, false, nil].map { |o| o.freeze; o.frozen? }").should ==
          "[true, true, true]"
      end
    end

    ruby_version_is "2.2" do
      it "has no effect since they are already frozen" do
        nil.frozen?.should be_true
        true.frozen?.should be_true
        false.frozen?.should be_true

        nil.freeze
        true.freeze
        false.freeze
      end
    end
  end

  it "causes mutative calls to raise RuntimeError" do
    o = Class.new do
      def mutate; @foo = 1; end
    end.new
    o.freeze
    lambda {o.mutate}.should raise_error(RuntimeError)
  end

  it "causes instance_variable_set to raise RuntimeError" do
    o = Object.new
    o.freeze
    lambda {o.instance_variable_set(:@foo, 1)}.should raise_error(RuntimeError)
  end
end
