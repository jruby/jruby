# -*- encoding: us-ascii -*-

require File.expand_path('../../spec_helper', __FILE__)

describe "Heredoc string" do

  before :each do
    @ip = 'xxx' # used for interpolation
  end

  it "allows HEREDOC with <<identifier, interpolated" do
    s = <<HERE
foo bar#{@ip}
HERE
    s.should == "foo barxxx\n"
  end

  it 'allow HEREDOC with <<"identifier", interpolated' do
    s = <<"HERE"
foo bar#{@ip}
HERE
    s.should == "foo barxxx\n"
  end

  it "allows HEREDOC with <<'identifier', no interpolation" do
    s = <<'HERE'
foo bar#{@ip}
HERE
    s.should == 'foo bar#{@ip}' + "\n"
  end

  it "allows HEREDOC with <<-identifier, allowing to indent identifier, interpolated" do
    s = <<-HERE
    foo bar#{@ip}
    HERE

    s.should == "    foo barxxx\n"
  end

  it 'allows HEREDOC with <<-"identifier", allowing to indent identifier, interpolated' do
    s = <<-"HERE"
    foo bar#{@ip}
    HERE

    s.should == "    foo barxxx\n"
  end

  it "allows HEREDOC with <<-'identifier', allowing to indent identifier, no interpolation" do
    s = <<-'HERE'
    foo bar#{@ip}
    HERE

    s.should == '    foo bar#{@ip}' + "\n"
  end

  ruby_version_is "2.3" do
    require File.expand_path('../fixtures/squiggly_heredoc', __FILE__)

    it "allows HEREDOC with <<~'identifier', allowing to indent identifier and content" do
      SquigglyHeredocSpecs.message.should == "character density, n.:\n  The number of very weird people in the office.\n"
    end

    it "trims trailing newline character for blank HEREDOC with <<~'identifier'" do
      SquigglyHeredocSpecs.blank.should == ""
    end

    it 'allows HEREDOC with <<~identifier, interpolated' do
      SquigglyHeredocSpecs.unquoted.should == "unquoted interpolated\n"
    end

    it 'allows HEREDOC with <<"identifier", interpolated' do
      SquigglyHeredocSpecs.doublequoted.should == "doublequoted interpolated\n"
    end

    it "allows HEREDOC with <<'identifier', no interpolation" do
      SquigglyHeredocSpecs.singlequoted.should == "singlequoted \#{\"interpolated\"}\n"
    end

    it "selects the least-indented line and removes its indentation from all the lines" do
      SquigglyHeredocSpecs.least_indented_on_the_last_line.should == "    a\n  b\nc\n"
    end
  end
end
