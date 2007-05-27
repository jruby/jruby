require File.dirname(__FILE__) + '/../spec_helper'

describe 'Creating the option parser' do
  it 'provides a .new to generate a blank set of options' do
    Options.new
  end
end

describe 'Configuring the option parser' do
  before { @o = Options.new }

  it 'will take individual options with #option, must have short, long and a description' do
    @o.option 'h', 'help', 'Displays a help message'  
    should_raise(ArgumentError) { @o.option '-f', '--foo' }
  end

  it "also accepts short and long with the leading -'s which are stripped" do
    @o.option '-h', '--help', 'Displays a help message'  
    @o.instance_variable_get(:@allowed)['h'].nil?.should == false
    @o.instance_variable_get(:@allowed)['help'].nil?.should == false
  end
end

describe 'Getting help from the option parser' do
  it 'provides a #usage message constructed from the given options' do
    o = Options.new
    o.option '-h', '--help', 'Displays a help message'
    o.option '-f', '--foo', 'Does nothing useful', :one

    o.usage.gsub(/\s+/, '').should == "-h--helpDisplaysahelpmessage-f--fooARGDoesnothinguseful"
  end  
end

  # Parse arguments according to previously a given #configure.
  # The parser only accepts an Array, Strings have to be parsed
  # beforehand. The parser matches the given options to the ones
  # defined in #configure, leaves non-option arguments (such as
  # the path in 'ls -la /etc') alone. Any unknown arguments raise
  # an ArgumentError. The results of the parse are returned as a
  # Hash, see below. The accepted forms for options are:
  #
  # Short opts: -h, -ht, -h ARG, -h=ARG, -ht ARG (same as -h -t ARG).
  # Long opts:  --help, --help ARG, --help=ARG. (No joining.)
  #
  # Quoted strings are valid arguments but for Strings only.
  # Arrays should have parsed those at an earlier point.
  #
  # The returned Hash is indexed by the names of the found
  # options. These indices point to an Array of arguments
  # to that option if any, or just true if not. The Hash
  # also contains a special index, :args, containing all
  # of the non-option arguments.
describe 'Parsing options using the configured parser' do
  before do 
    @o = Options.new 

    @o.option '-a', '--aa', 'Aa'  
    @o.option '-b', '--bb', 'Bb'  
    @o.option '-c', '--cc', 'Cc'  
  end

  it 'returns a Hash with given options as defined keys' do
    h = @o.parse '--aa'

    h.key?('aa').nil?.should == false
  end

  it 'makes given option available both as long and short version' do
    h = @o.parse '--aa'

    h.key?('a').nil?.should == false
    h.key?('aa').nil?.should == false
  end

  it 'sets the value of any given option without a parameter to true' do
    h = @o.parse '--aa'

    h.key?('a').should == true
    h.key?('aa').should == true
  end

  it 'places any given arguments in :args if they do not belong to options' do
    h = @o.parse '-a ARG'

    h[:args].should == ['ARG']
  end
end

describe 'Parsing short options without arguments' do
  before do 
    @o = Options.new 

    @o.option '-a', '--aa', 'Aa'  
    @o.option '-b', '--bb', 'Bb'  
    @o.option '-c', '--cc', 'Cc'  
  end

  it 'takes short options separately' do
    h = @o.parse '-a -b -c'
    
    h.key?('a').should == true
    h.key?('aa').should == true
    h.key?('b').should == true
    h.key?('bb').should == true
    h.key?('c').should == true
    h.key?('cc').should == true
  end

  it 'takes short options combined' do
    h = @o.parse '-abc'
    
    h.key?('a').should == true
    h.key?('aa').should == true
    h.key?('b').should == true
    h.key?('bb').should == true
    h.key?('c').should == true
    h.key?('cc').should == true
  end

  it 'takes short options interspersed with nonoption-arguments' do
    h = @o.parse '-ab ARG -c'
    
    h.key?('a').should == true
    h.key?('aa').should == true
    h.key?('b').should == true
    h.key?('bb').should == true
    h.key?('c').should == true
    h.key?('cc').should == true
    h[:args].should == ['ARG']
  end
