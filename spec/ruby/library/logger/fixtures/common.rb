require 'logger'

module LoggerSpecs

  def self.strip_date(str)
    str.gsub(/[A-Z].*\[.*\]/, "").lstrip
  end

  # Use defined? so that even with --unguarded this fixture can load
  # ruby_version_is ''...'2.2' do
  if defined?(Logger::Application)
    class TestApp < Logger::Application
      def initialize(appname, log_file=nil)
        super(appname)
        self.set_log(log_file) if log_file
      end

      def run
        log(WARN, "Test log message")
      end
    end
  end
end
