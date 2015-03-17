describe :getoptlong_each, :shared => true do
  before(:each) do
    @opts = GetoptLong.new(
      [ '--size', '-s',             GetoptLong::REQUIRED_ARGUMENT ],
      [ '--verbose', '-v',          GetoptLong::NO_ARGUMENT ],
      [ '--query', '-q',            GetoptLong::NO_ARGUMENT ],
      [ '--check', '--valid', '-c', GetoptLong::NO_ARGUMENT ]
    )
  end

  it "passes each argument/value pair to the block" do
    begin
      old_argv = ARGV
      ARGV = [ "--size", "10k", "-v", "-q", "a.txt", "b.txt" ]

      pairs = []
      @opts.send(@method) { |arg, val| pairs << [ arg, val ] }
      pairs.should == [ [ "--size", "10k" ], [ "--verbose", "" ], [ "--query", ""] ]
    ensure
      ARGV = old_argv
    end
  end
end
