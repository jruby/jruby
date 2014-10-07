#-*- mode: ruby -*-

gemfile

id 'org.jruby.osgi:gems-bundle', '1.0'

packaging 'bundle'

jar 'org.osgi:org.osgi.core', '5.0.0', :scope => :provided

jruby_plugin! :gem, :includeRubygemsInResources => true

# ruby-maven will dump an equivalent pom.xml
properties( 'tesla.dump.pom' => 'pom.xml',
            'jruby.home' => '${project.basedir}/../../../../../../' )

execute 'jrubydir', 'process-resources' do |ctx|
  require 'jruby/commands'
  JRuby::Commands.generate_dir_info( ctx.project.build.directory.to_pathname + '/rubygems' )
end

plugin( 'org.apache.felix:maven-bundle-plugin', '2.4.0',
        :instructions => {
          'Export-Package' => 'org.jruby.osgi.gems',
          'Include-Resource' => '{maven-resources}'
        } ) do
  # TODO fix DSL
  @current.extensions = true
end
