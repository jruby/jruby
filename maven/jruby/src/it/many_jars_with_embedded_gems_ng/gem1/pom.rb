# frozen_string_literal: true

# -*- mode: ruby -*-

gemfile

model.repositories.clear

extension 'org.jruby.maven:mavengem-wagon:2.0.2'
repository id: :mavengems, url: 'mavengem:https://rubygems.org'

id 'org.rubygems:gem1', '1'

jruby_plugin! :gem, includeRubygemsInResources: true, jrubyVersion: '9.0.0.0'