end

describe 'Parsing short options with arguments' do
  before do 
    @o = Options.new 

    @o.option '-a', '--aa', 'Aa'  
    @o.option '-b', '--bb', 'Bb'  
    @o.option '-c', '--cc', 'Cc'  
    @o.option '-d', '--dd', 'Dd', :one 
  end

  it 'defaults to :none specified arguments which means no following argument is captured' do 
    @o.parse('-a ARG')['a'].should == true
    @o.parse('-a ARG')['aa'].should == true
  end

  it 'stores the argument(s) in an Array stored as the value of the option name' do
    @o.parse('-d ARG')['d'].should == ['ARG']
    @o.parse('-d ARG')['dd'].should == ['ARG']
  end

  it 'accepts :one to denote a single argument' do
    @o.option '-e', '--ee', 'Ee', :one

    @o.parse('-e ARG')['e'].should == ['ARG']
    @o.parse('-e ARG')['ee'].should == ['ARG']
  end

  it 'ignores more than one argument when :one defined' do
    @o.option '-e', '--ee', 'Ee', :one
    
    h = @o.parse '-e ARG1 ARG2'

    h['e'].should == ['ARG1']
    h['ee'].should == ['ARG1']
    h[:args].should == ['ARG2']
  end

  it 'accepts :many to indicate as many nonoption args as follow before the following option' do
    @o.option '-f', '--ff', 'Ff', :many

    h = @o.parse '-f ARG1 ARG2 ARG3 -a ARG4'

    h['f'].should == %w|ARG1 ARG2 ARG3| 
    h['ff'].should == %w|ARG1 ARG2 ARG3| 
    h['a'].should == true
    h['aa'].should == true
    h[:args].should == %w|ARG4| 
  end

  it 'accepts :maybe to indicate zero or as many as possible arguments' do
    @o.option '-g', '--gg', 'Gg', :maybe

    @o.parse('-g -a')['g'].should == true
    @o.parse('-g -a')['gg'].should == true
    @o.parse('-g ARG -a')['g'].should == ['ARG']
    @o.parse('-g ARG -a')['gg'].should == ['ARG']
    @o.parse('-g ARG1 ARG2 ARG3 -a')['g'].should == %w|ARG1 ARG2 ARG3|
    @o.parse('-g ARG1 ARG2 ARG3 -a')['gg'].should == %w|ARG1 ARG2 ARG3|
  end

  it 'fails if :one and no arguments given' do
    @o.option '-h', '--hh', 'Hh', :one

    should_raise(ArgumentError) { @o.parse '-h' }
    should_raise(ArgumentError) { @o.parse '-h -a ARG' }
  end

  it 'fails if :many and one or no arguments given' do
    @o.option '-i', '--ii', 'Ii', :many

    should_raise(ArgumentError) { @o.parse '-i ARG' }
    should_raise(ArgumentError) { @o.parse '-i ARG -a ARG2' }
  end

  it 'assigns arguments only to the last in a set of combined short options ' do
    @o.option '-j', '--jj', 'Jj', :one

    @o.parse('-abj ARG')['a'].should == true
    @o.parse('-abj ARG')['aa'].should == true
    @o.parse('-abj ARG')['b'].should == true
    @o.parse('-abj ARG')['bb'].should == true
    @o.parse('-abj ARG')['j'].should == ['ARG']
    @o.parse('-abj ARG')['jj'].should == ['ARG']
  end
end

describe 'Parsing long options without arguments' do
  before do 
    @o = Options.new 

    @o.option '-a', '--aa', 'Aa'  
    @o.option '-b', '--bb', 'Bb'  
    @o.option '-c', '--cc', 'Cc'  
  end

  it 'takes long options separately' do
    h = @o.parse '--aa --bb --cc'
    
    h.key?('a').should == true
    h.key?('aa').should == true
    h.key?('b').should == true
    h.key?('bb').should == true
    h.key?('c').should == true
    h.key?('cc').should == true
  end

  it 'takes long options interspersed with nonoption-arguments' do
    h = @o.parse '--aa ARG --cc'
    
    p h
    h.key?('a').should == true
    h.key?('aa').should == true
    h.key?('c').should == true
    h.key?('cc').should == true
    h[:args].should == ['ARG']
  end
