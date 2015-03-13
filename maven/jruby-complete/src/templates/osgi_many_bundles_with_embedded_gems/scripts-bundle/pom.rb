#-*- mode: ruby -*-

id 'org.jruby.osgi:scripts-bundle', '1.0'

packaging 'bundle'

jar 'org.osgi:org.osgi.core', '5.0.0', :scope => :provided

# add some ruby scripts to bundle
resource :directory => 'src/main/ruby'

plugin( 'org.apache.felix:maven-bundle-plugin', '2.4.0',
        :instructions => {
          'Export-Package' => 'org.jruby.osgi.scripts',
          'Include-Resource' => '{maven-resources}'
        } ) do
  # TODO fix DSL
  @current.extensions = true
end
