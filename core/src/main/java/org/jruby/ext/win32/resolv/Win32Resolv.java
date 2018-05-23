package org.jruby.ext.win32.resolv;

import java.util.List;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;

public class Win32Resolv implements Library {
    @JRubyMethod(name = "get_dns_server_list", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject j_get_dns_server_list(ThreadContext context, IRubyObject self) {
        // FIXME it should return name servers only from connected network devices RUBY-BUG #12604
        List nameServers = sun.net.dns.ResolverConfiguration.open().nameservers();
        RubyArray ary = RubyArray.newArray(context.runtime, nameServers.size());
        nameServers.forEach((dns) -> {
            ary.append(context.runtime.newString(dns.toString()));
        });
        return ary;
    }

    public void load(Ruby runtime, boolean wrap) {
        RubyModule rb_cWin32 = runtime.defineModule("Win32");
        RubyModule rb_mResolv = rb_cWin32.defineModuleUnder("Resolv");
        rb_mResolv.defineAnnotatedMethods(Win32Resolv.class);
    }
}
