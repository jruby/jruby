# -*- encoding: utf-8 -*-
describe :string_encode, :shared => true do
  describe "when passed no options" do
    it "transcodes to Encoding.default_internal when set" do
      Encoding.default_internal = Encoding::UTF_8
      str = "\xA4\xA2".force_encoding Encoding::EUC_JP
      str.send(@method).should == "あ"
    end

    it "transcodes a 7-bit String despite no generic converting being available" do
      lambda do
        Encoding::Converter.new Encoding::Emacs_Mule, Encoding::ASCII_8BIT
      end.should raise_error(Encoding::ConverterNotFoundError)

      Encoding.default_internal = Encoding::Emacs_Mule
      str = "\x79".force_encoding Encoding::ASCII_8BIT

      str.send(@method).should == "y".force_encoding(Encoding::ASCII_8BIT)
    end

    it "raises an Encoding::ConverterNotFoundError when no conversion is possible" do
      Encoding.default_internal = Encoding::Emacs_Mule
      str = "\x80".force_encoding Encoding::ASCII_8BIT
      lambda { str.send(@method) }.should raise_error(Encoding::ConverterNotFoundError)
    end
  end

  describe "when passed to encoding" do
    it "accepts a String argument" do
      str = "\xA4\xA2".force_encoding Encoding::EUC_JP
      str.send(@method, "utf-8").should == "あ"
    end

    it "calls #to_str to convert the object to an Encoding" do
      enc = mock("string encode encoding")
      enc.should_receive(:to_str).and_return("utf-8")

      str = "\xA4\xA2".force_encoding Encoding::EUC_JP
      str.send(@method, enc).should == "あ"
    end

    it "transcodes to the passed encoding" do
      str = "\xA4\xA2".force_encoding Encoding::EUC_JP
      str.send(@method, Encoding::UTF_8).should == "あ"
    end

    it "transcodes a 7-bit String despite no generic converting being available" do
      lambda do
        Encoding::Converter.new Encoding::Emacs_Mule, Encoding::ASCII_8BIT
      end.should raise_error(Encoding::ConverterNotFoundError)

      str = "\x79".force_encoding Encoding::ASCII_8BIT
      str.send(@method, Encoding::Emacs_Mule).should == "y".force_encoding(Encoding::ASCII_8BIT)
    end

    it "raises an Encoding::ConverterNotFoundError when no conversion is possible" do
      str = "\x80".force_encoding Encoding::ASCII_8BIT
      lambda do
        str.send(@method, Encoding::Emacs_Mule)
      end.should raise_error(Encoding::ConverterNotFoundError)
    end

    it "raises an Encoding::ConverterNotFoundError for an invalid encoding" do
      lambda do
        "abc".send(@method, "xyz")
      end.should raise_error(Encoding::ConverterNotFoundError)
    end
  end

  describe "when passed options" do
    it "does not process transcoding options if not transcoding" do
      result = "あ\ufffdあ".send(@method, :undef => :replace)
      result.should == "あ\ufffdあ"
    end

    it "calls #to_hash to convert the object" do
      options = mock("string encode options")
      options.should_receive(:to_hash).and_return({ :undef => :replace })

      result = "あ\ufffdあ".send(@method, options)
      result.should == "あ\ufffdあ"
    end

    it "transcodes to Encoding.default_internal when set" do
      Encoding.default_internal = Encoding::UTF_8
      str = "\xA4\xA2".force_encoding Encoding::EUC_JP
      str.send(@method, :invalid => :replace).should == "あ"
    end

    it "raises an Encoding::ConverterNotFoundError when no conversion is possible despite ':invalid => :replace, :undef => :replace'" do
      Encoding.default_internal = Encoding::Emacs_Mule
      str = "\x80".force_encoding Encoding::ASCII_8BIT
      lambda do
        str.send(@method, :invalid => :replace, :undef => :replace)
      end.should raise_error(Encoding::ConverterNotFoundError)
    end
  end

  describe "when passed to, from" do
    it "transcodes between the encodings ignoring the String encoding" do
      str = "あ"
      result = "\xA6\xD0\x8F\xAB\xE4\x8F\xAB\xB1"
      result.force_encoding Encoding::EUC_JP
      str.send(@method, "euc-jp", "ibm437").should == result
    end

    it "calls #to_str to convert the from object to an Encoding" do
      enc = mock("string encode encoding")
      enc.should_receive(:to_str).and_return("ibm437")

      str = "あ"
      result = "\xA6\xD0\x8F\xAB\xE4\x8F\xAB\xB1"
      result.force_encoding Encoding::EUC_JP

      str.send(@method, "euc-jp", enc).should == result
    end
  end

  describe "when passed to, options" do
    it "replaces undefined characters in the destination encoding" do
      result = "あ?あ".send(@method, Encoding::EUC_JP, :undef => :replace)
      result.should == "\xA4\xA2?\xA4\xA2".force_encoding("euc-jp")
    end

    it "replaces invalid characters in the destination encoding" do
      "ab\xffc".send(@method, Encoding::ISO_8859_1, :invalid => :replace).should == "ab?c"
    end

    it "calls #to_hash to convert the options object" do
      options = mock("string encode options")
      options.should_receive(:to_hash).and_return({ :undef => :replace })

      result = "あ?あ".send(@method, Encoding::EUC_JP, options)
      result.should == "\xA4\xA2?\xA4\xA2".force_encoding("euc-jp")
    end
  end

  describe "when passed to, from, options" do
    it "replaces undefined characters in the destination encoding" do
      str = "あ?あ".force_encoding Encoding::ASCII_8BIT
      result = str.send(@method, "euc-jp", "utf-8", :undef => :replace)
      result.should == "\xA4\xA2?\xA4\xA2".force_encoding("euc-jp")
    end

    it "replaces invalid characters in the destination encoding" do
      str = "ab\xffc".force_encoding Encoding::ASCII_8BIT
      str.send(@method, "iso-8859-1", "utf-8", :invalid => :replace).should == "ab?c"
    end

    it "calls #to_str to convert the to object to an encoding" do
      to = mock("string encode to encoding")
      to.should_receive(:to_str).and_return("iso-8859-1")

      str = "ab\xffc".force_encoding Encoding::ASCII_8BIT
      str.send(@method, to, "utf-8", :invalid => :replace).should == "ab?c"
    end

    it "calls #to_str to convert the from object to an encoding" do
      from = mock("string encode to encoding")
      from.should_receive(:to_str).and_return("utf-8")

      str = "ab\xffc".force_encoding Encoding::ASCII_8BIT
      str.send(@method, "iso-8859-1", from, :invalid => :replace).should == "ab?c"
    end

    it "calls #to_hash to convert the options object" do
      options = mock("string encode options")
      options.should_receive(:to_hash).and_return({ :invalid => :replace })

      str = "ab\xffc".force_encoding Encoding::ASCII_8BIT
      str.send(@method, "iso-8859-1", "utf-8", options).should == "ab?c"
    end
  end

  describe "given the :xml => :text option" do
    it "replaces all instances of '&' with '&amp;'" do
      '& and &'.send(@method, "UTF-8", :xml => :text).should == '&amp; and &amp;'
    end

    it "replaces all instances of '<' with '&lt;'" do
      '< and <'.send(@method, "UTF-8", :xml => :text).should == '&lt; and &lt;'
    end

    it "replaces all instances of '>' with '&gt;'" do
      '> and >'.send(@method, "UTF-8", :xml => :text).should == '&gt; and &gt;'
    end

    it "does not replace '\"'" do
      '" and "'.send(@method, "UTF-8", :xml => :text).should == '" and "'
    end

    it "replaces undefined characters with their upper-case hexadecimal numeric character references" do
      'ürst'.send(@method, Encoding::US_ASCII, :xml => :text).should == '&#xFC;rst'
    end
  end

  describe "given the :xml => :attr option" do
    it "surrounds the encoded text with double-quotes" do
      'abc'.send(@method, "UTF-8", :xml => :attr).should == '"abc"'
    end

    it "replaces all instances of '&' with '&amp;'" do
      '& and &'.send(@method, "UTF-8", :xml => :attr).should == '"&amp; and &amp;"'
    end

    it "replaces all instances of '<' with '&lt;'" do
      '< and <'.send(@method, "UTF-8", :xml => :attr).should == '"&lt; and &lt;"'
    end

    it "replaces all instances of '>' with '&gt;'" do
      '> and >'.send(@method, "UTF-8", :xml => :attr).should == '"&gt; and &gt;"'
    end

    it "replaces all instances of '\"' with '&quot;'" do
      '" and "'.send(@method, "UTF-8", :xml => :attr).should == '"&quot; and &quot;"'
    end

    it "replaces undefined characters with their upper-case hexadecimal numeric character references" do
      'ürst'.send(@method, Encoding::US_ASCII, :xml => :attr).should == '"&#xFC;rst"'
    end
  end

  it "raises ArgumentError if the value of the :xml option is not :text or :attr" do
    lambda { ''.send(@method, "UTF-8", :xml => :other) }.should raise_error(ArgumentError)
  end
end
