# We no longer automatically sync these files since there are so few of them.
# Several, like socket.rb and pathname.rb, have different code for JRuby.
# This file remains as a list of the files we do not yet source from gems.

STDLIB_FILES = %w[
]

EXT_FILES = {
  'ext/nkf/lib/kconv.rb' => 'kconv.rb',
  'ext/pathname/lib/pathname.rb' => 'pathname.rb',
  'ext/pty/lib/expect.rb' => 'expect.rb',
  'ext/socket/lib/socket.rb' => 'socket.rb',
  'ext/win32/lib/win32' => 'win32',
  'ext/fiddle/lib/fiddle.rb' => 'fiddle.rb',
  'ext/fiddle/lib/fiddle' => 'fiddle',
  'ext/ripper/lib/ripper' => 'ripper',
  'ext/ripper/lib/ripper.rb' => 'ripper.rb',
  'ext/syslog/lib/syslog/logger.rb' => 'syslog/logger.rb',
}
