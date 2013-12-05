# -*- encoding: utf-8 -*-
require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/encode', __FILE__)

with_feature :encoding do
  describe "String#encode" do
    before :each do
      @external = Encoding.default_external
      @internal = Encoding.default_internal
    end

    after :each do
      Encoding.default_external = @external
      Encoding.default_internal = @internal
    end

    it_behaves_like :string_encode, :encode

    describe "when passed no options" do
      it "returns a copy when Encoding.default_internal is nil" do
        Encoding.default_internal = nil
        str = "あ"
        str.encode.should_not equal(str)
      end

      it "returns a copy for a ASCII-only String when Encoding.default_internal is nil" do
        Encoding.default_internal = nil
        str = "abc"
        str.encode.should_not equal(str)
      end
    end

    describe "when passed to encoding" do
      it "returns a copy when passed the same encoding as the String" do
        str = "あ"
        str.encode(Encoding::UTF_8).should_not equal(str)
      end

      it "round trips a String" do
        str = "abc def".force_encoding Encoding::US_ASCII
        str.encode("utf-32be").encode("ascii").should == "abc def"
      end
    end

    describe "when passed options" do
      it "returns a copy when Encoding.default_internal is nil" do
        Encoding.default_internal = nil
        str = "あ"
        str.encode(:invalid => :replace).should_not equal(str)
      end
    end

    describe "when passed to, from" do
      it "returns a copy when both encodings are the same" do
        str = "あ"
        str.encode("utf-8", "utf-8").should_not equal(str)
      end
    end

    describe "when passed to, options" do
      it "returns a copy when the destination encoding is the same as the String encoding" do
        str = "あ"
        str.encode(Encoding::UTF_8, :undef => :replace).should_not equal(str)
      end
    end

    describe "when passed to, from, options" do
      it "returns a copy when both encodings are the same" do
        str = "あ"
        str.encode("utf-8", "utf-8", :invalid => :replace).should_not equal(str)
      end
    end
  end

  describe "String#encode!" do
    before :each do
      @external = Encoding.default_external
      @internal = Encoding.default_internal
    end

    after :each do
      Encoding.default_external = @external
      Encoding.default_internal = @internal
    end

    it_behaves_like :string_encode, :encode!

    it "raises a RuntimeError when called on a frozen String" do
      lambda { "foo".freeze.encode!("euc-jp") }.should raise_error(RuntimeError)
    end

    # http://redmine.ruby-lang.org/issues/show/1836
    it "raises a RuntimeError when called on a frozen String when it's a no-op" do
      lambda { "foo".freeze.encode!("utf-8") }.should raise_error(RuntimeError)
    end

    describe "when passed no options" do
      it "returns self when Encoding.default_internal is nil" do
        Encoding.default_internal = nil
        str = "あ"
        str.encode!.should equal(str)
      end

      it "returns self for a ASCII-only String when Encoding.default_internal is nil" do
        Encoding.default_internal = nil
        str = "abc"
        str.encode!.should equal(str)
      end
    end

    describe "when passed to encoding" do
      it "returns self" do
        str = "abc"
        result = str.encode!(Encoding::BINARY)
        result.encoding.should equal(Encoding::BINARY)
        result.should equal(str)
      end
    end

    describe "when passed to, from" do
      it "returns self" do
        str = "ああ"
        result = str.encode!("euc-jp", "utf-8")
        result.encoding.should equal(Encoding::EUC_JP)
        result.should equal(str)
      end
    end
  end
end
