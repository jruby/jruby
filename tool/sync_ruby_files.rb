STDLIB_FILES = %w[
  English.rb
  abbrev.rb
  base64.rb
  benchmark.rb
  cgi
  cgi.rb
  date
  date.rb
  debug.rb
  delegate.rb
  drb
  drb.rb
  erb.rb
  find.rb
  getoptlong.rb
  monitor.rb
  net
  observer.rb
  open-uri.rb
  open3.rb
  optionparser.rb
  optparse
  optparse.rb
  ostruct.rb
  pp.rb
  prettyprint.rb
  profile.rb
  profiler.rb
  pstore.rb
  resolv-replace.rb
  resolv.rb
  rinda
  set.rb
  shellwords.rb
  singleton.rb
  time.rb
  tmpdir.rb
  tsort.rb
  un.rb
  unicode_normalize
  uri
  uri.rb
  weakref.rb
  yaml
  yaml.rb
]

EXT_FILES = {
  'ext/bigdecimal/lib/bigdecimal' => 'bigdecimal',
  'ext/digest/lib/digest.rb' => 'digest.rb',
  'ext/digest/sha2/lib/sha2.rb' => 'digest/sha2.rb',
  'ext/nkf/lib/kconv.rb' => 'kconv.rb',
  'ext/pathname/lib/pathname.rb' => 'pathname.rb',
  'ext/pty/lib/expect.rb' => 'expect.rb',
  'ext/socket/lib/socket.rb' => 'socket.rb',
  'ext/win32/lib/win32' => 'win32',
  'ext/fiddle/lib/fiddle.rb' => 'fiddle.rb',
  'ext/fiddle/lib/fiddle' => 'fiddle',
  'ext/ripper/lib/ripper' => 'ripper',
  'ext/ripper/lib/ripper.rb' => 'ripper.rb',
}
