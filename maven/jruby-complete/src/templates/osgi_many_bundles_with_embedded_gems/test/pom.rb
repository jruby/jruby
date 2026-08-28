# frozen_string_literal: true

# -*- mode: ruby -*-

properties("exam.version": '3.0.3',
           "url.version": '1.5.2',
           "logback.version": '1.0.13')

bundle 'org.jruby:jruby-complete', '${jruby.version}'
bundle 'org.jruby.osgi:gems-bundle', '1.0'
bundle 'org.jruby.osgi:scripts-bundle', '1.0'

plugin('org.apache.felix:maven-bundle-plugin', '2.4.0') do
  # TODO: fix DSL
  @current.extensions = true
end

scope :test do
  jar 'junit:junit:4.11'
  jar 'org.osgi:org.osgi.core:5.0.0'

  jar 'org.ops4j.pax.exam:pax-exam-link-mvn', '${exam.version}'
  jar 'org.ops4j.pax.exam:pax-exam-junit4', '${exam.version}'
  jar 'org.ops4j.pax.exam:pax-exam-container-forked', '${exam.version}'
  jar 'org.ops4j.pax.url:pax-url-aether', '${url.version}'

  jar 'ch.qos.logback:logback-core', '${logback.version}'
  jar 'ch.qos.logback:logback-classic', '${logback.version}'

  profile id: 'equinox-3.6' do
    jar 'org.eclipse.osgi:org.eclipse.osgi:3.6.0.v20100517'
  end
  profile id: 'equinox-3.7' do
    jar 'org.eclipse.osgi:org.eclipse.osgi:3.7.1'
  end
  profile id: 'felix-3.2' do
    jar 'org.apache.felix:org.apache.felix.framework:3.2.2'
  end
  profile id: 'felix-4.4' do
    jar 'org.apache.felix:org.apache.felix.framework:4.4.1'
  end
  profile id: 'knoplerfish' do
    repository(id: :knoplerfish,
               url: 'http://www.knopflerfish.org/maven2')
    jar 'org.knopflerfish:framework:5.1.6'
  end
end
