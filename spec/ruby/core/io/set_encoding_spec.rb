require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "IO#set_encoding" do
    before :each do
      @name = tmp('io_set_encoding.txt')
      @io = new_io @name
      @io.set_encoding(nil, nil)
    end

    after :each do
      @io.close unless @io.closed?
      rm_r @name
    end

    it "sets the external encoding when passed an Encoding argument" do
      @io.set_encoding(Encoding::UTF_8)
      @io.external_encoding.should == Encoding::UTF_8
      @io.internal_encoding.should be_nil
    end

    it "sets the external and internal encoding when passed two Encoding arguments" do
      @io.set_encoding(Encoding::UTF_8, Encoding::UTF_16BE)
      @io.external_encoding.should == Encoding::UTF_8
      @io.internal_encoding.should == Encoding::UTF_16BE
    end

    it "sets the external encoding when passed the name of an Encoding" do
      @io.set_encoding("utf-8")
      @io.external_encoding.should == Encoding::UTF_8
      @io.internal_encoding.should be_nil
    end

    ruby_bug "http://redmine.ruby-lang.org/issues/5568", "1.9.3" do
      it "ignores the internal encoding if the same as external when passed Encoding objects" do
        @io.set_encoding(Encoding::UTF_8, Encoding::UTF_8)
        @io.external_encoding.should == Encoding::UTF_8
        @io.internal_encoding.should be_nil
      end
    end

    it "ignores the internal encoding if the same as external when passed encoding names separanted by ':'" do
      @io.set_encoding("utf-8:utf-8")
      @io.external_encoding.should == Encoding::UTF_8
      @io.internal_encoding.should be_nil
    end

    it "sets the external and internal encoding when passed the names of Encodings separated by ':'" do
      @io.set_encoding("utf-8:utf-16be")
      @io.external_encoding.should == Encoding::UTF_8
      @io.internal_encoding.should == Encoding::UTF_16BE
    end

    it "sets the external and internal encoding when passed two String arguments" do
      @io.set_encoding("utf-8", "utf-16be")
      @io.external_encoding.should == Encoding::UTF_8
      @io.internal_encoding.should == Encoding::UTF_16BE
    end

    it "calls #to_str to convert an abject to a String" do
      obj = mock("io_set_encoding")
      obj.should_receive(:to_str).and_return("utf-8:utf-16be")
      @io.set_encoding(obj)
      @io.external_encoding.should == Encoding::UTF_8
      @io.internal_encoding.should == Encoding::UTF_16BE
    end

    it "calls #to_str to convert the second argument to a String" do
      obj = mock("io_set_encoding")
      obj.should_receive(:to_str).at_least(1).times.and_return("utf-16be")
      @io.set_encoding(Encoding::UTF_8, obj)
      @io.external_encoding.should == Encoding::UTF_8
      @io.internal_encoding.should == Encoding::UTF_16BE
    end
  end
end
