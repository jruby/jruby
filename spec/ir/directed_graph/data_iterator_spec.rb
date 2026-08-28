import 'org.jruby.ir.util.DirectedGraph'
import 'org.jruby.ir.util.DataIterator'
import 'java.util.NoSuchElementException'

describe "DataIterator" do

  before do
    @graph = DirectedGraph.new
    @graph.addEdge(1, 2, "foo")
    @graph.addEdge(2, 3, "foo")
  end

  # hasNext method doesn't use the source or destination of iterator at all
  # So specs are not written for iterator having source set to false
  describe "hasNext" do

    context "edges of given type" do

      it "returns true if the iterator contains an edge of given type" do
        iterator = DataIterator.new(@graph.edges(), "foo", true, false)
        expect(iterator.hasNext).to eq true
      end

      it "returns false if the iterator does not contain any edge of given type" do
        iterator = DataIterator.new(@graph.edges(), "bar", true, false)
        expect(iterator.hasNext).to eq false
      end

    end

    context "edges not of given type" do

      it "returns true if the iterator contains an edge not of given type" do
        iterator = DataIterator.new(@graph.edges(), "bar", true, true)
        expect(iterator.hasNext).to eq true
      end

      it "returns false if the iterator contains an edge of given type" do
        iterator = DataIterator.new(@graph.edges(), "foo", true, true)
        expect(iterator.hasNext).to eq false
      end

    end

    context "when iterator type is null" do

      context "edges of given type" do

        it "returns true if the iterator contains an edge of type nil" do
          # add an edge of type nil
          @graph.addEdge(4,1,nil)
          iterator = DataIterator.new(@graph.edges(), nil, true, false)
          expect(iterator.hasNext).to eq true
        end

        it "returns false if the iterator does not contain any edge of type nil" do
          iterator = DataIterator.new(@graph.edges(), nil, true, false)
          expect(iterator.hasNext).to eq false
        end

      end

      context "edges not of given type" do

        it "returns true if the iterator contains an edge not of type nil" do
          iterator = DataIterator.new(@graph.edges(), nil, true, true)
          expect(iterator.hasNext).to eq true
        end

        it "returns false if the iterator contains all edges of type nil" do
          # remove existing edges not of type nil
          @graph.removeEdge(1,2)
          @graph.removeEdge(2,3)
          # add an edge of type nil
          @graph.addEdge(4,1,nil)
          iterator = DataIterator.new(@graph.edges(), nil, true, true)
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
        iterator = DataIterator.new(@graph.edges(), "foo", true, true)
        expect(iterator.hasNext).to eq true
      end

      it "returns false if the iterator contains all edges not of type nil" do
        iterator = DataIterator.new(@graph.edges(), "foo", true, true)
        expect(iterator.hasNext).to eq false
      end
    end

  end

  describe "next" do

    context "when the iterator has next edge" do

      context "when asked for data of source vertex" do
        it "returns the data of the source of the edge" do
          iterator = DataIterator.new(@graph.edges(), "foo", true, false)
          expect([1, 2]).to include iterator.next
        end
      end

      context "when asked for data of destination vertex" do
        it "returns the data of the destination of the edge" do
          iterator = DataIterator.new(@graph.edges(), "foo", false, false)
          expect([2, 3]).to include iterator.next
        end
      end
    end

    context "when the iterator does not have next edge" do

      before do
        @empty_graph = DirectedGraph.new
      end

      it "throws NoSuchElementException for source data" do
        iterator = DataIterator.new(@empty_graph.edges(), "foo", true, false)
        expect { iterator.next }.to raise_error NoSuchElementException
      end

      it "throws NoSuchElementException for destination data" do
        iterator = DataIterator.new(@empty_graph.edges(), "foo", false, false)
        expect { iterator.next }.to raise_error NoSuchElementException
      end
    end
  end

  describe "remove" do

    it "throws UnsupportedOperationException exception" do
      iterator = DataIterator.new(@graph.edges(), "foo", true, false)
      expect { iterator.remove }.to raise_error Java::JavaLang::UnsupportedOperationException
    end
  end

end
