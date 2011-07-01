# -*- encoding: utf-8 -*-
$:.push File.expand_path("../lib", __FILE__)
require "jruby-win32ole/version"

Gem::Specification.new do |s|
  s.name        = 'jruby-win32ole'
  s.version     = JRuby::WIN32OLE::VERSION
  s.platform    = Gem::Platform::RUBY
  s.authors     = 'Thomas E. Enebo'
  s.email       = 'tom.enebo@gmail.com'
  s.homepage    = 'http://github.com/enebo/jruby-win32ole'
  s.summary     = %q{A Gem for win32ole support on JRuby}
  s.description = %q{A Gem for win32ole support on JRuby}

  s.rubyforge_project = "jruby-win32ole"

  s.files         = `git ls-files`.split("\n")
  s.test_files    = `git ls-files -- {test,spec,features}/*`.split("\n")
  s.executables   = `git ls-files -- bin/*`.split("\n").map{ |f| File.basename(f) }
  s.require_paths = ["lib"]
end
