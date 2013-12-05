describe :string_each_line, :shared => true do
  it "splits using default newline separator when none is specified" do
    a = []
    "one\ntwo\r\nthree".send(@method) { |s| a << s }
    a.should == ["one\n", "two\r\n", "three"]

    b = []
    "hello\n\n\nworld".send(@method) { |s| b << s }
    b.should == ["hello\n", "\n", "\n", "world"]

    c = []
    "\n\n\n\n\n".send(@method) {|s| c << s}
    c.should == ["\n", "\n", "\n", "\n", "\n"]
  end

  it "splits self using the supplied record separator and passes each substring to the block" do
    a = []
    "one\ntwo\r\nthree".send(@method, "\n") { |s| a << s }
    a.should == ["one\n", "two\r\n", "three"]

    b = []
    "hello\nworld".send(@method, 'l') { |s| b << s }
    b.should == [ "hel", "l", "o\nworl", "d" ]

    c = []
    "hello\n\n\nworld".send(@method, "\n") { |s| c << s }
    c.should == ["hello\n", "\n", "\n", "world"]
  end

  it "taints substrings that are passed to the block if self is tainted" do
    "one\ntwo\r\nthree".taint.send(@method) { |s| s.tainted?.should == true }

    "x.y.".send(@method, ".".taint) { |s| s.tainted?.should == false }
  end

  it "passes self as a whole to the block if the separator is nil" do
    a = []
    "one\ntwo\r\nthree".send(@method, nil) { |s| a << s }
    a.should == ["one\ntwo\r\nthree"]
  end

  it "yields paragraphs (broken by 2 or more successive newlines) when passed ''" do
    a = []
    "hello\nworld\n\n\nand\nuniverse\n\n\n\n\n".send(@method, '') { |s| a << s }
    a.should == ["hello\nworld\n\n\n", "and\nuniverse\n\n\n\n\n"]

    a = []
    "hello\nworld\n\n\nand\nuniverse\n\n\n\n\ndog".send(@method, '') { |s| a << s }
    a.should == ["hello\nworld\n\n\n", "and\nuniverse\n\n\n\n\n", "dog"]
  end

  it "uses $/ as the separator when none is given" do
    [
      "", "x", "x\ny", "x\ry", "x\r\ny", "x\n\r\r\ny",
      "hello hullo bello"
    ].each do |str|
      ["", "llo", "\n", "\r", nil].each do |sep|
        begin
          expected = []
          str.send(@method, sep) { |x| expected << x }

          old_rec_sep, $/ = $/, sep

          actual = []
          str.send(@method) { |x| actual << x }

          actual.should == expected
        ensure
          $/ = old_rec_sep
        end
      end
    end
  end

  it "yields subclass instances for subclasses" do
    a = []
    StringSpecs::MyString.new("hello\nworld").send(@method) { |s| a << s.class }
    a.should == [StringSpecs::MyString, StringSpecs::MyString]
  end

  it "returns self" do
    s = "hello\nworld"
    (s.send(@method) {}).should equal(s)
  end

  it "tries to convert the separator to a string using to_str" do
    separator = mock('l')
    separator.should_receive(:to_str).and_return("l")

    a = []
    "hello\nworld".send(@method, separator) { |s| a << s }
    a.should == [ "hel", "l", "o\nworl", "d" ]
  end

  ruby_version_is ''...'1.9' do
    it "raises a RuntimeError if the string is modified while substituting" do
      str = "hello\nworld"
      lambda { str.send(@method) { str[0] = 'x' } }.should raise_error(RuntimeError)
    end
  end

  ruby_version_is '1.9' do
    it "does not care if the string is modified while substituting" do
      str = "hello\nworld."
      out = []
      str.send(@method){|x| out << x; str[-1] = '!' }.should == "hello\nworld!"
      out.should == ["hello\n", "world."]
    end
  end

  it "raises a TypeError when the separator can't be converted to a string" do
    lambda { "hello world".send(@method, false) {}     }.should raise_error(TypeError)
    lambda { "hello world".send(@method, mock('x')) {} }.should raise_error(TypeError)
  end

  ruby_version_is ''...'1.9' do
    it "raises a TypeError when the separator is a character or a symbol" do
      lambda { "hello world".send(@method, ?o) {}        }.should raise_error(TypeError)
      lambda { "hello world".send(@method, :o) {}        }.should raise_error(TypeError)
    end
  end

  ruby_version_is '1.9' do
    it "accept string separator" do
      "hello world".send(@method, ?o).to_a.should == ["hello", " wo", "rld"]
    end

    it "raises a TypeError when the separator is a symbol" do
      lambda { "hello world".send(@method, :o).to_a }.should raise_error(TypeError)
    end
  end
end
