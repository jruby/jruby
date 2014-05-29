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

  ruby_version_is '' ... '1.9' do
    it "has no effect on immediate values" do
      [nil, true, false, 1, :sym].map {|o| o.freeze; o.frozen? }.should ==
        [false, false, false, false, false]
    end
  end

  ruby_version_is '1.9' do
    # 1.9 allows immediates to be frozen #1747. Test in a separate process so
    # as to avoid polluting the spec process with frozen immediates.
    it "freezes immediate values" do
      ruby_exe("print [nil, true, false, 1, :sym].map {|o| o.freeze; o.frozen? }").should ==
        "[true, true, true, true, true]"
    end
  end

  ruby_version_is "" ... "1.9" do
    it "causes mutative calls to raise TypeError" do
      o = Class.new do
        def mutate; @foo = 1; end
      end.new
      o.freeze
      lambda {o.mutate}.should raise_error(TypeError)
    end

    it "causes instance_variable_set to raise TypeError" do
      o = Object.new
      o.freeze
      lambda {o.instance_variable_set(:@foo, 1)}.should raise_error(TypeError)
    end
  end

  ruby_version_is "1.9" do
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
end
