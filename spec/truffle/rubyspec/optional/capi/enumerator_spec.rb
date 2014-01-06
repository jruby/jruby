require File.expand_path('../spec_helper', __FILE__)

load_extension("enumerator")

describe "C-API Enumerator function" do
  before :each do
    @s = CApiEnumeratorSpecs.new
  end

  ruby_version_is "1.8.7" do
    describe "rb_enumeratorize" do
      before do
        @enumerable = [1, 2, 3]
      end

      ruby_version_is ""..."1.9" do
        it "constructs a new Enumerator for the given object, method and arguments" do
          enumerator = @s.rb_enumeratorize(@enumerable, :each, :arg1, :arg2)
          enumerator.class.should == Enumerable::Enumerator
        end
      end

      ruby_version_is "1.9" do
        it "constructs a new Enumerator for the given object, method and arguments" do
          enumerator = @s.rb_enumeratorize(@enumerable, :each, :arg1, :arg2)
          enumerator.class.should == Enumerator
        end
      end

      it "enumerates the given object" do
        enumerator = @s.rb_enumeratorize(@enumerable, :each)
        enumerated = []
        enumerator.each { |i| enumerated << i }
        enumerated.should == @enumerable
      end

      it "uses the given method for enumeration" do
        enumerator = @s.rb_enumeratorize(@enumerable, :awesome_each)
        @enumerable.should_receive(:awesome_each)
        enumerator.each {}
      end

      it "passes the given arguments to the enumeration method" do
        enumerator = @s.rb_enumeratorize(@enumerable, :each, :arg1, :arg2)
        @enumerable.should_receive(:each).with(:arg1, :arg2)
        enumerator.each {}
      end
    end
  end
end