end

describe 'Parsing long options with arguments' do
  before do 
    @o = Options.new 

    @o.option '-a', '--aa', 'Aa'  
    @o.option '-b', '--bb', 'Bb'  
    @o.option '-c', '--cc', 'Cc'  
    @o.option '-d', '--dd', 'Dd', :one 
  end

  it 'defaults to :none specified arguments which means no following argument is captured' do 
    @o.parse('--aa ARG')['a'].should == true
    @o.parse('--aa ARG')['aa'].should == true
  end

  it 'stores the argument(s) in an Array stored as the value of the option name' do
    @o.parse('--dd ARG')['d'].should == ['ARG']
    @o.parse('--dd ARG')['dd'].should == ['ARG']
  end

  it 'accepts :one to denote a single argument' do
    @o.option '-e', '--ee', 'Ee', :one

    @o.parse('--ee ARG')['e'].should == ['ARG']
    @o.parse('--ee ARG')['ee'].should == ['ARG']
  end

  it 'ignores more than one argument when :one defined' do
    @o.option '-e', '--ee', 'Ee', :one
    
    h = @o.parse '--ee ARG1 ARG2'

    h['e'].should == ['ARG1']
    h['ee'].should == ['ARG1']
    h[:args].should == ['ARG2']
  end

  it 'accepts :many to indicate as many nonoption args as follow before the following option' do
    @o.option '-f', '--ff', 'Ff', :many

    h = @o.parse '--ff ARG1 ARG2 ARG3 -a ARG4'

    h['f'].should == %w|ARG1 ARG2 ARG3| 
    h['ff'].should == %w|ARG1 ARG2 ARG3| 
    h['a'].should == true
    h['aa'].should == true
    h[:args].should == %w|ARG4| 
  end

  it 'accepts :maybe to indicate zero or as many as possible arguments' do
    @o.option '-g', '--gg', 'Gg', :maybe

    @o.parse('--gg -a')['g'].should == true
    @o.parse('--gg -a')['gg'].should == true
    @o.parse('--gg ARG -a')['g'].should == ['ARG']
    @o.parse('--gg ARG -a')['gg'].should == ['ARG']
    @o.parse('--gg ARG1 ARG2 ARG3 -a')['g'].should == %w|ARG1 ARG2 ARG3|
    @o.parse('--gg ARG1 ARG2 ARG3 -a')['gg'].should == %w|ARG1 ARG2 ARG3|
  end

  it 'fails if :one and no arguments given' do
    @o.option '-h', '--hh', 'Hh', :one

    should_raise(ArgumentError) { @o.parse '--hh' }
    should_raise(ArgumentError) { @o.parse '--hh -a ARG' }
  end

  it 'fails if :many and one or no arguments given' do
    @o.option '-i', '--ii', 'Ii', :many

    should_raise(ArgumentError) { @o.parse '--ii ARG' }
    should_raise(ArgumentError) { @o.parse '--ii ARG -a ARG2' }
  end
end
#
#describe 'Using alternative syntax for parsing options' do
#  it 'allows using = with short or long options as an argument separator' do
#    o = Options.new
#    o.option '-a', '--aa', 'Aa', :one
#    o.option '-b', '--bb', 'Bb', :one
#    o.option '-c', '--cc', 'Cc'
#    o.option '-d', '--dd', 'Dd'
#
#    o.parse('-a=ARG')['a'].should == 'ARG'
#    o.parse('--aa=ARG')['a'].should == 'ARG'
#    o.parse('-ab=ARG')['a'].should == true
#    o.parse('-ab=ARG')['b'].should == 'ARG'
#  end
#end
