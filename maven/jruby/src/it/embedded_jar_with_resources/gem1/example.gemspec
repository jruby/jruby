#-*- mode: ruby -*-

Gem::Specification.new do |s|
  s.name = 'example'
  s.version = "3"
  s.author = 'example person'
  s.email = [ 'mail@example.com' ]
  s.summary = 'example'

  s.files << Dir[ '*file' ]
  s.files << Dir[ 'lib/*' ]
  s.files << 'example.gemspec'

end

# vim: syntax=Ruby
