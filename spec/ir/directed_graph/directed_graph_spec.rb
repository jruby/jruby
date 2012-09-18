require 'jruby'
require 'java'
require 'rspec'
import 'org.jruby.ir.util.DirectedGraph'
# This is spec for Directed Graph Library

describe "Directed Graph Utility" do

  before do
    @graph = DirectedGraph.new
    @graph.addEdge(1,2,'simple')
    @graph.addEdge(2,3,'simple')
    @graph.addEdge(3,4,'simple')
    @graph.addEdge(4,1,'simple')
  end

  it "should add an edge to newly created graph" do
    @graph.edges.size.should == 4
    @graph.addEdge(4,5,'simple')
    @graph.edges.size.should == 5
  end

  it "should remove an existing edge from a graph" do
    @graph.edges.size.should == 4
    @graph.removeEdge(1,2)
    @graph.edges.size.should == 3
  end

  it "should not delete a non-existent edge from the graph" do
    @graph.edges.size.should == 4
    @graph.removeEdge(2,1)
    @graph.edges.size.should == 4
  end

end
