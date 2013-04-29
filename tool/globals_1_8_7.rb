STDLIB_FILES = %w[
  English.rb
  Env.rb
  README
  abbrev.rb
  base64.rb
  benchmark.rb
  cgi
  cgi-lib.rb
  cgi.rb
  complex.rb
  csv.rb
  date
  date.rb
  date2.rb
  debug.rb
  delegate.rb
  dl.rb
  drb
  drb.rb
  e2mmap.rb
  erb.rb
  eregex.rb
  fileutils.rb
  finalize.rb
  find.rb
  forwardable.rb
  ftools.rb
  generator.rb
  getoptlong.rb
  getopts.rb
  gserver.rb
  importenv.rb
  ipaddr.rb
  irb
  irb.rb
  jcode.rb
  logger.rb
  mailread.rb
  mathn.rb
  matrix.rb
  monitor.rb
  mutex_m.rb
  net
  observer.rb
  open-uri.rb
  open3.rb
  optparse
  optparse.rb
  ostruct.rb
  parsearg.rb
  parsedate.rb
  pathname.rb
  ping.rb
  pp.rb
  prettyprint.rb
  profile.rb
  profiler.rb
  pstore.rb
  racc
  rational.rb
  rdoc
  readbytes.rb
  resolv-replace.rb
  resolv.rb
  rexml
  rinda
  rss
  rss.rb
  rubyunit.rb
  runit
  scanf.rb
  set.rb
  shell
  shell.rb
  shellwords.rb
  singleton.rb
  soap
  sync.rb
  test
  thwait.rb
  time.rb
  tracer.rb
  tsort.rb
  un.rb
  uri
  uri.rb
  webrick
  webrick.rb
  wsdl
  xmlrpc
  xsd
  yaml.rb
  yaml
]

EXT_FILES = {
  'ext/bigdecimal/lib/bigdecimal' => 'bigdecimal',
  'ext/dl/lib/dl' => 'dl',
  'ext/pty/lib/expect.rb' => 'expect.rb',
  'ext/io/wait/lib/nonblock.rb' => 'io/nonblock.rb',
  'ext/nkf/lib/kconv.rb' => 'kconv.rb',
  'ext/digest/lib/digest.rb' => 'digest.rb',
  'ext/digest/lib/md5.rb' => 'md5.rb',
  'ext/digest/lib/sha1.rb' => 'sha1.rb',
  'ext/digest/sha2/lib/sha2.rb' => 'digest/sha2.rb',
  'ext/Win32API/lib/win32' => 'win32',
  'ext/openssl/lib/openssl' => 'openssl'
}
