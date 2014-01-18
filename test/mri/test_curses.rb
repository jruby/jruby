require 'test/unit'
require_relative 'ruby/envutil'

begin
  require 'curses'
  require 'pty'
rescue LoadError
end

class TestCurses < Test::Unit::TestCase
  def test_version
    assert_instance_of(String, Curses::VERSION)
  end
end if defined? Curses

class TestCurses
  def run_curses(src, input = nil, timeout: 1)
    r, w, pid = PTY.spawn({"TERM"=>ENV["TERM"]||"dumb"}, EnvUtil.rubybin, "-e", <<-"src")
require 'timeout'
require 'curses'
include Curses
init_screen
begin
  result = Timeout.timeout(#{timeout}) do
    print "!"
    #{src}
  end
rescue Exception => e
ensure
  close_screen
  puts "", [Marshal.dump([result, e])].pack('m').delete("\n")
  print "\\0"
  $stdio.flush
end
src
    begin
      wait = r.readpartial(1)
      if wait != "!"
        wait << r.readpartial(1000)
        raise "TERM: '#{ENV["TERM"]}'; #{wait}"
      end
      if input
        w.print(input)
        w.flush
      end
      res = r.gets("\0")
      return unless res
      res.chomp!("\0")
      res, error = Marshal.load(res[/(.*)\Z/, 1].unpack('m')[0])
      raise error if error
      return res
    ensure
      r.close unless r.closed?
      w.close unless w.closed?
      Process.wait(pid)
    end
  end

  def test_getch
    assert_equal("a", run_curses("getch", "a"))
  end
  def test_getch_cbreak
    assert_equal("a", run_curses("cbreak; getch", "a"))
  end
  def test_getch_nocbreak
    assert_raise(Timeout::Error) {run_curses("nocbreak; getch", "a")}
  end
  def test_getch_crmode
    assert_equal("a", run_curses("crmode; getch", "a"))
  end
  def test_getch_nocrmode
    assert_raise(Timeout::Error) {run_curses("nocrmode; getch", "a")}
  end
end if defined? TestCurses and defined? PTY
