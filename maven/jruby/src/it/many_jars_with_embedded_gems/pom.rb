# frozen_string_literal: true

# -*- mode: ruby -*-

# default versions will be overwritten by pom.rb from root directory
properties("jruby.plugins.version": '3.0.6')

packaging :pom

modules %w[zip_gem app]
