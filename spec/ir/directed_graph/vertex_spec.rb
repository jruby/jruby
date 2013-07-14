import 'org.jruby.ir.util.DirectedGraph'
import 'org.jruby.ir.util.Vertex'

describe "Vertex" do

  before do
    @graph = DirectedGraph.new
    @source = Vertex.new(@graph, "foo", 1)
    @dest   = Vertex.new(@graph, "bar", 2)
  end

  describe "Adding an edge from source to destination" do

    before do
      @source.addEdgeTo(@dest)
    end

    it "adds outgoing edge to source" do
      expect(@source.outDegree).to eq 1
    end

    it "adds incoming edge to destination" do
      expect(@dest.inDegree).to eq 1
    end

    it "adds the edge to the graph containing source" do
      expect(@graph.edges()).not_to be nil
    end

    it "sets edge type to null if is not provided" do
      expect(@graph.edges().first.getType).to be nil
    end

    it "sets edge type to the given value if is provided" do
      @source.removeEdgeTo(@dest)
      @source.addEdgeTo(@dest, "foobar")
      expect(@graph.edges.first.getType).to eq "foobar"
    end

  end

  describe "Removing an outgoing edge from current vertex" do

    before do
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
      @interim = Vertex.new(@graph, "interim", 3)
      @dest.addEdgeTo(@source)
      @interim.addEdgeTo(@source)
    end

    it "removes all incoming edges to the vertex" do
      @source.removeAllIncomingEdges()
      expect(@source.inDegree).to eq 0
    end

  end

  describe "Remove all outgoing edges" do

    before do
      @interim = Vertex.new(@graph, "interim", 3)
      @source.addEdgeTo(@dest)
      @source.addEdgeTo(@interim)
    end

    it "removes all outgoing edges from the vertex" do
      @source.removeAllOutgoingEdges()
      expect(@source.outDegree).to eq 0
    end

  end

  describe "Remove all edges" do

    before do
      @interim = Vertex.new(@graph, "interim", 3)
      @source.addEdgeTo(@dest)
      @source.addEdgeTo(@interim)
      @dest.addEdgeTo(@source)
      @interim.addEdgeTo(@source)
    end

    it "removes all edges from the vertex" do
      @source.removeAllEdges()
      expect(@source.outDegree).to eq 0
      expect(@source.inDegree).to eq 0
    end

  end

  describe "getOutGoingEdge" do

    before do
      @null_vertex = Vertex.new(@graph, "null", 3)
    end

    it "returns first outgoing edge from the vertex not of type 'null'" do
      @source.addEdgeTo(@dest, "not_null")
      @source.addEdgeTo(@null_vertex, nil)
      expect(@source.getOutgoingEdge.getType).to_not be nil
      expect(@source.getOutgoingEdge.getType).to eq "not_null"
    end

    it "returns null when all outgoing edges from the vertex are of type 'null'" do
      @source.addEdgeTo(@dest)
      @source.addEdgeTo(@null_vertex, nil)
      expect(@source.getOutgoingEdge).to be nil
    end
  end

  describe "getIncomingEdge" do

    before do
      @null_vertex = Vertex.new(@graph, "null", 3)
    end

    it "returns first incoming edge to the vertex not of type 'null'" do
      @source.addEdgeTo(@dest, "not_null")
      @null_vertex.addEdgeTo(@dest, nil)
      expect(@dest.getIncomingEdge.getType).to_not be nil
      expect(@dest.getIncomingEdge.getType).to eq "not_null"
    end

    it "returns null when all incoming edges to the vertex are of type 'null'" do
      @source.addEdgeTo(@dest)
      @null_vertex.addEdgeTo(@dest, nil)
      expect(@dest.getIncomingEdge).to be nil
    end
  end

  describe "getOutGoingEdgeOfType" do

    context "when the edge of given type exists" do
      it "returns first outgoing edge of the given type" do
        @source.addEdgeTo(@dest, "baz")
        expect(@source.getOutgoingEdgeOfType("baz").getType).to eq "baz"
      end
    end

    context "when the edge of given type does not exist" do
      it "returns null" do
        @source.addEdgeTo(@dest, "foobarbaz")
        expect(@source.getOutgoingEdgeOfType("foo-bar-baz")).to be nil
      end
    end
  end

  describe "getIncomingEdgeOfType" do

    context "when the edge of given type exists" do
      it "returns first incoming edge of the given type" do
        @source.addEdgeTo(@dest, "baz")
        expect(@dest.getIncomingEdgeOfType("baz").getType).to eq "baz"
      end
    end

    context "when the edge of given type does not exist" do
      it "returns null" do
        @source.addEdgeTo(@dest, "foobarbaz")
        expect(@dest.getIncomingEdgeOfType("foo-bar-baz")).to be nil
      end
    end
  end

  describe "getIncomingSourceData" do

    context "when there is atleast one incoming edge to the current vertex" do
      it "returns data of the source of that first incoming edge" do
        @source.addEdgeTo(@dest)
        expect(@dest.getIncomingSourceData).to eq "foo"
      end
    end

    context "when there is no incoming edge to the current vertex" do
      it "returns null" do
        @source.addEdgeTo(@dest)
        expect(@source.getIncomingSourceData).to be nil
      end
    end

  end

  describe "getIncomingSourceDataOfType" do

    context "when there is atleast one incoming edge to the current vertex of the given type" do
      it "returns data of the source of that first incoming edge of given type" do
        @source.addEdgeTo(@dest)
        expect(@dest.getIncomingSourceDataOfType(nil)).to eq "foo"
      end
    end

    context "when there is no incoming edge to the current vertex of given type" do
      it "returns null" do
        @source.addEdgeTo(@dest, "foo")
        expect(@dest.getIncomingEdgeOfType(nil)).to be nil
      end
    end

  end

  describe "getOutgoingDestinationData" do

    context "when there is atleast one outgoing edge from the current vertex" do
      it "returns data of the destination of that first outgoing edge" do
        @source.addEdgeTo(@dest)
        expect(@source.getOutgoingDestinationData).to eq "bar"
      end
    end

    context "when there is no outgoing edge from the current vertex" do
      it "returns null" do
        @source.addEdgeTo(@dest)
        expect(@dest.getOutgoingDestinationData).to be nil
      end
    end

  end

  describe "getOutgoingDestinationDataOfType" do

    context "when there is atleast one outgoing edge from the current vertex of the given type" do
      it "returns data of the source of that first outgoing edge of given type" do
        @source.addEdgeTo(@dest)
        expect(@source.getOutgoingDestinationDataOfType(nil)).to eq "bar"
      end
    end

    context "when there is no outgoing edge from the current vertex of given type" do
      it "returns null" do
        @source.addEdgeTo(@dest, "foo")
        expect(@source.getOutgoingDestinationDataOfType(nil)).to be nil
      end
    end

  end

  describe "toString" do

    before do
      @interim = Vertex.new(@graph, "interim", 3)
    end

    context "when vertex has no edges" do
      it "returns string representation of the vertex" do
        expect(@source.toString).to eq "foo:\n"
      end
    end

    context "when vertex has only one outgoing edge" do
      it "returns string representation of the vertex" do
        @source.addEdgeTo(@dest)
        expect(@source.toString).to eq "foo:>[2]\n"
      end
    end

    context "when vertex has many outgoing edges" do
      it "returns string representation of the vertex" do
        @source.addEdgeTo(@dest)
        @source.addEdgeTo(@interim)
        expect(@source.toString).to eq "foo:>[2,3]\n"
      end
    end

    context "when vertex has only one incoming edge" do
      it "returns string representation of the vertex" do
        @source.addEdgeTo(@dest)
        expect(@dest.toString).to eq "bar:<[1]\n"
      end
    end

    context "when vertex has many incoming edges" do
      it "returns string representation of the vertex" do
        @source.addEdgeTo(@dest)
        @interim.addEdgeTo(@dest)
        expect(@dest.toString).to eq "bar:<[1,3]\n"
      end
    end

    context "when vertex has both incoming and outgoing edges" do
      it "returns string representation of the vertex" do
        @source.addEdgeTo(@interim)
        @interim.addEdgeTo(@dest)
        expect(@interim.toString).to eq "interim:>[2], <[1]\n"
      end
    end

  end
end
