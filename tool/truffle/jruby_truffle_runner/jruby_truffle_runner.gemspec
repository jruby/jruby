Gem::Specification.new do |s|
  s.name        = 'jruby_truffle_runner'
  s.version     = '0.0.1'
  s.platform    = Gem::Platform::RUBY
  s.authors     = ['Petr Chalupa']
  s.email       = ['git@pitr.ch']
  s.homepage    = 'https://github.com/jruby/jruby'
  s.summary     = 'Temporary JRuby+Truffle runner'
  s.description = 'Until JRuby+Truffle is more complete, allows to run apps/gems simply on JRuby+Truffle.'

  s.required_rubygems_version = '>= 1.3.6'

  s.files        = Dir['{lib}/**/*.rb', 'bin/*', 'gem_configurations/*.yaml', 'LICENSE', '*.md']
  s.require_path = 'lib'

  s.executables = ['truffle']
end
