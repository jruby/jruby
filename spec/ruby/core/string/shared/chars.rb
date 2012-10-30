# -*- encoding: utf-8 -*-
require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe :string_chars, :shared => true do
  it "passes each char in self to the given block" do
    a = []
    "hello".send(@method) { |c| a << c }
    a.should == ['h', 'e', 'l', 'l', 'o']
  end

  ruby_bug 'redmine #1487', '1.9.1' do
    it "returns self" do
      s = StringSpecs::MyString.new "hello"
      s.send(@method){}.should equal(s)
    end
  end

  it "returns an enumerator when no block given" do
    enum = "hello".send(@method)
    enum.should be_an_instance_of(enumerator_class)
    enum.to_a.should == ['h', 'e', 'l', 'l', 'o']
  end


  it "is unicode aware" do
    before = $KCODE
    $KCODE = "UTF-8"
    "\303\207\342\210\202\303\251\306\222g".send(@method).to_a.should == ["\303\207", "\342\210\202", "\303\251", "\306\222", "g"]
    $KCODE = before
  end

  with_feature :encoding do
    it "returns characters in the same encoding as self" do
      "&%".force_encoding('Shift_JIS').chars.to_a.all? {|c| c.encoding.name.should == 'Shift_JIS'}
      "&%".encode('ASCII-8BIT').chars.to_a.all? {|c| c.encoding.name.should == 'ASCII-8BIT'}
    end

    it "works with multibyte characters" do
      s = "\u{8987}".force_encoding("UTF-8")
      s.bytesize.should == 3
      s.send(@method).to_a.should == [s]
    end

    it "works if the String's contents is invalid for its encoding" do
      s = "\xA4"
      s.force_encoding('UTF-8')
      s.valid_encoding?.should be_false
      s.send(@method).to_a.should == ["\xA4".force_encoding("UTF-8")]
    end

    it "returns a different character if the String is transcoded" do
      s = "\u{20AC}".force_encoding('UTF-8')
      s.encode('UTF-8').send(@method).to_a.should == ["\u{20AC}".force_encoding('UTF-8')]
      s.encode('iso-8859-15').send(@method).to_a.should == [
        "\xA4".force_encoding('iso-8859-15')]
      s.encode('iso-8859-15').encode('UTF-8').send(@method).to_a.should == [
        "\u{20AC}".force_encoding('UTF-8')]
    end

    it "uses the String's encoding to determine what characters it contains" do
      s = "\u{287}"
      s.force_encoding('UTF-8').send(@method).to_a.should == [s.force_encoding('UTF-8')]
      s.force_encoding('BINARY').send(@method).to_a.should == [
        "\xCA".force_encoding('BINARY'), "\x87".force_encoding('BINARY')]
      s.force_encoding('SJIS').send(@method).to_a.should == [
        "\xCA".force_encoding('SJIS'), "\x87".force_encoding('SJIS')]
    end
  end
end
