$LOAD_PATH.unshift File.dirname(__FILE__) + "/../../helpers/ir"

require 'vertex_helpers'
require 'edge_helpers'

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
      @source.add_edge(:to => @dest)
    end

    it "adds outgoing edge to source" do
      expect(@source).to have_out_degree 1
    end

    it "adds incoming edge to destination" do
      expect(@dest).to have_in_degree 1
    end

    it "adds the edge to the graph containing source" do
      expect(@graph.edges()).not_to be nil
    end

    it "sets edge type to null if is not provided" do
      expect(@graph.edges().first).to have_type(nil)
    end

    it "sets edge type to the given value if is provided" do
      @source.remove_edge(:to => @dest)
      @source.add_edge(:to => @dest, :type => "foobar")
      expect(@graph.edges.first).to have_type("foobar")
    end

  end

  describe "Removing an outgoing edge from current vertex" do

    before do
      @source.add_edge(:to => @dest)
    end

    context "Destination of any one of the outgoing edges from the current vertex matched with given destination" do

      it "removes an edge from outgoing edges of the source vertex" do
        @source.remove_edge(:to => @dest)
        expect(@source).to have_out_degree 0
      end

      it "removes an edge from incoming edges of the destination vertex" do
        @source.remove_edge(:to => @dest)
        expect(@dest).to have_in_degree 0
      end

    end

    context "Destination of all of the outgoing edges from the current vertex doesn't match with given destination" do
      it "returns false" do
        non_existent_destination = Vertex.new(@graph, "baz", 3)
        expect(@source.remove_edge(:to => non_existent_destination)).to be false
      end
    end

  end

  describe "Remove all incoming edges" do

    before do
      @interim = Vertex.new(@graph, "interim", 3)
      @dest.add_edge(:to => @source)
      @interim.add_edge(:to => @source)
    end

    it "removes all incoming edges to the vertex" do
      @source.remove_edges(:direction => :in)
      expect(@source).to have_in_degree 0
    end

  end

  describe "Remove all outgoing edges" do

    before do
      @interim = Vertex.new(@graph, "interim", 3)
      @source.add_edge(:to => @dest)
      @source.add_edge(:to => @interim)
    end

    it "removes all outgoing edges from the vertex" do
      @source.remove_edges(:direction => :out)
      expect(@source).to have_out_degree 0
    end

  end

  describe "Remove all edges" do

    before do
      @interim = Vertex.new(@graph, "interim", 3)
      @source.add_edge(:to => @dest)
      @source.add_edge(:to => @interim)
      @dest.add_edge(:to => @source)
      @interim.add_edge(:to => @source)
    end

    it "removes all edges from the vertex" do
      @source.remove_edges
      expect(@source).to have_out_degree 0
      expect(@source).to have_in_degree 0
    end

  end

  describe "getOutGoingEdge" do

    before do
      @null_vertex = Vertex.new(@graph, "null", 3)
    end

    it "returns first outgoing edge from the vertex not of type 'null'" do
      @source.add_edge(:to => @dest, :type => "not_null")
      @source.add_edge(:to => @null_vertex, :type => nil)
      expect(@source.outgoing_edge).to have_type("not_null")
    end

    it "returns null when all outgoing edges from the vertex are of type 'null'" do
      @source.add_edge(:to => @dest)
      @source.add_edge(:to => @null_vertex, :type => nil)
      expect(@source.outgoing_edge).to be nil
    end
  end

  describe "getIncomingEdge" do

    before do
      @null_vertex = Vertex.new(@graph, "null", 3)
    end

    it "returns first incoming edge to the vertex not of type 'null'" do
      @source.add_edge(:to => @dest, :type => "not_null")
      @null_vertex.add_edge(:to => @dest, :type => nil)
      expect(@dest.incoming_edge).to have_type("not_null")
    end

    it "returns null when all incoming edges to the vertex are of type 'null'" do
      @source.add_edge(:to => @dest)
      @null_vertex.add_edge(:to => @dest, :type => nil)
      expect(@dest.incoming_edge).to be nil
    end
  end

  describe "getOutGoingEdgeOfType" do

    context "when the edge of given type exists" do
      it "returns first outgoing edge of the given type" do
        @source.add_edge(:to => @dest, :type => "baz")
        expect(@source.outgoing_edge(:type => "baz")).to have_type("baz")
      end
    end

    context "when the edge of given type does not exist" do
      it "returns null" do
        @source.add_edge(:to => @dest, :type => "foobarbaz")
        expect(@source.outgoing_edge(:type => "foo-bar-baz")).to be nil
      end
    end
  end

  describe "getIncomingEdgeOfType" do

    context "when the edge of given type exists" do
      it "returns first incoming edge of the given type" do
        @source.add_edge(:to => @dest, :type => "baz")
        expect(@dest.incoming_edge(:type => "baz")).to have_type("baz")
      end
    end

    context "when the edge of given type does not exist" do
      it "returns null" do
        @source.add_edge(:to => @dest, :type => "foobarbaz")
        expect(@dest.incoming_edge(:type => "foo-bar-baz")).to be nil
      end
    end
  end

  describe "getIncomingSourceData" do

    context "when there is atleast one incoming edge to the current vertex" do
      it "returns data of the source of that first incoming edge" do
        @source.add_edge(:to => @dest)
        expect(@dest.data(:direction => :in)).to eq "foo"
      end
    end

    context "when there is no incoming edge to the current vertex" do
      it "returns null" do
        @source.add_edge(:to => @dest)
        expect(@source.data(:direction => :in)).to be nil
      end
    end

  end

  describe "getIncomingSourceDataOfType" do

    context "when there is atleast one incoming edge to the current vertex of the given type" do
      it "returns data of the source of that first incoming edge of given type" do
        @source.add_edge(:to => @dest)
        expect(@dest.data(:direction => :in, :type => nil)).to eq "foo"
      end
    end

    context "when there is no incoming edge to the current vertex of given type" do
      it "returns null" do
        @source.add_edge(:to => @dest, :type => "foo")
        expect(@dest.incoming_edge(:type => nil)).to eq nil
      end
    end

  end

  describe "getOutgoingDestinationData" do

    context "when there is atleast one outgoing edge from the current vertex" do
      it "returns data of the destination of that first outgoing edge" do
        @source.add_edge(:to => @dest)
        expect(@source.data(:direction => :out)).to eq "bar"
      end
    end

    context "when there is no outgoing edge from the current vertex" do
      it "returns null" do
        @source.add_edge(:to => @dest)
        expect(@dest.data(:direction => :out)).to be nil
      end
    end

  end

  describe "getOutgoingDestinationDataOfType" do

    context "when there is atleast one outgoing edge from the current vertex of the given type" do
      it "returns data of the source of that first outgoing edge of given type" do
        @source.add_edge(:to => @dest)
        expect(@source.data(:direction => :out, :type => nil)).to eq "bar"
      end
    end

    context "when there is no outgoing edge from the current vertex of given type" do
      it "returns null" do
        @source.add_edge(:to => @dest, :type => "foo")
        expect(@source.data(:direction => :out, :type => nil)).to be nil
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
        @source.add_edge(:to => @dest)
        expect(@source.toString).to eq "foo:>[2]\n"
      end
    end

    context "when vertex has many outgoing edges" do
      it "returns string representation of the vertex" do
        @source.add_edge(:to => @dest)
        @source.add_edge(:to => @interim)
        expect(["foo:>[2,3]\n", "foo:>[3,2]\n"]).to include @source.toString
      end
    end

    context "when vertex has only one incoming edge" do
      it "returns string representation of the vertex" do
        @source.add_edge(:to => @dest)
        expect(@dest.toString).to eq "bar:<[1]\n"
      end
    end

    context "when vertex has many incoming edges" do
      it "returns string representation of the vertex" do
        @source.add_edge(:to => @dest)
        @interim.add_edge(:to => @dest)
        expect(["bar:<[1,3]\n", "bar:<[3,1]\n"]).to include @dest.toString
      end
    end

    context "when vertex has both incoming and outgoing edges" do
      it "returns string representation of the vertex" do
        @source.add_edge(:to => @interim)
        @interim.add_edge(:to => @dest)
        expect(@interim.toString).to eq "interim:>[2], <[1]\n"
      end
    end

  end
end
