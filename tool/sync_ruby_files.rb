STDLIB_FILES = %w[
  resolv.rb
  tmpdir.rb
  unicode_normalize
  weakref.rb
]

EXT_FILES = {
  'ext/bigdecimal/lib/bigdecimal' => 'bigdecimal',
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
