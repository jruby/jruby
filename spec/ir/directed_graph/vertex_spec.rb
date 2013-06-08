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
        fake   = Vertex.new(@graph, "baz", 3)
        expect(@source.removeEdgeTo(fake)).to be false
      end
    end

  end
end
