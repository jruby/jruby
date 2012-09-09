require 'jruby'
require 'java'
require 'rspec'
import 'org.jruby.ir.util.DirectedGraph'
# This is spec for Directed Graph Library

describe "Directed Graph Utility" do
  it "should create object of DirectedGraph" do
    graph = DirectedGraph.new
    graph.class.should == Java::OrgJrubyIrUtil::DirectedGraph
  end
end
