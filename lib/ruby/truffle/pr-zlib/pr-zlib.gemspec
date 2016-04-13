require 'rubygems'

Gem::Specification.new do |spec|
  spec.name             = 'pr-zlib'
  spec.version          = '1.0.3'
  spec.authors          = ['Park Heesob', 'Daniel Berger']
  spec.email            = 'phasis@gmail.com'
  spec.homepage         = 'https://github.com/djberg96/pr-zlib'
  spec.license          = 'zlib'
  spec.summary          = 'Pure Ruby version of the zlib library'
  spec.test_files       = Dir['test/*.rb']
  spec.files            = Dir["**/*"].reject{ |f| f.include?('git') }
  spec.extra_rdoc_files = ['README', 'CHANGES', 'MANIFEST']
  spec.cert_chain       = Dir['certs/*']

  spec.add_development_dependency('test-unit', '>= 2.4.0')
  spec.add_development_dependency('ruby-prof')
   
  spec.description = <<-EOF
    The pr-zlib library is a pure Ruby implementation of both the zlib C
    library, and the Ruby zlib interface that ships as part of the standard
    library.
  EOF
end
