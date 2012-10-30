# -*- encoding: ascii -*-
describe :io_gets_ascii, :shared => true do
  describe "with ASCII separator" do
    before :each do
      @name = tmp("gets_specs.txt")
      touch(@name, "wb") { |f| f.print "this is a test\xFFtesty\ntestier" }

      File.open(@name, "rb") { |f| @data = f.gets("\xFF") }
    end

    after :each do
      rm_r @name
    end

    ruby_version_is ""..."1.9" do
      it "returns the separator's number representation" do
        @data.should == "this is a test\377"
      end
    end

    ruby_version_is "1.9" do
      it "returns the separator's character representation" do
        @data.should == "this is a test\xFF"
      end
    end
  end
end
