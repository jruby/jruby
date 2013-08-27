#! /usr/bin/env jruby
require 'rspec'

def capture_io
  require 'stringio'

  captured_stdout, captured_stderr = StringIO.new, StringIO.new
  orig_stdout, orig_stderr = $stdout, $stderr
  $stdout, $stderr         = captured_stdout, captured_stderr
  begin
    yield
  ensure
    $stdout = orig_stdout
    $stderr = orig_stderr
  end
  
  return captured_stdout.string, captured_stderr.string
end

def capture_subprocess_io
  require 'tempfile'

  captured_stdout, captured_stderr = Tempfile.new("out"), Tempfile.new("err")

    orig_stdout, orig_stderr = $stdout.dup, $stderr.dup
    $stdout.reopen captured_stdout
    $stderr.reopen captured_stderr

    begin
      yield

      $stdout.rewind
      $stderr.rewind

      [captured_stdout.read, captured_stderr.read]
    ensure
      captured_stdout.unlink
      captured_stderr.unlink
      $stdout.reopen orig_stdout
      $stderr.reopen orig_stderr
    end
end


describe 'Kernel#exec' do
  it 'inherits Dir.pwd inside Dir.chdir' do
    RUBY = File.join(*RbConfig::CONFIG.values_at("bindir", "ruby_install_name")) + RbConfig::CONFIG["EXEEXT"]

    this_dir = File.dirname(__FILE__).split('/').last

    Dir.chdir ".." do
      out, err = capture_subprocess_io { exec RUBY, "#{this_dir}/foo.rb" }
      out.should == 'foo'
    end
  end
end

