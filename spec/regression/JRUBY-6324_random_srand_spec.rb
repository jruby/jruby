require 'java'

describe "JRUBY-6324: random seed for srand is not initialized properly" do
  it "initializes initial seed for PRNG" do
    rt = Java.org.jruby.embed.ScriptingContainer.new(org.jruby.embed.LocalContextScope::SINGLETHREAD)
    expect(rt.run_scriptlet("srand")).to be > 0
  end

  it "initializes initial seed for PRNG" do
    rt = Java.org.jruby.embed.ScriptingContainer.new(org.jruby.embed.LocalContextScope::SINGLETHREAD)
    id1, id2 = rt.run_scriptlet("n = 2**128; srand(n); [n.object_id, srand.object_id]")

    # You cannot do id1.should == id2 here because the Objects are from
    # different runtime!  Cross runtime comparison confuses rspec and it tries
    # to raise an expectation exception, but the RaiseException is also
    # confusing about runtime so it can be thrown.
    #
    # Cross runtime object passing is evil.  I should implement Channel for
    # this purpose.
    expect(String.new(id1.to_s)).to eq(String.new(id2.to_s))
  end
end
