# This file is used by Rack-based servers to start the application.

# fix the env to development since production needs more setup
ENV['RAILS_ENV'] = 'development'

require ::File.expand_path('../config/environment', __FILE__)
run Rails.application
