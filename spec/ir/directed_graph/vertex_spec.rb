import 'org.jruby.ir.util.DirectedGraph'
import 'org.jruby.ir.util.Vertex'

describe "Vertex" do

  before do
    @graph = DirectedGraph.new
  end

  describe "Adding an edge from source to destination" do

    before do
      @source = Vertex.new(@graph, "foo", 1)
      @dest   = Vertex.new(@graph, "bar", 2)
    end

    it "adds outgoing edge to source" do
      @source.addEdgeTo(@dest)
      expect(@source.outDegree).to eq 1
    end

    it "adds incoming edge to destination" do
      @source.addEdgeTo(@dest)
      expect(@dest.inDegree).to eq 1
    end

    it "adds the edge to the graph containing source" do
      @source.addEdgeTo(@dest)
      expect(@graph.edges()).not_to be nil
    end

    it "sets edge type to null if is not provided" do
      @source.addEdgeTo(@dest)
      expect(@graph.edges().first.getType).to be nil
    end

    it "sets edge type to the given value if is provided" do
      @source.addEdgeTo(@dest, "foobar")
      expect(@graph.edges().first.getType).to eq "foobar"
    end

  end

  describe "Removing an outgoing edge from current vertex" do

    before do
      @source = Vertex.new(@graph, "foo", 1)
      @dest   = Vertex.new(@graph, "bar", 2)
      @source.addEdgeTo(@dest)
    end

    context "Destination of any one of the outgoing edges from the current vertex matched with given destination" do

      it "removes an edge from outgoing edges of the source vertex" do
        @source.removeEdgeTo(@dest)
        expect(@source.outDegree).to eq 0
      end

      it "removes an edge from incoming edges of the destination vertex" do
        @source.removeEdgeTo(@dest)
        expect(@dest.inDegree).to eq 0
      end

    end

    context "Destination of all of the outgoing edges from the current vertex doesn't match with given destination" do
      it "returns false" do
        non_existent_destination = Vertex.new(@graph, "baz", 3)
        expect(@source.removeEdgeTo(non_existent_destination)).to be false
      end
    end

  end

  describe "Remove all incoming edges" do

    before do
      @a = Vertex.new(@graph, "foo", 1)
      @b = Vertex.new(@graph, "bar", 2)
      @c = Vertex.new(@graph, "baz", 2)
      @b.addEdgeTo(@a)
      @c.addEdgeTo(@a)
    end

    it "removes all incoming edges to the vertex" do
      @a.removeAllIncomingEdges()
      expect(@a.inDegree).to eq 0
    end

  end

  describe "Remove all outgoing edges" do

    before do
      @a = Vertex.new(@graph, "foo", 1)
      @b = Vertex.new(@graph, "bar", 2)
      @c = Vertex.new(@graph, "baz", 2)
      @a.addEdgeTo(@b)
      @a.addEdgeTo(@c)
    end

    it "removes all outgoing edges from the vertex" do
      @a.removeAllOutgoingEdges()
      expect(@a.outDegree).to eq 0
    end

  end

  describe "Remove all edges" do

    before do
      @a = Vertex.new(@graph, "foo", 1)
      @b = Vertex.new(@graph, "bar", 2)
      @c = Vertex.new(@graph, "baz", 2)
      @a.addEdgeTo(@b)
      @a.addEdgeTo(@c)
      @c.addEdgeTo(@a)
      @b.addEdgeTo(@a)
    end

    it "removes all edges from the vertex" do
      @a.removeAllEdges()
      expect(@a.outDegree).to eq 0
      expect(@a.inDegree).to eq 0
    end

  end

end
