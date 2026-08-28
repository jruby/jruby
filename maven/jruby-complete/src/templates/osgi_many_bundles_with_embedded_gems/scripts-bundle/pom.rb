# frozen_string_literal: true

# -*- mode: ruby -*-

id 'org.jruby.osgi:scripts-bundle', '1.0'

packaging 'bundle'

properties( # needed bundle plugin
  "polyglot.dump.pom": 'pom.xml'
)

# add some ruby scripts to bundle
resource directory: 'src/main/ruby'

plugin('org.apache.felix:maven-bundle-plugin', '2.4.0',
       instructions: {
         "Export-Package": 'org.jruby.osgi.scripts',
         "Include-Resource": '{maven-resources}'
       }) do
  # TODO: fix DSL
  @current.extensions = true
end
