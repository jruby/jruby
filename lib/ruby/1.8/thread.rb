#
#		thread.rb - thread support classes
#			$Date$
#			by Yukihiro Matsumoto <matz@netlab.co.jp>
#
# Copyright (C) 2001  Yukihiro Matsumoto
# Copyright (C) 2000  Network Applied Communication Laboratory, Inc.
# Copyright (C) 2000  Information-technology Promotion Agency, Japan
#

unless defined? Thread
  fail "Thread not available for this ruby interpreter"
end

unless defined? ThreadError
  class ThreadError<StandardError
  end
end

if $DEBUG
  Thread.abort_on_exception = true
end

class Thread
  #
  # Wraps a block in Thread.critical, restoring the original value upon exit
  # from the critical section.
  #
  def Thread.exclusive
    _old = Thread.critical
    begin
      Thread.critical = true
      return yield
    ensure
      Thread.critical = _old
    end
  end
end

require 'thread.so'

