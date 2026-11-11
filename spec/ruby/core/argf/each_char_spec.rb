require_relative '../../spec_helper'

describe "ARGF.each_char" do
  before :each do
    @file1_name = fixture __FILE__, "file1.txt"
    @file2_name = fixture __FILE__, "file2.txt"

    @chars = []
    File.read(@file1_name).each_char { |c| @chars << c }
    File.read(@file2_name).each_char { |c| @chars << c }
  end

  it "yields each char of all streams to the passed block" do
    argf [@file1_name, @file2_name] do
      chars = []
      @argf.each_char { |c| chars << c }
      chars.should == @chars
    end
  end

  it "yields a multibyte character as one String" do
    multibyte_name = fixture __FILE__, "multibyte.txt"
    argf [multibyte_name] do
      @argf.set_encoding(Encoding::UTF_8)
      @argf.send(@method).to_a.should == ["é", "t", "é", "€"]
    end
  end

  it "returns self when passed a block" do
    argf [@file1_name, @file2_name] do
      @argf.each_char {}.should.equal?(@argf)
    end
  end

  it "returns an Enumerator when passed no block" do
    argf [@file1_name, @file2_name] do
      enum = @argf.each_char
      enum.should.instance_of?(Enumerator)

      chars = []
      enum.each { |c| chars << c }
      chars.should == @chars
    end
  end

  describe "when no block is given" do
    it "returns an Enumerator" do
      argf [@file1_name, @file2_name] do
        enum = @argf.each_char
        enum.should.instance_of?(Enumerator)

        chars = []
        enum.each { |c| chars << c }
        chars.should == @chars
      end
    end

    describe "returned Enumerator" do
      describe "size" do
        it "should return nil" do
          argf [@file1_name, @file2_name] do
            @argf.each_char.size.should == nil
          end
        end
      end
    end
  end
end

describe "ARGF.each_char when reading $stdin" do
  before :each do
    @r, @w = IO.pipe("UTF-8")
    @w.write "hé"
    @w.close
    @stdin = $stdin
    $stdin = @r
  end

  after :each do
    $stdin = @stdin
    @r.close unless @r.closed?
  end

  it "yields a multibyte character as one String" do
    argf ['-'] do
      @argf.each_char.to_a.should == ["h", "é"]
    end
  end
end
