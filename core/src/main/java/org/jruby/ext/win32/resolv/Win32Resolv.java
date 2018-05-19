package org.jruby.ext.win32.resolv;

import java.util.ArrayList;
import java.util.List;
import jnr.ffi.byref.IntByReference;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.util.WindowsFFI.Iphlpapi;

public class Win32Resolv implements Library {
    @JRubyMethod(name = "get_dns_server_list", module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject j_get_dns_server_list(ThreadContext context, IRubyObject self) {
        List<String> list = getDnsServerList(context.runtime);
        RubyArray ary = RubyArray.newArray(context.runtime, list.size());
        list.forEach((item) -> {
            ary.append(context.runtime.newString(item));
        });
        return ary;
    }

    public static final int ERROR_BUFFER_OVERFLOW = 111;
    public static final int NO_ERROR = 0;

    private static List<String> getDnsServerList(Ruby runtime) {
        IntByReference bufferSize = new IntByReference();
        Iphlpapi api = org.jruby.util.WindowsFFI.getIphlpapi();
        int ret = api.GetNetworkParams(null, bufferSize);
        if (ret != ERROR_BUFFER_OVERFLOW) {
            throw runtime.newRuntimeError("Win32::Resolv::Error");
        }

        Iphlpapi.FIXED_INFO buffer = null; //allocate(bufferSize.getValue().longValue());
        ret = api.GetNetworkParams(buffer, bufferSize);
        if (ret != NO_ERROR) {
            throw runtime.newRuntimeError("Win32::Resolv::Error");
        }

        List<String> list = new ArrayList<>();
        Iphlpapi.IP_ADDR_STRING ipaddr = buffer.DnsServerList;

        do {
            String addr = new String(ipaddr.IpAddress.String);
            int pos = addr.indexOf(0);
            if (pos != -1) {
                addr = addr.substring(0, pos);
            }
            if (!addr.equals("0.0.0.0")) {
                list.add(addr);
            }
        } while ((ipaddr = ipaddr.Next) != null);

        return list;
    }

    public void load(Ruby runtime, boolean wrap) {
        RubyModule rb_cWin32 = runtime.defineModule("Win32");
        RubyModule rb_mResolv = rb_cWin32.defineModuleUnder("Resolv");
        rb_mResolv.defineAnnotatedMethods(Win32Resolv.class);
    }
}
