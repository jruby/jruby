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

  it "should remove a vertex and its associated edges" do
    @graph.removeVertexFor(3)
    @graph.edges.size.should == 2
    @graph.vertices.size.should == 3
    @graph.removeVertexFor(2)
    @graph.vertices.size.should == 2
  end

  it "should give vertex for given data" do
    @graph.vertexFor(2).getData().should == 2
  end

  it "should create a new vertex if it is not present" do
    @graph.vertexFor(100).getData().should == 100
  end

  it "should find already existing vertex" do
    @graph.findVertexFor(2).getData().should == 2
    @graph.findVertexFor(100).should == nil
  end

  it "should give correct size of graph" do
    @graph.removeEdge(1,2)
    @graph.size.should == 4         # Passes ?
    @graph.addEdge(5,6,'simple')
    @graph.size.should == 4         # Fails; Because size of graph is actually number of vertices according to implementation
  end

end
