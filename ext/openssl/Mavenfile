#-*- mode: ruby -*-

gemspec :jar => 'jopenssl', :include_jars => true

version = File.read( File.join( basedir, '..', '..', 'VERSION' ) ).strip
version.gsub!( /-SNAPSHOT$/, '' )

if model.version.to_s.match /[a-zA-Z]/
  model.group_id = 'org.jruby.gems'

  plugin :deploy do
    execute_goals( :deploy, 
                  :skip => false,
                  :altDeploymentRepository => 'sonatype-nexus-snapshots::default::https://oss.sonatype.org/content/repositories/snapshots/' )
  end
else
  parent 'org.jruby:jruby-ext', version
end

plugin( :compiler, :target => '1.6', :source => '1.6', :debug => true, :verbose => false, :showWarnings => true, :showDeprecation => true )

jruby_plugin! :gem do
  # avoid adding this not yet built openssl to the load_path
  # when installing dependent gems
  execute_goal :initialize, :lib => 'non-existing'
  execute_goals :id => 'default-push', :skip => true
end

# we need the jruby API here, the version should be less important here
jar 'org.jruby:jruby-core:1.6.8', :scope => :provided

properties( 'gem.home' => '../target/rubygems',
            'gem.path' => '${gem.home}',
            'tesla.dump.pom' => 'pom.xml',
            'tesla.dump.readonly' => true )

# vim: syntax=Ruby
