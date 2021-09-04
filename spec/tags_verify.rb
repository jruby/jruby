require 'xmlrpc/client'
require 'pp'
require 'set'

# Simple script to scan all JRuby's rubyspec tags
# and report invalid ones, i.e. those that have
# references to already resolved/closed bugs.

if ARGV.size != 2
  puts "Usage: tags_verify.rb jira-login jira-password"
  exit 1
end

class JiraVerifier
  INVALID_STATUSES = {"5"=>"Resolved", "6"=>"Closed"}
  def initialize(login, pass)
    @server = XMLRPC::Client.new('jira.codehaus.org', "/rpc/xmlrpc")
    @auth =  @server.call("jira1.login", login, pass)
    @stat_info = {}
    @server.call("jira1.getStatuses", @auth).each { |s|
      @stat_info[s["id"]] = s["name"]
    }
    # pp @stat_info
  end
  def get_issue(id)
    @server.call("jira1.getIssue", @auth, id)
  end
  def get_status(issue)
    issue["status"]
  end
  def is_valid(issue)
    INVALID_STATUSES[get_status(issue)].nil?
  end
  def check_ids(ids)
    ids.each { |id, files|
      issue = get_issue(id)
      unless (is_valid(issue))
        puts "#{id} -- Wrong State: #{@stat_info[get_status(issue)]}. Used in the following tag files:"
        files.each { |filename|
          puts "  - #{filename}"
        }
      end
    }
  end
  def logout
    @server.call("jira1.logout", @auth)
  end
end

TAGS_DIR = File.expand_path(File.join(File.dirname(__FILE__), 'tags'))
puts "Verifying tags in '#{TAGS_DIR}'..."

ids = {}

Dir.glob(TAGS_DIR + "/**/*.txt").each { |filename|
  File.open(filename, 'r') { |f|
    while (line = f.gets)
      line.scan(/JRUBY-\d+/) { |id|
        (ids[id] ||= Set.new) << filename
      }
    end
  }
}

puts "#{ids.size} JRuby jira issues used in the tags."

jira = JiraVerifier.new(ARGV[0], ARGV[1])
jira.check_ids(ids)
jira.logout
