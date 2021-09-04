$LOAD_PATH.unshift File.dirname(__FILE__) + "/../../helpers/ir"

require 'edge_helpers'

import 'org.jruby.ir.util.DirectedGraph'
import 'org.jruby.ir.util.EdgeTypeIterator'
import 'java.util.NoSuchElementException'

describe "EdgeTypeIterable" do

  before do
    @graph = DirectedGraph.new
    @graph.addEdge(1, 2, "foo")
    @graph.addEdge(2, 3, "foo")
  end

  describe "hasNext" do

    context "edges of given type" do

      it "returns true if the iterator contains an edge of given type" do
        iterator = EdgeTypeIterator.new(@graph.edges(), "foo", false)
        expect(iterator.hasNext).to eq true
      end

      it "returns false if the iterator does not contain any edge of given type" do
        iterator = EdgeTypeIterator.new(@graph.edges(), "bar", false)
        expect(iterator.hasNext).to eq false
      end

    end

    context "edges not of given type" do

      it "returns true if the iterator contains an edge not of given type" do
        iterator = EdgeTypeIterator.new(@graph.edges(), "bar", true)
        expect(iterator.hasNext).to eq true
      end

      it "returns false if the iterator contains an edge of given type" do
        iterator = EdgeTypeIterator.new(@graph.edges(), "foo", true)
        expect(iterator.hasNext).to eq false
      end

    end

    context "when iterator type is null" do

      context "edges of given type" do

        it "returns true if the iterator contains an edge of type nil" do
          # add an edge of type nil
          @graph.addEdge(4,1,nil)
          iterator = EdgeTypeIterator.new(@graph.edges(), nil, false)
          expect(iterator.hasNext).to eq true
        end

        it "returns false if the iterator does not contain any edge of type nil" do
          iterator = EdgeTypeIterator.new(@graph.edges(), nil, false)
          expect(iterator.hasNext).to eq false
        end

      end

      context "edges not of given type" do

        it "returns true if the iterator contains an edge not of type nil" do
          iterator = EdgeTypeIterator.new(@graph.edges(), nil, true)
          expect(iterator.hasNext).to eq true
        end

        it "returns false if the iterator contains all edges of type nil" do
          # remove existing edges not of type nil
          @graph.removeEdge(1,2)
          @graph.removeEdge(2,3)
          # add an edge of type nil
          @graph.addEdge(4,1,nil)
          iterator = EdgeTypeIterator.new(@graph.edges(), nil, true)
          expect(iterator.hasNext).to eq false
        end

      end

    end

    context "when edge type is nil and iterator type is not nil" do

      it "returns true if the iterator contains an edge not of type nil" do
        # remove existing edges not of type nil
        @graph.removeEdge(1,2)
        @graph.removeEdge(2,3)
        # add an edge of type nil
        @graph.addEdge(4,1,nil)
        iterator = EdgeTypeIterator.new(@graph.edges(), "foo", true)
        expect(iterator.hasNext).to eq true
      end

      it "returns false if the iterator contains all edges not of type nil" do
        iterator = EdgeTypeIterator.new(@graph.edges(), "foo", true)
        expect(iterator.hasNext).to eq false
      end
    end

  end

  describe "next" do

    context "when the iterator has next edge" do

      it "returns the next edge" do
        iterator = EdgeTypeIterator.new(@graph.edges(), "foo", false)
        expect(iterator.next).to have_type("foo")
      end
    end

    context "when the iterator does not have next edge" do
      it "throws NoSuchElementException" do
        empty_graph = DirectedGraph.new
        iterator = EdgeTypeIterator.new(empty_graph.edges(), "foo", false)
        expect { iterator.next }.to raise_error NoSuchElementException
      end
    end
  end

  describe "remove" do

    it "throws UnsupportedOperationException exception" do
      iterator = EdgeTypeIterator.new(@graph.edges(), "foo", false)
      expect { iterator.remove }.to raise_error Java::JavaLang::UnsupportedOperationException
    end
  end

end
