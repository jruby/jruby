# two jars with embedded gems
jar 'org.jruby.maven:maven-tools', '3.0.6-SNAPSHOT'
jar 'org.rubygems:zip', '2.0.2'

# jruby scripting container
pom 'org.jruby:jruby', '${jruby.version}'

# unit tests
jar 'junit:junit', '4.8.2', :scope => :test

resource :directory => "src/main/ruby"
