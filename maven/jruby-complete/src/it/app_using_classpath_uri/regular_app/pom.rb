jar 'org.jruby.osgi:bundle:1.0'

jar 'org.jruby:jruby-complete', '@project.version@'

# unit tests
jar 'junit:junit', '4.8.2', :scope => :test
