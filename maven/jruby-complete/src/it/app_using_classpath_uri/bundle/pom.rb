#-*- mode: ruby -*-

id 'org.jruby.osgi:bundle', '1.0'

packaging 'bundle'

# add some ruby scripts to bundle
resource :directory => 'src/main/ruby'

plugin( 'org.apache.felix:maven-bundle-plugin', '2.4.0',
        :instructions => {
          'Export-Package' => 'org.jruby.osgi.bundle',
          'Include-Resource' => '{maven-resources}'
        } ) do
  # TODO fix DSL
  @current.extensions = true
end
