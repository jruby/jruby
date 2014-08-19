# two jars with embedded gems
jar 'org.rubygems:gem1', '1'
jar 'org.rubygems:gem2', '2'

# jruby scripting container
pom 'org.jruby:jruby', '1.7.14.dev-SNAPSHOT'#@project.version@'

# unit tests
jar 'junit:junit', '4.8.2', :scope => :test

resource :directory => "src/main/ruby"
