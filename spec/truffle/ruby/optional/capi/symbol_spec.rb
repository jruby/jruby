require File.expand_path('../spec_helper', __FILE__)

load_extension('symbol')

describe "C-API Symbol function" do
  before :each do
    @s = CApiSymbolSpecs.new
  end

  describe "rb_intern" do
    it "converts a string to a symbol, uniquely" do
      @s.rb_intern("test_symbol").should == :test_symbol
      @s.rb_intern_c_compare("test_symbol", :test_symbol).should == true
    end
  end

  describe "rb_id2name" do
    it "converts a symbol to a C char array" do
      @s.rb_id2name(:test_symbol).should == "test_symbol"
    end
  end

  ruby_version_is "1.9" do
    describe "rb_id2str" do
      it "converts a symbol to a Ruby string" do
        @s.rb_id2str(:test_symbol).should == "test_symbol"
      end

      it "creates a string with the same encoding as the symbol" do
        str = "test_symbol".encode(Encoding::UTF_16LE)
        @s.rb_id2str(str.to_sym).encoding.should == Encoding::UTF_16LE
      end
    end
  end

  describe "rb_is_const_id" do
    it "returns true given a const-like symbol" do
      @s.rb_is_const_id(:Foo).should == true
    end

    it "returns false given an ivar-like symbol" do
      @s.rb_is_const_id(:@foo).should == false
    end

    it "returns false given a cvar-like symbol" do
      @s.rb_is_const_id(:@@foo).should == false
    end

    it "returns false given an undecorated symbol" do
      @s.rb_is_const_id(:foo).should == false
    end
  end

  describe "rb_is_instance_id" do
    it "returns false given a const-like symbol" do
      @s.rb_is_instance_id(:Foo).should == false
    end

    it "returns true given an ivar-like symbol" do
      @s.rb_is_instance_id(:@foo).should == true
    end

    it "returns false given a cvar-like symbol" do
      @s.rb_is_instance_id(:@@foo).should == false
    end

    it "returns false given an undecorated symbol" do
      @s.rb_is_instance_id(:foo).should == false
    end
  end

  describe "rb_is_class_id" do
    it "returns false given a const-like symbol" do
      @s.rb_is_class_id(:Foo).should == false
    end

    it "returns false given an ivar-like symbol" do
      @s.rb_is_class_id(:@foo).should == false
    end

    it "returns true given a cvar-like symbol" do
      @s.rb_is_class_id(:@@foo).should == true
    end

    it "returns false given an undecorated symbol" do
      @s.rb_is_class_id(:foo).should == false
    end
  end
end
