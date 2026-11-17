# frozen_string_literal: true

# jruby scripting container
jar 'org.jruby:jruby-core', '${jruby.version}'
jar 'org.jruby:jruby-stdlib', '${jruby.version}'

# unit tests
jar 'junit:junit', '4.8.2', scope: :test

plugin :surefire, '2.15', reuseForks: false,
                          additionalClasspathElements: ['${basedir}/../../../../../core/target/test-classes', '${basedir}/../../../../../test/target/test-classes']
