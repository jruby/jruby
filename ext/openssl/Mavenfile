#-*- mode: ruby -*-

gemspec :jar => 'jopenssl', :include_jars => true

# that all should be part of ruby-maven to pick the right extension
properties 'jruby.plugins.version' => '1.0.2'
build.extensions.clear
extension 'de.saumya.mojo:gem-with-jar-extension', '${jruby.plugins.version}'

if model.version.to_s.match /[a-zA-Z]/
  model.group_id = 'org.jruby.gems'

  model.version = model.version + '-SNAPSHOT'
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

# you can use -Djruby.version=1.6.8 to pick a jruby version
# TODO use 1.6.8 and let the gem-maven-plugin pick the right version
properties 'jruby.version' => '1.7.12'
# we need the jruby API here, the version should be less important here
jar 'org.jruby:jruby-core', '${jruby.version}', :scope => :provided
# this artifact is needed to run the packaging at the end of the build
jar 'org.jruby:jruby-stdlib', '${jruby.version}', :scope => :provided

scope :test do
  jar 'junit:junit:4.11'
end

properties( 'gem.home' => '../target/rubygems',
            'gem.path' => '${gem.home}',
            'tesla.dump.pom' => 'pom.xml',
            'tesla.dump.readonly' => true )

# vim: syntax=Ruby
