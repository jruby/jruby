import 'org.jruby.ir.util.Edge'
import 'org.jruby.ir.util.Vertex'
import 'org.jruby.ir.util.DirectedGraph'

describe "Edge" do

  before do
    @graph = DirectedGraph.new
  end

  describe "toString" do
    context "When edge type is not null" do
      it "represents edge with type" do
        edge  = Edge.new(Vertex.new(@graph, "foo", 1), Vertex.new(@graph, "bar", 2), "baz")
        expect(edge.toString).to eq "<1 --> 2> (baz)"
      end
    end

    context "When edge type is null" do
      it "represents edge without type" do
        edge  = Edge.new(Vertex.new(@graph, "foo", 1), Vertex.new(@graph, "bar", 2), nil)
        expect(edge.toString).to eq "<1 --> 2>"
      end
    end
  end
end
