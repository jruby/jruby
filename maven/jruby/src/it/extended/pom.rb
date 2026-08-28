# frozen_string_literal: true

# jruby scripting container
pom 'org.jruby:jruby', '${jruby.version}'

# unit tests
jar 'junit:junit', '4.8.2', scope: :test

properties "tesla.dump.pom": 'pom.xml', "tesla.dump.readOnly": true

plugin :surefire, '2.15', reuseForks: false,
                          additionalClasspathElements: ['${basedir}/../../../../../core/target/test-classes', '${basedir}/../../../../../test/target/test-classes']
