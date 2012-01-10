require 'java'

describe "JRUBY-6324: random seed for srand is not initialized properly" do
  it "initializes initial seed for PRNG" do
    rt = Java.org.jruby.embed.ScriptingContainer.new
    rt.run_scriptlet("srand").should > 0
  end

  it "initializes initial seed for PRNG" do
    rt = Java.org.jruby.embed.ScriptingContainer.new
    id1, id2 = rt.run_scriptlet("n = 2**128; srand(n); [n.object_id, srand.object_id]")
    id1.should == id2
  end
end
