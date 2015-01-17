#-*- mode: ruby -*-

gemspec :include_jars => true, :jar => 'readline.jar'

version = File.read( File.join( basedir, '..', '..', 'VERSION' ) ).strip
parent 'org.jruby:jruby-ext', version

jruby_plugin! :gem do
  execute_goals :id => 'default-push', :skip => true
end

# we need the jruby API here, the version should be less important here
jar 'org.jruby:jruby:1.7.11', :scope => :provided

properties( 'gem.home' => '${basedir}/../target/rubygems',
            'gem.path' => '${gem.home}',
            'jruby.plugins.version' => '1.0.7',
            'tesla.dump.pom' => 'pom.xml',
            'tesla.dump.readonly' => true )

# vim: syntax=Ruby
