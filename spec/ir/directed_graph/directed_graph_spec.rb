import 'org.jruby.ir.util.DirectedGraph'

# This is spec for Directed Graph Library

describe "Directed Graph Utility" do

  before do
    @graph = DirectedGraph.new
  end

  it "adds an edge to newly created graph" do
    @graph.edges.size.should be 0
    @graph.addEdge(1,2,'foo')
    @graph.addEdge(4,5,'bar')
    @graph.edges.size.should be 2
  end

  it "removes an existing edge from a graph" do
    @graph.addEdge(1,2,'foo')
    @graph.addEdge(4,5,'bar')
    @graph.removeEdge(4,5)
    @graph.edges.size.should be 1
    @graph.removeEdge(@graph.edges.to_a.last)
    @graph.edges.size.should be 0
  end

  it "does not delete a non-existent edge from the graph" do
    @graph.removeEdge(2,1)
    @graph.edges.size.should be 0
  end

  it "removes a vertex and its associated edges" do
    @graph.removeVertexFor(3)
    @graph.vertices.size.should be 0
    @graph.addEdge(1,2,'foo')
    @graph.addEdge(4,5,'bar')
    @graph.removeVertexFor(2)
    @graph.vertices.size.should be 3
    @graph.edges.size.should be 1
  end

  it "gives vertex for given data" do
    @graph.addEdge(1,2,'foo')
    @graph.vertexFor(2).getData().should be 2
  end

  it "creates a new vertex if it is not present" do
    @graph.vertexFor(100).getData().should be 100
  end

  it "finds already existing vertex" do
    @graph.findVertexFor(100).should be_nil
    @graph.addEdge(1,2,'foo')
    @graph.findVertexFor(1).getData().should be 1
  end

  it "gives correct size of graph" do
    @graph.removeEdge(1,2)
    @graph.size.should be 0
    @graph.addEdge(5,6,'baz')
    @graph.size.should be 2
    @graph.addEdge('foo','bar','baz')
    @graph.size.should be 4
  end

  it "gives all data in the graph" do
    @graph.allData.size.should be 0
    @graph.addEdge(1,2,'baz')
    @graph.allData.each do |key|
      @graph.findVertexFor(key).should_not be_nil
    end
    @graph.removeVertexFor(1)
    @graph.allData.each do |key|
      @graph.findVertexFor(key).should_not be_nil
    end
  end

  it "gives data in the graph in the order in which it was inserted" do
    @graph.getInorderData.to_a.size.should be 0
    @graph.vertexFor(1)
    @graph.getInorderData.to_a.should eq [1]
    @graph.addEdge('foo','bar','baz')
    @graph.getInorderData.to_a.should eq [1,'foo','bar']
    @graph.removeVertexFor('foo')
    @graph.getInorderData.to_a.should eq [1,'bar']
  end

end
