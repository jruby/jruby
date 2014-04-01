#-*- mode: ruby -*-

gemspec :jar => 'jopenssl', :include_jars => true

version = File.read( File.join( basedir, '..', '..', 'VERSION' ) ).strip
version.gsub!( /-SNAPSHOT$/, '' )

parent 'org.jruby:jruby-ext', version

if model.version.to_s.match /[a-zA-Z]/
  model.group_id = 'org.jruby.gems'
end

jruby_plugin! :gem do
  # avoid adding this not yet built openssl to the load_path
  # when installing dependent gems
  execute_goal :initialize, :lib => 'non-existing'
end

# we need the jruby API here, the version should be less important here
jar 'org.jruby:jruby:1.7.11', :scope => :provided

properties( 'gem.home' => '../target/rubygems',
            'gem.path' => '${gem.home}',
            'tesla.dump.pom' => 'pom.xml',
            'tesla.dump.readonly' => true )

# vim: syntax=Ruby
