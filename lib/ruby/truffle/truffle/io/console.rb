# Copyright (c) 2007-2016 The JRuby project. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# This is a shim based on JRuby's implementation with stty

# This implementation of io/console is a little hacky. It shells out to `stty`
# for most operations, which does not work on Windows, in secured environments,
# and so on.
#
# Finally, since we're using stty to shell out, we can only manipulate stdin/
# stdout tty rather than manipulating whatever terminal is actually associated
# with the IO we're calling against. This will produce surprising results if
# anyone is actually using io/console against non-stdio ttys...but that case
# seems like it would be pretty rare.

require 'rbconfig'

# Methods common to all backend impls
class IO
  def getch(*)
    raw do
      getc
    end
  end

  def getpass(prompt = nil)
    wio = self == $stdin ? $stderr : self
    wio.write(prompt) if prompt
    begin
      str = nil
      noecho do
        str = gets
      end
    ensure
      puts($/)
    end
    str.chomp
  end
end


class IO
  if RbConfig::CONFIG['host_os'].downcase =~ /linux/ && File.exists?("/proc/#{Process.pid}/fd")
    def stty(*args)
      `stty #{args.join(' ')} < /proc/#{Process.pid}/fd/#{fileno}`
    end
  else
    def stty(*args)
      `stty #{args.join(' ')}`
    end
  end

  def raw(*)
    saved = stty('-g')
    stty('raw -echo')
    yield self
  ensure
    stty(saved)
  end

  def raw!(*)
    stty('raw -echo')
  end

  def cooked(*)
    saved = stty('-g')
    stty('-raw')
    yield self
  ensure
    stty(saved)
  end

  def cooked!(*)
    stty('-raw')
  end

  def echo=(echo)
    stty(echo ? 'echo' : '-echo')
  end

  def echo?
    (stty('-a') =~ / -echo /) ? false : true
  end

  def noecho
    saved = stty('-g')
    stty('-echo')
    yield self
  ensure
    stty(saved)
  end

  # Not all systems return same format of stty -a output
  IEEE_STD_1003_2 = '(?<rows>\d+) rows; (?<columns>\d+) columns'
  UBUNTU = 'rows (?<rows>\d+); columns (?<columns>\d+)'

  def winsize
    match = stty('-a').match(/#{IEEE_STD_1003_2}|#{UBUNTU}/)
    [match[:rows].to_i, match[:columns].to_i]
  end

  def winsize=(size)
    stty("rows #{size[0]} cols #{size[1]}")
  end

  def iflush
  end

  def oflush
  end

  def ioflush
  end
end
