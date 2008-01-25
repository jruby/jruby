/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2008 Ola Bini <ola.bini@gmail.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.socket;

import java.util.Map;
import java.util.HashMap;

public abstract class IANA {
    private IANA() {}

    public final static Map<String, Integer> serviceToPort = new HashMap<String, Integer>();
    public final static Map<Integer, String> portToService = new HashMap<Integer, String>();

    static {
        serviceToPort.put("spr-itunes/tcp", 0);
        portToService.put(0, "spr-itunes/tcp");

        serviceToPort.put("spl-itunes/tcp", 0);
        portToService.put(0, "spl-itunes/tcp");

        serviceToPort.put("tcpmux/tcp", 1);
        portToService.put(1, "tcpmux/tcp");

        serviceToPort.put("tcpmux/udp", 1);
        portToService.put(1, "tcpmux/udp");

        serviceToPort.put("compressnet/tcp", 2);
        portToService.put(2, "compressnet/tcp");

        serviceToPort.put("compressnet/udp", 2);
        portToService.put(2, "compressnet/udp");

        serviceToPort.put("compressnet/tcp", 3);
        portToService.put(3, "compressnet/tcp");

        serviceToPort.put("compressnet/udp", 3);
        portToService.put(3, "compressnet/udp");

        serviceToPort.put("rje/tcp", 5);
        portToService.put(5, "rje/tcp");

        serviceToPort.put("rje/udp", 5);
        portToService.put(5, "rje/udp");

        serviceToPort.put("echo/tcp", 7);
        portToService.put(7, "echo/tcp");

        serviceToPort.put("echo/udp", 7);
        portToService.put(7, "echo/udp");

        serviceToPort.put("discard/tcp", 9);
        portToService.put(9, "discard/tcp");

        serviceToPort.put("discard/udp", 9);
        portToService.put(9, "discard/udp");

        serviceToPort.put("discard/sctp", 9);
        portToService.put(9, "discard/sctp");

        serviceToPort.put("discard/dccp", 9);
        portToService.put(9, "discard/dccp");

        serviceToPort.put("systat/tcp", 11);
        portToService.put(11, "systat/tcp");

        serviceToPort.put("systat/udp", 11);
        portToService.put(11, "systat/udp");

        serviceToPort.put("daytime/tcp", 13);
        portToService.put(13, "daytime/tcp");

        serviceToPort.put("daytime/udp", 13);
        portToService.put(13, "daytime/udp");

        serviceToPort.put("qotd/tcp", 17);
        portToService.put(17, "qotd/tcp");

        serviceToPort.put("qotd/udp", 17);
        portToService.put(17, "qotd/udp");

        serviceToPort.put("msp/tcp", 18);
        portToService.put(18, "msp/tcp");

        serviceToPort.put("msp/udp", 18);
        portToService.put(18, "msp/udp");

        serviceToPort.put("chargen/tcp", 19);
        portToService.put(19, "chargen/tcp");

        serviceToPort.put("chargen/udp", 19);
        portToService.put(19, "chargen/udp");

        serviceToPort.put("ftp-data/tcp", 20);
        portToService.put(20, "ftp-data/tcp");

        serviceToPort.put("ftp-data/udp", 20);
        portToService.put(20, "ftp-data/udp");

        serviceToPort.put("ftp-data/sctp", 20);
        portToService.put(20, "ftp-data/sctp");

        serviceToPort.put("ftp/tcp", 21);
        portToService.put(21, "ftp/tcp");

        serviceToPort.put("ftp/udp", 21);
        portToService.put(21, "ftp/udp");

        serviceToPort.put("ftp/sctp", 21);
        portToService.put(21, "ftp/sctp");

        serviceToPort.put("ssh/tcp", 22);
        portToService.put(22, "ssh/tcp");

        serviceToPort.put("ssh/udp", 22);
        portToService.put(22, "ssh/udp");

        serviceToPort.put("ssh/sctp", 22);
        portToService.put(22, "ssh/sctp");

        serviceToPort.put("telnet/tcp", 23);
        portToService.put(23, "telnet/tcp");

        serviceToPort.put("telnet/udp", 23);
        portToService.put(23, "telnet/udp");

        serviceToPort.put("smtp/tcp", 25);
        portToService.put(25, "smtp/tcp");

        serviceToPort.put("smtp/udp", 25);
        portToService.put(25, "smtp/udp");

        serviceToPort.put("nsw-fe/tcp", 27);
        portToService.put(27, "nsw-fe/tcp");

        serviceToPort.put("nsw-fe/udp", 27);
        portToService.put(27, "nsw-fe/udp");

        serviceToPort.put("msg-icp/tcp", 29);
        portToService.put(29, "msg-icp/tcp");

        serviceToPort.put("msg-icp/udp", 29);
        portToService.put(29, "msg-icp/udp");

        serviceToPort.put("msg-auth/tcp", 31);
        portToService.put(31, "msg-auth/tcp");

        serviceToPort.put("msg-auth/udp", 31);
        portToService.put(31, "msg-auth/udp");

        serviceToPort.put("dsp/tcp", 33);
        portToService.put(33, "dsp/tcp");

        serviceToPort.put("dsp/udp", 33);
        portToService.put(33, "dsp/udp");

        serviceToPort.put("time/tcp", 37);
        portToService.put(37, "time/tcp");

        serviceToPort.put("time/udp", 37);
        portToService.put(37, "time/udp");

        serviceToPort.put("rap/tcp", 38);
        portToService.put(38, "rap/tcp");

        serviceToPort.put("rap/udp", 38);
        portToService.put(38, "rap/udp");

        serviceToPort.put("rlp/tcp", 39);
        portToService.put(39, "rlp/tcp");

        serviceToPort.put("rlp/udp", 39);
        portToService.put(39, "rlp/udp");

        serviceToPort.put("graphics/tcp", 41);
        portToService.put(41, "graphics/tcp");

        serviceToPort.put("graphics/udp", 41);
        portToService.put(41, "graphics/udp");

        serviceToPort.put("name/tcp", 42);
        portToService.put(42, "name/tcp");

        serviceToPort.put("name/udp", 42);
        portToService.put(42, "name/udp");

        serviceToPort.put("nameserver/tcp", 42);
        portToService.put(42, "nameserver/tcp");

        serviceToPort.put("nameserver/udp", 42);
        portToService.put(42, "nameserver/udp");

        serviceToPort.put("nicname/tcp", 43);
        portToService.put(43, "nicname/tcp");

        serviceToPort.put("nicname/udp", 43);
        portToService.put(43, "nicname/udp");

        serviceToPort.put("mpm-flags/tcp", 44);
        portToService.put(44, "mpm-flags/tcp");

        serviceToPort.put("mpm-flags/udp", 44);
        portToService.put(44, "mpm-flags/udp");

        serviceToPort.put("mpm/tcp", 45);
        portToService.put(45, "mpm/tcp");

        serviceToPort.put("mpm/udp", 45);
        portToService.put(45, "mpm/udp");

        serviceToPort.put("mpm-snd/tcp", 46);
        portToService.put(46, "mpm-snd/tcp");

        serviceToPort.put("mpm-snd/udp", 46);
        portToService.put(46, "mpm-snd/udp");

        serviceToPort.put("ni-ftp/tcp", 47);
        portToService.put(47, "ni-ftp/tcp");

        serviceToPort.put("ni-ftp/udp", 47);
        portToService.put(47, "ni-ftp/udp");

        serviceToPort.put("auditd/tcp", 48);
        portToService.put(48, "auditd/tcp");

        serviceToPort.put("auditd/udp", 48);
        portToService.put(48, "auditd/udp");

        serviceToPort.put("tacacs/tcp", 49);
        portToService.put(49, "tacacs/tcp");

        serviceToPort.put("tacacs/udp", 49);
        portToService.put(49, "tacacs/udp");

        serviceToPort.put("re-mail-ck/tcp", 50);
        portToService.put(50, "re-mail-ck/tcp");

        serviceToPort.put("re-mail-ck/udp", 50);
        portToService.put(50, "re-mail-ck/udp");

        serviceToPort.put("la-maint/tcp", 51);
        portToService.put(51, "la-maint/tcp");

        serviceToPort.put("la-maint/udp", 51);
        portToService.put(51, "la-maint/udp");

        serviceToPort.put("xns-time/tcp", 52);
        portToService.put(52, "xns-time/tcp");

        serviceToPort.put("xns-time/udp", 52);
        portToService.put(52, "xns-time/udp");

        serviceToPort.put("domain/tcp", 53);
        portToService.put(53, "domain/tcp");

        serviceToPort.put("domain/udp", 53);
        portToService.put(53, "domain/udp");

        serviceToPort.put("xns-ch/tcp", 54);
        portToService.put(54, "xns-ch/tcp");

        serviceToPort.put("xns-ch/udp", 54);
        portToService.put(54, "xns-ch/udp");

        serviceToPort.put("isi-gl/tcp", 55);
        portToService.put(55, "isi-gl/tcp");

        serviceToPort.put("isi-gl/udp", 55);
        portToService.put(55, "isi-gl/udp");

        serviceToPort.put("xns-auth/tcp", 56);
        portToService.put(56, "xns-auth/tcp");

        serviceToPort.put("xns-auth/udp", 56);
        portToService.put(56, "xns-auth/udp");

        serviceToPort.put("xns-mail/tcp", 58);
        portToService.put(58, "xns-mail/tcp");

        serviceToPort.put("xns-mail/udp", 58);
        portToService.put(58, "xns-mail/udp");

        serviceToPort.put("ni-mail/tcp", 61);
        portToService.put(61, "ni-mail/tcp");

        serviceToPort.put("ni-mail/udp", 61);
        portToService.put(61, "ni-mail/udp");

        serviceToPort.put("acas/tcp", 62);
        portToService.put(62, "acas/tcp");

        serviceToPort.put("acas/udp", 62);
        portToService.put(62, "acas/udp");

        serviceToPort.put("whois++/tcp", 63);
        portToService.put(63, "whois++/tcp");

        serviceToPort.put("whois++/udp", 63);
        portToService.put(63, "whois++/udp");

        serviceToPort.put("covia/tcp", 64);
        portToService.put(64, "covia/tcp");

        serviceToPort.put("covia/udp", 64);
        portToService.put(64, "covia/udp");

        serviceToPort.put("tacacs-ds/tcp", 65);
        portToService.put(65, "tacacs-ds/tcp");

        serviceToPort.put("tacacs-ds/udp", 65);
        portToService.put(65, "tacacs-ds/udp");

        serviceToPort.put("sql*net/tcp", 66);
        portToService.put(66, "sql*net/tcp");

        serviceToPort.put("sql*net/udp", 66);
        portToService.put(66, "sql*net/udp");

        serviceToPort.put("bootps/tcp", 67);
        portToService.put(67, "bootps/tcp");

        serviceToPort.put("bootps/udp", 67);
        portToService.put(67, "bootps/udp");

        serviceToPort.put("bootpc/tcp", 68);
        portToService.put(68, "bootpc/tcp");

        serviceToPort.put("bootpc/udp", 68);
        portToService.put(68, "bootpc/udp");

        serviceToPort.put("tftp/tcp", 69);
        portToService.put(69, "tftp/tcp");

        serviceToPort.put("tftp/udp", 69);
        portToService.put(69, "tftp/udp");

        serviceToPort.put("gopher/tcp", 70);
        portToService.put(70, "gopher/tcp");

        serviceToPort.put("gopher/udp", 70);
        portToService.put(70, "gopher/udp");

        serviceToPort.put("netrjs-1/tcp", 71);
        portToService.put(71, "netrjs-1/tcp");

        serviceToPort.put("netrjs-1/udp", 71);
        portToService.put(71, "netrjs-1/udp");

        serviceToPort.put("netrjs-2/tcp", 72);
        portToService.put(72, "netrjs-2/tcp");

        serviceToPort.put("netrjs-2/udp", 72);
        portToService.put(72, "netrjs-2/udp");

        serviceToPort.put("netrjs-3/tcp", 73);
        portToService.put(73, "netrjs-3/tcp");

        serviceToPort.put("netrjs-3/udp", 73);
        portToService.put(73, "netrjs-3/udp");

        serviceToPort.put("netrjs-4/tcp", 74);
        portToService.put(74, "netrjs-4/tcp");

        serviceToPort.put("netrjs-4/udp", 74);
        portToService.put(74, "netrjs-4/udp");

        serviceToPort.put("deos/tcp", 76);
        portToService.put(76, "deos/tcp");

        serviceToPort.put("deos/udp", 76);
        portToService.put(76, "deos/udp");

        serviceToPort.put("vettcp/tcp", 78);
        portToService.put(78, "vettcp/tcp");

        serviceToPort.put("vettcp/udp", 78);
        portToService.put(78, "vettcp/udp");

        serviceToPort.put("finger/tcp", 79);
        portToService.put(79, "finger/tcp");

        serviceToPort.put("finger/udp", 79);
        portToService.put(79, "finger/udp");

        serviceToPort.put("http/tcp", 80);
        portToService.put(80, "http/tcp");

        serviceToPort.put("http/udp", 80);
        portToService.put(80, "http/udp");

        serviceToPort.put("www/tcp", 80);
        portToService.put(80, "www/tcp");

        serviceToPort.put("www/udp", 80);
        portToService.put(80, "www/udp");

        serviceToPort.put("www-http/tcp", 80);
        portToService.put(80, "www-http/tcp");

        serviceToPort.put("www-http/udp", 80);
        portToService.put(80, "www-http/udp");

        serviceToPort.put("http/sctp", 80);
        portToService.put(80, "http/sctp");

        serviceToPort.put("xfer/tcp", 82);
        portToService.put(82, "xfer/tcp");

        serviceToPort.put("xfer/udp", 82);
        portToService.put(82, "xfer/udp");

        serviceToPort.put("mit-ml-dev/tcp", 83);
        portToService.put(83, "mit-ml-dev/tcp");

        serviceToPort.put("mit-ml-dev/udp", 83);
        portToService.put(83, "mit-ml-dev/udp");

        serviceToPort.put("ctf/tcp", 84);
        portToService.put(84, "ctf/tcp");

        serviceToPort.put("ctf/udp", 84);
        portToService.put(84, "ctf/udp");

        serviceToPort.put("mit-ml-dev/tcp", 85);
        portToService.put(85, "mit-ml-dev/tcp");

        serviceToPort.put("mit-ml-dev/udp", 85);
        portToService.put(85, "mit-ml-dev/udp");

        serviceToPort.put("mfcobol/tcp", 86);
        portToService.put(86, "mfcobol/tcp");

        serviceToPort.put("mfcobol/udp", 86);
        portToService.put(86, "mfcobol/udp");

        serviceToPort.put("kerberos/tcp", 88);
        portToService.put(88, "kerberos/tcp");

        serviceToPort.put("kerberos/udp", 88);
        portToService.put(88, "kerberos/udp");

        serviceToPort.put("su-mit-tg/tcp", 89);
        portToService.put(89, "su-mit-tg/tcp");

        serviceToPort.put("su-mit-tg/udp", 89);
        portToService.put(89, "su-mit-tg/udp");

        serviceToPort.put("dnsix/tcp", 90);
        portToService.put(90, "dnsix/tcp");

        serviceToPort.put("dnsix/udp", 90);
        portToService.put(90, "dnsix/udp");

        serviceToPort.put("mit-dov/tcp", 91);
        portToService.put(91, "mit-dov/tcp");

        serviceToPort.put("mit-dov/udp", 91);
        portToService.put(91, "mit-dov/udp");

        serviceToPort.put("npp/tcp", 92);
        portToService.put(92, "npp/tcp");

        serviceToPort.put("npp/udp", 92);
        portToService.put(92, "npp/udp");

        serviceToPort.put("dcp/tcp", 93);
        portToService.put(93, "dcp/tcp");

        serviceToPort.put("dcp/udp", 93);
        portToService.put(93, "dcp/udp");

        serviceToPort.put("objcall/tcp", 94);
        portToService.put(94, "objcall/tcp");

        serviceToPort.put("objcall/udp", 94);
        portToService.put(94, "objcall/udp");

        serviceToPort.put("supdup/tcp", 95);
        portToService.put(95, "supdup/tcp");

        serviceToPort.put("supdup/udp", 95);
        portToService.put(95, "supdup/udp");

        serviceToPort.put("dixie/tcp", 96);
        portToService.put(96, "dixie/tcp");

        serviceToPort.put("dixie/udp", 96);
        portToService.put(96, "dixie/udp");

        serviceToPort.put("swift-rvf/tcp", 97);
        portToService.put(97, "swift-rvf/tcp");

        serviceToPort.put("swift-rvf/udp", 97);
        portToService.put(97, "swift-rvf/udp");

        serviceToPort.put("tacnews/tcp", 98);
        portToService.put(98, "tacnews/tcp");

        serviceToPort.put("tacnews/udp", 98);
        portToService.put(98, "tacnews/udp");

        serviceToPort.put("metagram/tcp", 99);
        portToService.put(99, "metagram/tcp");

        serviceToPort.put("metagram/udp", 99);
        portToService.put(99, "metagram/udp");

        serviceToPort.put("newacct/tcp", 100);
        portToService.put(100, "newacct/tcp");

        serviceToPort.put("hostname/tcp", 101);
        portToService.put(101, "hostname/tcp");

        serviceToPort.put("hostname/udp", 101);
        portToService.put(101, "hostname/udp");

        serviceToPort.put("iso-tsap/tcp", 102);
        portToService.put(102, "iso-tsap/tcp");

        serviceToPort.put("iso-tsap/udp", 102);
        portToService.put(102, "iso-tsap/udp");

        serviceToPort.put("gppitnp/tcp", 103);
        portToService.put(103, "gppitnp/tcp");

        serviceToPort.put("gppitnp/udp", 103);
        portToService.put(103, "gppitnp/udp");

        serviceToPort.put("acr-nema/tcp", 104);
        portToService.put(104, "acr-nema/tcp");

        serviceToPort.put("acr-nema/udp", 104);
        portToService.put(104, "acr-nema/udp");

        serviceToPort.put("cso/tcp", 105);
        portToService.put(105, "cso/tcp");

        serviceToPort.put("cso/udp", 105);
        portToService.put(105, "cso/udp");

        serviceToPort.put("csnet-ns/tcp", 105);
        portToService.put(105, "csnet-ns/tcp");

        serviceToPort.put("csnet-ns/udp", 105);
        portToService.put(105, "csnet-ns/udp");

        serviceToPort.put("3com-tsmux/tcp", 106);
        portToService.put(106, "3com-tsmux/tcp");

        serviceToPort.put("3com-tsmux/udp", 106);
        portToService.put(106, "3com-tsmux/udp");

        serviceToPort.put("rtelnet/tcp", 107);
        portToService.put(107, "rtelnet/tcp");

        serviceToPort.put("rtelnet/udp", 107);
        portToService.put(107, "rtelnet/udp");

        serviceToPort.put("snagas/tcp", 108);
        portToService.put(108, "snagas/tcp");

        serviceToPort.put("snagas/udp", 108);
        portToService.put(108, "snagas/udp");

        serviceToPort.put("pop2/tcp", 109);
        portToService.put(109, "pop2/tcp");

        serviceToPort.put("pop2/udp", 109);
        portToService.put(109, "pop2/udp");

        serviceToPort.put("pop3/tcp", 110);
        portToService.put(110, "pop3/tcp");

        serviceToPort.put("pop3/udp", 110);
        portToService.put(110, "pop3/udp");

        serviceToPort.put("sunrpc/tcp", 111);
        portToService.put(111, "sunrpc/tcp");

        serviceToPort.put("sunrpc/udp", 111);
        portToService.put(111, "sunrpc/udp");

        serviceToPort.put("mcidas/tcp", 112);
        portToService.put(112, "mcidas/tcp");

        serviceToPort.put("mcidas/udp", 112);
        portToService.put(112, "mcidas/udp");

        serviceToPort.put("ident/tcp", 113);
        portToService.put(113, "ident/tcp");

        serviceToPort.put("auth/tcp", 113);
        portToService.put(113, "auth/tcp");

        serviceToPort.put("auth/udp", 113);
        portToService.put(113, "auth/udp");

        serviceToPort.put("sftp/tcp", 115);
        portToService.put(115, "sftp/tcp");

        serviceToPort.put("sftp/udp", 115);
        portToService.put(115, "sftp/udp");

        serviceToPort.put("ansanotify/tcp", 116);
        portToService.put(116, "ansanotify/tcp");

        serviceToPort.put("ansanotify/udp", 116);
        portToService.put(116, "ansanotify/udp");

        serviceToPort.put("uucp-path/tcp", 117);
        portToService.put(117, "uucp-path/tcp");

        serviceToPort.put("uucp-path/udp", 117);
        portToService.put(117, "uucp-path/udp");

        serviceToPort.put("sqlserv/tcp", 118);
        portToService.put(118, "sqlserv/tcp");

        serviceToPort.put("sqlserv/udp", 118);
        portToService.put(118, "sqlserv/udp");

        serviceToPort.put("nntp/tcp", 119);
        portToService.put(119, "nntp/tcp");

        serviceToPort.put("nntp/udp", 119);
        portToService.put(119, "nntp/udp");

        serviceToPort.put("cfdptkt/tcp", 120);
        portToService.put(120, "cfdptkt/tcp");

        serviceToPort.put("cfdptkt/udp", 120);
        portToService.put(120, "cfdptkt/udp");

        serviceToPort.put("erpc/tcp", 121);
        portToService.put(121, "erpc/tcp");

        serviceToPort.put("erpc/udp", 121);
        portToService.put(121, "erpc/udp");

        serviceToPort.put("smakynet/tcp", 122);
        portToService.put(122, "smakynet/tcp");

        serviceToPort.put("smakynet/udp", 122);
        portToService.put(122, "smakynet/udp");

        serviceToPort.put("ntp/tcp", 123);
        portToService.put(123, "ntp/tcp");

        serviceToPort.put("ntp/udp", 123);
        portToService.put(123, "ntp/udp");

        serviceToPort.put("ansatrader/tcp", 124);
        portToService.put(124, "ansatrader/tcp");

        serviceToPort.put("ansatrader/udp", 124);
        portToService.put(124, "ansatrader/udp");

        serviceToPort.put("locus-map/tcp", 125);
        portToService.put(125, "locus-map/tcp");

        serviceToPort.put("locus-map/udp", 125);
        portToService.put(125, "locus-map/udp");

        serviceToPort.put("nxedit/tcp", 126);
        portToService.put(126, "nxedit/tcp");

        serviceToPort.put("nxedit/udp", 126);
        portToService.put(126, "nxedit/udp");

        serviceToPort.put("locus-con/tcp", 127);
        portToService.put(127, "locus-con/tcp");

        serviceToPort.put("locus-con/udp", 127);
        portToService.put(127, "locus-con/udp");

        serviceToPort.put("gss-xlicen/tcp", 128);
        portToService.put(128, "gss-xlicen/tcp");

        serviceToPort.put("gss-xlicen/udp", 128);
        portToService.put(128, "gss-xlicen/udp");

        serviceToPort.put("pwdgen/tcp", 129);
        portToService.put(129, "pwdgen/tcp");

        serviceToPort.put("pwdgen/udp", 129);
        portToService.put(129, "pwdgen/udp");

        serviceToPort.put("cisco-fna/tcp", 130);
        portToService.put(130, "cisco-fna/tcp");

        serviceToPort.put("cisco-fna/udp", 130);
        portToService.put(130, "cisco-fna/udp");

        serviceToPort.put("cisco-tna/tcp", 131);
        portToService.put(131, "cisco-tna/tcp");

        serviceToPort.put("cisco-tna/udp", 131);
        portToService.put(131, "cisco-tna/udp");

        serviceToPort.put("cisco-sys/tcp", 132);
        portToService.put(132, "cisco-sys/tcp");

        serviceToPort.put("cisco-sys/udp", 132);
        portToService.put(132, "cisco-sys/udp");

        serviceToPort.put("statsrv/tcp", 133);
        portToService.put(133, "statsrv/tcp");

        serviceToPort.put("statsrv/udp", 133);
        portToService.put(133, "statsrv/udp");

        serviceToPort.put("ingres-net/tcp", 134);
        portToService.put(134, "ingres-net/tcp");

        serviceToPort.put("ingres-net/udp", 134);
        portToService.put(134, "ingres-net/udp");

        serviceToPort.put("epmap/tcp", 135);
        portToService.put(135, "epmap/tcp");

        serviceToPort.put("epmap/udp", 135);
        portToService.put(135, "epmap/udp");

        serviceToPort.put("profile/tcp", 136);
        portToService.put(136, "profile/tcp");

        serviceToPort.put("profile/udp", 136);
        portToService.put(136, "profile/udp");

        serviceToPort.put("netbios-ns/tcp", 137);
        portToService.put(137, "netbios-ns/tcp");

        serviceToPort.put("netbios-ns/udp", 137);
        portToService.put(137, "netbios-ns/udp");

        serviceToPort.put("netbios-dgm/tcp", 138);
        portToService.put(138, "netbios-dgm/tcp");

        serviceToPort.put("netbios-dgm/udp", 138);
        portToService.put(138, "netbios-dgm/udp");

        serviceToPort.put("netbios-ssn/tcp", 139);
        portToService.put(139, "netbios-ssn/tcp");

        serviceToPort.put("netbios-ssn/udp", 139);
        portToService.put(139, "netbios-ssn/udp");

        serviceToPort.put("emfis-data/tcp", 140);
        portToService.put(140, "emfis-data/tcp");

        serviceToPort.put("emfis-data/udp", 140);
        portToService.put(140, "emfis-data/udp");

        serviceToPort.put("emfis-cntl/tcp", 141);
        portToService.put(141, "emfis-cntl/tcp");

        serviceToPort.put("emfis-cntl/udp", 141);
        portToService.put(141, "emfis-cntl/udp");

        serviceToPort.put("bl-idm/tcp", 142);
        portToService.put(142, "bl-idm/tcp");

        serviceToPort.put("bl-idm/udp", 142);
        portToService.put(142, "bl-idm/udp");

        serviceToPort.put("imap/tcp", 143);
        portToService.put(143, "imap/tcp");

        serviceToPort.put("imap/udp", 143);
        portToService.put(143, "imap/udp");

        serviceToPort.put("uma/tcp", 144);
        portToService.put(144, "uma/tcp");

        serviceToPort.put("uma/udp", 144);
        portToService.put(144, "uma/udp");

        serviceToPort.put("uaac/tcp", 145);
        portToService.put(145, "uaac/tcp");

        serviceToPort.put("uaac/udp", 145);
        portToService.put(145, "uaac/udp");

        serviceToPort.put("iso-tp0/tcp", 146);
        portToService.put(146, "iso-tp0/tcp");

        serviceToPort.put("iso-tp0/udp", 146);
        portToService.put(146, "iso-tp0/udp");

        serviceToPort.put("iso-ip/tcp", 147);
        portToService.put(147, "iso-ip/tcp");

        serviceToPort.put("iso-ip/udp", 147);
        portToService.put(147, "iso-ip/udp");

        serviceToPort.put("jargon/tcp", 148);
        portToService.put(148, "jargon/tcp");

        serviceToPort.put("jargon/udp", 148);
        portToService.put(148, "jargon/udp");

        serviceToPort.put("aed-512/tcp", 149);
        portToService.put(149, "aed-512/tcp");

        serviceToPort.put("aed-512/udp", 149);
        portToService.put(149, "aed-512/udp");

        serviceToPort.put("sql-net/tcp", 150);
        portToService.put(150, "sql-net/tcp");

        serviceToPort.put("sql-net/udp", 150);
        portToService.put(150, "sql-net/udp");

        serviceToPort.put("hems/tcp", 151);
        portToService.put(151, "hems/tcp");

        serviceToPort.put("hems/udp", 151);
        portToService.put(151, "hems/udp");

        serviceToPort.put("bftp/tcp", 152);
        portToService.put(152, "bftp/tcp");

        serviceToPort.put("bftp/udp", 152);
        portToService.put(152, "bftp/udp");

        serviceToPort.put("sgmp/tcp", 153);
        portToService.put(153, "sgmp/tcp");

        serviceToPort.put("sgmp/udp", 153);
        portToService.put(153, "sgmp/udp");

        serviceToPort.put("netsc-prod/tcp", 154);
        portToService.put(154, "netsc-prod/tcp");

        serviceToPort.put("netsc-prod/udp", 154);
        portToService.put(154, "netsc-prod/udp");

        serviceToPort.put("netsc-dev/tcp", 155);
        portToService.put(155, "netsc-dev/tcp");

        serviceToPort.put("netsc-dev/udp", 155);
        portToService.put(155, "netsc-dev/udp");

        serviceToPort.put("sqlsrv/tcp", 156);
        portToService.put(156, "sqlsrv/tcp");

        serviceToPort.put("sqlsrv/udp", 156);
        portToService.put(156, "sqlsrv/udp");

        serviceToPort.put("knet-cmp/tcp", 157);
        portToService.put(157, "knet-cmp/tcp");

        serviceToPort.put("knet-cmp/udp", 157);
        portToService.put(157, "knet-cmp/udp");

        serviceToPort.put("pcmail-srv/tcp", 158);
        portToService.put(158, "pcmail-srv/tcp");

        serviceToPort.put("pcmail-srv/udp", 158);
        portToService.put(158, "pcmail-srv/udp");

        serviceToPort.put("nss-routing/tcp", 159);
        portToService.put(159, "nss-routing/tcp");

        serviceToPort.put("nss-routing/udp", 159);
        portToService.put(159, "nss-routing/udp");

        serviceToPort.put("sgmp-traps/tcp", 160);
        portToService.put(160, "sgmp-traps/tcp");

        serviceToPort.put("sgmp-traps/udp", 160);
        portToService.put(160, "sgmp-traps/udp");

        serviceToPort.put("snmp/tcp", 161);
        portToService.put(161, "snmp/tcp");

        serviceToPort.put("snmp/udp", 161);
        portToService.put(161, "snmp/udp");

        serviceToPort.put("snmptrap/tcp", 162);
        portToService.put(162, "snmptrap/tcp");

        serviceToPort.put("snmptrap/udp", 162);
        portToService.put(162, "snmptrap/udp");

        serviceToPort.put("cmip-man/tcp", 163);
        portToService.put(163, "cmip-man/tcp");

        serviceToPort.put("cmip-man/udp", 163);
        portToService.put(163, "cmip-man/udp");

        serviceToPort.put("cmip-agent/tcp", 164);
        portToService.put(164, "cmip-agent/tcp");

        serviceToPort.put("cmip-agent/udp", 164);
        portToService.put(164, "cmip-agent/udp");

        serviceToPort.put("xns-courier/tcp", 165);
        portToService.put(165, "xns-courier/tcp");

        serviceToPort.put("xns-courier/udp", 165);
        portToService.put(165, "xns-courier/udp");

        serviceToPort.put("s-net/tcp", 166);
        portToService.put(166, "s-net/tcp");

        serviceToPort.put("s-net/udp", 166);
        portToService.put(166, "s-net/udp");

        serviceToPort.put("namp/tcp", 167);
        portToService.put(167, "namp/tcp");

        serviceToPort.put("namp/udp", 167);
        portToService.put(167, "namp/udp");

        serviceToPort.put("rsvd/tcp", 168);
        portToService.put(168, "rsvd/tcp");

        serviceToPort.put("rsvd/udp", 168);
        portToService.put(168, "rsvd/udp");

        serviceToPort.put("send/tcp", 169);
        portToService.put(169, "send/tcp");

        serviceToPort.put("send/udp", 169);
        portToService.put(169, "send/udp");

        serviceToPort.put("print-srv/tcp", 170);
        portToService.put(170, "print-srv/tcp");

        serviceToPort.put("print-srv/udp", 170);
        portToService.put(170, "print-srv/udp");

        serviceToPort.put("multiplex/tcp", 171);
        portToService.put(171, "multiplex/tcp");

        serviceToPort.put("multiplex/udp", 171);
        portToService.put(171, "multiplex/udp");

        serviceToPort.put("cl/1/tcp", 172);
        portToService.put(172, "cl/1/tcp");

        serviceToPort.put("cl/1/udp", 172);
        portToService.put(172, "cl/1/udp");

        serviceToPort.put("xyplex-mux/tcp", 173);
        portToService.put(173, "xyplex-mux/tcp");

        serviceToPort.put("xyplex-mux/udp", 173);
        portToService.put(173, "xyplex-mux/udp");

        serviceToPort.put("mailq/tcp", 174);
        portToService.put(174, "mailq/tcp");

        serviceToPort.put("mailq/udp", 174);
        portToService.put(174, "mailq/udp");

        serviceToPort.put("vmnet/tcp", 175);
        portToService.put(175, "vmnet/tcp");

        serviceToPort.put("vmnet/udp", 175);
        portToService.put(175, "vmnet/udp");

        serviceToPort.put("genrad-mux/tcp", 176);
        portToService.put(176, "genrad-mux/tcp");

        serviceToPort.put("genrad-mux/udp", 176);
        portToService.put(176, "genrad-mux/udp");

        serviceToPort.put("xdmcp/tcp", 177);
        portToService.put(177, "xdmcp/tcp");

        serviceToPort.put("xdmcp/udp", 177);
        portToService.put(177, "xdmcp/udp");

        serviceToPort.put("nextstep/tcp", 178);
        portToService.put(178, "nextstep/tcp");

        serviceToPort.put("nextstep/udp", 178);
        portToService.put(178, "nextstep/udp");

        serviceToPort.put("bgp/tcp", 179);
        portToService.put(179, "bgp/tcp");

        serviceToPort.put("bgp/udp", 179);
        portToService.put(179, "bgp/udp");

        serviceToPort.put("bgp/sctp", 179);
        portToService.put(179, "bgp/sctp");

        serviceToPort.put("ris/tcp", 180);
        portToService.put(180, "ris/tcp");

        serviceToPort.put("ris/udp", 180);
        portToService.put(180, "ris/udp");

        serviceToPort.put("unify/tcp", 181);
        portToService.put(181, "unify/tcp");

        serviceToPort.put("unify/udp", 181);
        portToService.put(181, "unify/udp");

        serviceToPort.put("audit/tcp", 182);
        portToService.put(182, "audit/tcp");

        serviceToPort.put("audit/udp", 182);
        portToService.put(182, "audit/udp");

        serviceToPort.put("ocbinder/tcp", 183);
        portToService.put(183, "ocbinder/tcp");

        serviceToPort.put("ocbinder/udp", 183);
        portToService.put(183, "ocbinder/udp");

        serviceToPort.put("ocserver/tcp", 184);
        portToService.put(184, "ocserver/tcp");

        serviceToPort.put("ocserver/udp", 184);
        portToService.put(184, "ocserver/udp");

        serviceToPort.put("remote-kis/tcp", 185);
        portToService.put(185, "remote-kis/tcp");

        serviceToPort.put("remote-kis/udp", 185);
        portToService.put(185, "remote-kis/udp");

        serviceToPort.put("kis/tcp", 186);
        portToService.put(186, "kis/tcp");

        serviceToPort.put("kis/udp", 186);
        portToService.put(186, "kis/udp");

        serviceToPort.put("aci/tcp", 187);
        portToService.put(187, "aci/tcp");

        serviceToPort.put("aci/udp", 187);
        portToService.put(187, "aci/udp");

        serviceToPort.put("mumps/tcp", 188);
        portToService.put(188, "mumps/tcp");

        serviceToPort.put("mumps/udp", 188);
        portToService.put(188, "mumps/udp");

        serviceToPort.put("qft/tcp", 189);
        portToService.put(189, "qft/tcp");

        serviceToPort.put("qft/udp", 189);
        portToService.put(189, "qft/udp");

        serviceToPort.put("gacp/tcp", 190);
        portToService.put(190, "gacp/tcp");

        serviceToPort.put("gacp/udp", 190);
        portToService.put(190, "gacp/udp");

        serviceToPort.put("prospero/tcp", 191);
        portToService.put(191, "prospero/tcp");

        serviceToPort.put("prospero/udp", 191);
        portToService.put(191, "prospero/udp");

        serviceToPort.put("osu-nms/tcp", 192);
        portToService.put(192, "osu-nms/tcp");

        serviceToPort.put("osu-nms/udp", 192);
        portToService.put(192, "osu-nms/udp");

        serviceToPort.put("srmp/tcp", 193);
        portToService.put(193, "srmp/tcp");

        serviceToPort.put("srmp/udp", 193);
        portToService.put(193, "srmp/udp");

        serviceToPort.put("irc/tcp", 194);
        portToService.put(194, "irc/tcp");

        serviceToPort.put("irc/udp", 194);
        portToService.put(194, "irc/udp");

        serviceToPort.put("dn6-nlm-aud/tcp", 195);
        portToService.put(195, "dn6-nlm-aud/tcp");

        serviceToPort.put("dn6-nlm-aud/udp", 195);
        portToService.put(195, "dn6-nlm-aud/udp");

        serviceToPort.put("dn6-smm-red/tcp", 196);
        portToService.put(196, "dn6-smm-red/tcp");

        serviceToPort.put("dn6-smm-red/udp", 196);
        portToService.put(196, "dn6-smm-red/udp");

        serviceToPort.put("dls/tcp", 197);
        portToService.put(197, "dls/tcp");

        serviceToPort.put("dls/udp", 197);
        portToService.put(197, "dls/udp");

        serviceToPort.put("dls-mon/tcp", 198);
        portToService.put(198, "dls-mon/tcp");

        serviceToPort.put("dls-mon/udp", 198);
        portToService.put(198, "dls-mon/udp");

        serviceToPort.put("smux/tcp", 199);
        portToService.put(199, "smux/tcp");

        serviceToPort.put("smux/udp", 199);
        portToService.put(199, "smux/udp");

        serviceToPort.put("src/tcp", 200);
        portToService.put(200, "src/tcp");

        serviceToPort.put("src/udp", 200);
        portToService.put(200, "src/udp");

        serviceToPort.put("at-rtmp/tcp", 201);
        portToService.put(201, "at-rtmp/tcp");

        serviceToPort.put("at-rtmp/udp", 201);
        portToService.put(201, "at-rtmp/udp");

        serviceToPort.put("at-nbp/tcp", 202);
        portToService.put(202, "at-nbp/tcp");

        serviceToPort.put("at-nbp/udp", 202);
        portToService.put(202, "at-nbp/udp");

        serviceToPort.put("at-3/tcp", 203);
        portToService.put(203, "at-3/tcp");

        serviceToPort.put("at-3/udp", 203);
        portToService.put(203, "at-3/udp");

        serviceToPort.put("at-echo/tcp", 204);
        portToService.put(204, "at-echo/tcp");

        serviceToPort.put("at-echo/udp", 204);
        portToService.put(204, "at-echo/udp");

        serviceToPort.put("at-5/tcp", 205);
        portToService.put(205, "at-5/tcp");

        serviceToPort.put("at-5/udp", 205);
        portToService.put(205, "at-5/udp");

        serviceToPort.put("at-zis/tcp", 206);
        portToService.put(206, "at-zis/tcp");

        serviceToPort.put("at-zis/udp", 206);
        portToService.put(206, "at-zis/udp");

        serviceToPort.put("at-7/tcp", 207);
        portToService.put(207, "at-7/tcp");

        serviceToPort.put("at-7/udp", 207);
        portToService.put(207, "at-7/udp");

        serviceToPort.put("at-8/tcp", 208);
        portToService.put(208, "at-8/tcp");

        serviceToPort.put("at-8/udp", 208);
        portToService.put(208, "at-8/udp");

        serviceToPort.put("qmtp/tcp", 209);
        portToService.put(209, "qmtp/tcp");

        serviceToPort.put("qmtp/udp", 209);
        portToService.put(209, "qmtp/udp");

        serviceToPort.put("z39.50/tcp", 210);
        portToService.put(210, "z39.50/tcp");

        serviceToPort.put("z39.50/udp", 210);
        portToService.put(210, "z39.50/udp");

        serviceToPort.put("914c/g/tcp", 211);
        portToService.put(211, "914c/g/tcp");

        serviceToPort.put("914c/g/udp", 211);
        portToService.put(211, "914c/g/udp");

        serviceToPort.put("anet/tcp", 212);
        portToService.put(212, "anet/tcp");

        serviceToPort.put("anet/udp", 212);
        portToService.put(212, "anet/udp");

        serviceToPort.put("ipx/tcp", 213);
        portToService.put(213, "ipx/tcp");

        serviceToPort.put("ipx/udp", 213);
        portToService.put(213, "ipx/udp");

        serviceToPort.put("vmpwscs/tcp", 214);
        portToService.put(214, "vmpwscs/tcp");

        serviceToPort.put("vmpwscs/udp", 214);
        portToService.put(214, "vmpwscs/udp");

        serviceToPort.put("softpc/tcp", 215);
        portToService.put(215, "softpc/tcp");

        serviceToPort.put("softpc/udp", 215);
        portToService.put(215, "softpc/udp");

        serviceToPort.put("CAIlic/tcp", 216);
        portToService.put(216, "CAIlic/tcp");

        serviceToPort.put("CAIlic/udp", 216);
        portToService.put(216, "CAIlic/udp");

        serviceToPort.put("dbase/tcp", 217);
        portToService.put(217, "dbase/tcp");

        serviceToPort.put("dbase/udp", 217);
        portToService.put(217, "dbase/udp");

        serviceToPort.put("mpp/tcp", 218);
        portToService.put(218, "mpp/tcp");

        serviceToPort.put("mpp/udp", 218);
        portToService.put(218, "mpp/udp");

        serviceToPort.put("uarps/tcp", 219);
        portToService.put(219, "uarps/tcp");

        serviceToPort.put("uarps/udp", 219);
        portToService.put(219, "uarps/udp");

        serviceToPort.put("imap3/tcp", 220);
        portToService.put(220, "imap3/tcp");

        serviceToPort.put("imap3/udp", 220);
        portToService.put(220, "imap3/udp");

        serviceToPort.put("fln-spx/tcp", 221);
        portToService.put(221, "fln-spx/tcp");

        serviceToPort.put("fln-spx/udp", 221);
        portToService.put(221, "fln-spx/udp");

        serviceToPort.put("rsh-spx/tcp", 222);
        portToService.put(222, "rsh-spx/tcp");

        serviceToPort.put("rsh-spx/udp", 222);
        portToService.put(222, "rsh-spx/udp");

        serviceToPort.put("cdc/tcp", 223);
        portToService.put(223, "cdc/tcp");

        serviceToPort.put("cdc/udp", 223);
        portToService.put(223, "cdc/udp");

        serviceToPort.put("masqdialer/tcp", 224);
        portToService.put(224, "masqdialer/tcp");

        serviceToPort.put("masqdialer/udp", 224);
        portToService.put(224, "masqdialer/udp");

        serviceToPort.put("direct/tcp", 242);
        portToService.put(242, "direct/tcp");

        serviceToPort.put("direct/udp", 242);
        portToService.put(242, "direct/udp");

        serviceToPort.put("sur-meas/tcp", 243);
        portToService.put(243, "sur-meas/tcp");

        serviceToPort.put("sur-meas/udp", 243);
        portToService.put(243, "sur-meas/udp");

        serviceToPort.put("inbusiness/tcp", 244);
        portToService.put(244, "inbusiness/tcp");

        serviceToPort.put("inbusiness/udp", 244);
        portToService.put(244, "inbusiness/udp");

        serviceToPort.put("link/tcp", 245);
        portToService.put(245, "link/tcp");

        serviceToPort.put("link/udp", 245);
        portToService.put(245, "link/udp");

        serviceToPort.put("dsp3270/tcp", 246);
        portToService.put(246, "dsp3270/tcp");

        serviceToPort.put("dsp3270/udp", 246);
        portToService.put(246, "dsp3270/udp");

        serviceToPort.put("subntbcst_tftp/tcp", 247);
        portToService.put(247, "subntbcst_tftp/tcp");

        serviceToPort.put("subntbcst_tftp/udp", 247);
        portToService.put(247, "subntbcst_tftp/udp");

        serviceToPort.put("bhfhs/tcp", 248);
        portToService.put(248, "bhfhs/tcp");

        serviceToPort.put("bhfhs/udp", 248);
        portToService.put(248, "bhfhs/udp");

        serviceToPort.put("rap/tcp", 256);
        portToService.put(256, "rap/tcp");

        serviceToPort.put("rap/udp", 256);
        portToService.put(256, "rap/udp");

        serviceToPort.put("set/tcp", 257);
        portToService.put(257, "set/tcp");

        serviceToPort.put("set/udp", 257);
        portToService.put(257, "set/udp");

        serviceToPort.put("esro-gen/tcp", 259);
        portToService.put(259, "esro-gen/tcp");

        serviceToPort.put("esro-gen/udp", 259);
        portToService.put(259, "esro-gen/udp");

        serviceToPort.put("openport/tcp", 260);
        portToService.put(260, "openport/tcp");

        serviceToPort.put("openport/udp", 260);
        portToService.put(260, "openport/udp");

        serviceToPort.put("nsiiops/tcp", 261);
        portToService.put(261, "nsiiops/tcp");

        serviceToPort.put("nsiiops/udp", 261);
        portToService.put(261, "nsiiops/udp");

        serviceToPort.put("arcisdms/tcp", 262);
        portToService.put(262, "arcisdms/tcp");

        serviceToPort.put("arcisdms/udp", 262);
        portToService.put(262, "arcisdms/udp");

        serviceToPort.put("hdap/tcp", 263);
        portToService.put(263, "hdap/tcp");

        serviceToPort.put("hdap/udp", 263);
        portToService.put(263, "hdap/udp");

        serviceToPort.put("bgmp/tcp", 264);
        portToService.put(264, "bgmp/tcp");

        serviceToPort.put("bgmp/udp", 264);
        portToService.put(264, "bgmp/udp");

        serviceToPort.put("x-bone-ctl/tcp", 265);
        portToService.put(265, "x-bone-ctl/tcp");

        serviceToPort.put("x-bone-ctl/udp", 265);
        portToService.put(265, "x-bone-ctl/udp");

        serviceToPort.put("sst/tcp", 266);
        portToService.put(266, "sst/tcp");

        serviceToPort.put("sst/udp", 266);
        portToService.put(266, "sst/udp");

        serviceToPort.put("td-service/tcp", 267);
        portToService.put(267, "td-service/tcp");

        serviceToPort.put("td-service/udp", 267);
        portToService.put(267, "td-service/udp");

        serviceToPort.put("td-replica/tcp", 268);
        portToService.put(268, "td-replica/tcp");

        serviceToPort.put("td-replica/udp", 268);
        portToService.put(268, "td-replica/udp");

        serviceToPort.put("manet/tcp", 269);
        portToService.put(269, "manet/tcp");

        serviceToPort.put("manet/udp", 269);
        portToService.put(269, "manet/udp");

        serviceToPort.put("http-mgmt/tcp", 280);
        portToService.put(280, "http-mgmt/tcp");

        serviceToPort.put("http-mgmt/udp", 280);
        portToService.put(280, "http-mgmt/udp");

        serviceToPort.put("personal-link/tcp", 281);
        portToService.put(281, "personal-link/tcp");

        serviceToPort.put("personal-link/udp", 281);
        portToService.put(281, "personal-link/udp");

        serviceToPort.put("cableport-ax/tcp", 282);
        portToService.put(282, "cableport-ax/tcp");

        serviceToPort.put("cableport-ax/udp", 282);
        portToService.put(282, "cableport-ax/udp");

        serviceToPort.put("rescap/tcp", 283);
        portToService.put(283, "rescap/tcp");

        serviceToPort.put("rescap/udp", 283);
        portToService.put(283, "rescap/udp");

        serviceToPort.put("corerjd/tcp", 284);
        portToService.put(284, "corerjd/tcp");

        serviceToPort.put("corerjd/udp", 284);
        portToService.put(284, "corerjd/udp");

        serviceToPort.put("fxp/tcp", 286);
        portToService.put(286, "fxp/tcp");

        serviceToPort.put("fxp/udp", 286);
        portToService.put(286, "fxp/udp");

        serviceToPort.put("k-block/tcp", 287);
        portToService.put(287, "k-block/tcp");

        serviceToPort.put("k-block/udp", 287);
        portToService.put(287, "k-block/udp");

        serviceToPort.put("novastorbakcup/tcp", 308);
        portToService.put(308, "novastorbakcup/tcp");

        serviceToPort.put("novastorbakcup/udp", 308);
        portToService.put(308, "novastorbakcup/udp");

        serviceToPort.put("entrusttime/tcp", 309);
        portToService.put(309, "entrusttime/tcp");

        serviceToPort.put("entrusttime/udp", 309);
        portToService.put(309, "entrusttime/udp");

        serviceToPort.put("bhmds/tcp", 310);
        portToService.put(310, "bhmds/tcp");

        serviceToPort.put("bhmds/udp", 310);
        portToService.put(310, "bhmds/udp");

        serviceToPort.put("asip-webadmin/tcp", 311);
        portToService.put(311, "asip-webadmin/tcp");

        serviceToPort.put("asip-webadmin/udp", 311);
        portToService.put(311, "asip-webadmin/udp");

        serviceToPort.put("vslmp/tcp", 312);
        portToService.put(312, "vslmp/tcp");

        serviceToPort.put("vslmp/udp", 312);
        portToService.put(312, "vslmp/udp");

        serviceToPort.put("magenta-logic/tcp", 313);
        portToService.put(313, "magenta-logic/tcp");

        serviceToPort.put("magenta-logic/udp", 313);
        portToService.put(313, "magenta-logic/udp");

        serviceToPort.put("opalis-robot/tcp", 314);
        portToService.put(314, "opalis-robot/tcp");

        serviceToPort.put("opalis-robot/udp", 314);
        portToService.put(314, "opalis-robot/udp");

        serviceToPort.put("dpsi/tcp", 315);
        portToService.put(315, "dpsi/tcp");

        serviceToPort.put("dpsi/udp", 315);
        portToService.put(315, "dpsi/udp");

        serviceToPort.put("decauth/tcp", 316);
        portToService.put(316, "decauth/tcp");

        serviceToPort.put("decauth/udp", 316);
        portToService.put(316, "decauth/udp");

        serviceToPort.put("zannet/tcp", 317);
        portToService.put(317, "zannet/tcp");

        serviceToPort.put("zannet/udp", 317);
        portToService.put(317, "zannet/udp");

        serviceToPort.put("pkix-timestamp/tcp", 318);
        portToService.put(318, "pkix-timestamp/tcp");

        serviceToPort.put("pkix-timestamp/udp", 318);
        portToService.put(318, "pkix-timestamp/udp");

        serviceToPort.put("ptp-event/tcp", 319);
        portToService.put(319, "ptp-event/tcp");

        serviceToPort.put("ptp-event/udp", 319);
        portToService.put(319, "ptp-event/udp");

        serviceToPort.put("ptp-general/tcp", 320);
        portToService.put(320, "ptp-general/tcp");

        serviceToPort.put("ptp-general/udp", 320);
        portToService.put(320, "ptp-general/udp");

        serviceToPort.put("pip/tcp", 321);
        portToService.put(321, "pip/tcp");

        serviceToPort.put("pip/udp", 321);
        portToService.put(321, "pip/udp");

        serviceToPort.put("rtsps/tcp", 322);
        portToService.put(322, "rtsps/tcp");

        serviceToPort.put("rtsps/udp", 322);
        portToService.put(322, "rtsps/udp");

        serviceToPort.put("texar/tcp", 333);
        portToService.put(333, "texar/tcp");

        serviceToPort.put("texar/udp", 333);
        portToService.put(333, "texar/udp");

        serviceToPort.put("pdap/tcp", 344);
        portToService.put(344, "pdap/tcp");

        serviceToPort.put("pdap/udp", 344);
        portToService.put(344, "pdap/udp");

        serviceToPort.put("pawserv/tcp", 345);
        portToService.put(345, "pawserv/tcp");

        serviceToPort.put("pawserv/udp", 345);
        portToService.put(345, "pawserv/udp");

        serviceToPort.put("zserv/tcp", 346);
        portToService.put(346, "zserv/tcp");

        serviceToPort.put("zserv/udp", 346);
        portToService.put(346, "zserv/udp");

        serviceToPort.put("fatserv/tcp", 347);
        portToService.put(347, "fatserv/tcp");

        serviceToPort.put("fatserv/udp", 347);
        portToService.put(347, "fatserv/udp");

        serviceToPort.put("csi-sgwp/tcp", 348);
        portToService.put(348, "csi-sgwp/tcp");

        serviceToPort.put("csi-sgwp/udp", 348);
        portToService.put(348, "csi-sgwp/udp");

        serviceToPort.put("mftp/tcp", 349);
        portToService.put(349, "mftp/tcp");

        serviceToPort.put("mftp/udp", 349);
        portToService.put(349, "mftp/udp");

        serviceToPort.put("matip-type-a/tcp", 350);
        portToService.put(350, "matip-type-a/tcp");

        serviceToPort.put("matip-type-a/udp", 350);
        portToService.put(350, "matip-type-a/udp");

        serviceToPort.put("matip-type-b/tcp", 351);
        portToService.put(351, "matip-type-b/tcp");

        serviceToPort.put("matip-type-b/udp", 351);
        portToService.put(351, "matip-type-b/udp");

        serviceToPort.put("bhoetty/tcp", 351);
        portToService.put(351, "bhoetty/tcp");

        serviceToPort.put("bhoetty/udp", 351);
        portToService.put(351, "bhoetty/udp");

        serviceToPort.put("dtag-ste-sb/tcp", 352);
        portToService.put(352, "dtag-ste-sb/tcp");

        serviceToPort.put("dtag-ste-sb/udp", 352);
        portToService.put(352, "dtag-ste-sb/udp");

        serviceToPort.put("bhoedap4/tcp", 352);
        portToService.put(352, "bhoedap4/tcp");

        serviceToPort.put("bhoedap4/udp", 352);
        portToService.put(352, "bhoedap4/udp");

        serviceToPort.put("ndsauth/tcp", 353);
        portToService.put(353, "ndsauth/tcp");

        serviceToPort.put("ndsauth/udp", 353);
        portToService.put(353, "ndsauth/udp");

        serviceToPort.put("bh611/tcp", 354);
        portToService.put(354, "bh611/tcp");

        serviceToPort.put("bh611/udp", 354);
        portToService.put(354, "bh611/udp");

        serviceToPort.put("datex-asn/tcp", 355);
        portToService.put(355, "datex-asn/tcp");

        serviceToPort.put("datex-asn/udp", 355);
        portToService.put(355, "datex-asn/udp");

        serviceToPort.put("cloanto-net-1/tcp", 356);
        portToService.put(356, "cloanto-net-1/tcp");

        serviceToPort.put("cloanto-net-1/udp", 356);
        portToService.put(356, "cloanto-net-1/udp");

        serviceToPort.put("bhevent/tcp", 357);
        portToService.put(357, "bhevent/tcp");

        serviceToPort.put("bhevent/udp", 357);
        portToService.put(357, "bhevent/udp");

        serviceToPort.put("shrinkwrap/tcp", 358);
        portToService.put(358, "shrinkwrap/tcp");

        serviceToPort.put("shrinkwrap/udp", 358);
        portToService.put(358, "shrinkwrap/udp");

        serviceToPort.put("nsrmp/tcp", 359);
        portToService.put(359, "nsrmp/tcp");

        serviceToPort.put("nsrmp/udp", 359);
        portToService.put(359, "nsrmp/udp");

        serviceToPort.put("scoi2odialog/tcp", 360);
        portToService.put(360, "scoi2odialog/tcp");

        serviceToPort.put("scoi2odialog/udp", 360);
        portToService.put(360, "scoi2odialog/udp");

        serviceToPort.put("semantix/tcp", 361);
        portToService.put(361, "semantix/tcp");

        serviceToPort.put("semantix/udp", 361);
        portToService.put(361, "semantix/udp");

        serviceToPort.put("srssend/tcp", 362);
        portToService.put(362, "srssend/tcp");

        serviceToPort.put("srssend/udp", 362);
        portToService.put(362, "srssend/udp");

        serviceToPort.put("rsvp_tunnel/tcp", 363);
        portToService.put(363, "rsvp_tunnel/tcp");

        serviceToPort.put("rsvp_tunnel/udp", 363);
        portToService.put(363, "rsvp_tunnel/udp");

        serviceToPort.put("aurora-cmgr/tcp", 364);
        portToService.put(364, "aurora-cmgr/tcp");

        serviceToPort.put("aurora-cmgr/udp", 364);
        portToService.put(364, "aurora-cmgr/udp");

        serviceToPort.put("dtk/tcp", 365);
        portToService.put(365, "dtk/tcp");

        serviceToPort.put("dtk/udp", 365);
        portToService.put(365, "dtk/udp");

        serviceToPort.put("odmr/tcp", 366);
        portToService.put(366, "odmr/tcp");

        serviceToPort.put("odmr/udp", 366);
        portToService.put(366, "odmr/udp");

        serviceToPort.put("mortgageware/tcp", 367);
        portToService.put(367, "mortgageware/tcp");

        serviceToPort.put("mortgageware/udp", 367);
        portToService.put(367, "mortgageware/udp");

        serviceToPort.put("qbikgdp/tcp", 368);
        portToService.put(368, "qbikgdp/tcp");

        serviceToPort.put("qbikgdp/udp", 368);
        portToService.put(368, "qbikgdp/udp");

        serviceToPort.put("rpc2portmap/tcp", 369);
        portToService.put(369, "rpc2portmap/tcp");

        serviceToPort.put("rpc2portmap/udp", 369);
        portToService.put(369, "rpc2portmap/udp");

        serviceToPort.put("codaauth2/tcp", 370);
        portToService.put(370, "codaauth2/tcp");

        serviceToPort.put("codaauth2/udp", 370);
        portToService.put(370, "codaauth2/udp");

        serviceToPort.put("clearcase/tcp", 371);
        portToService.put(371, "clearcase/tcp");

        serviceToPort.put("clearcase/udp", 371);
        portToService.put(371, "clearcase/udp");

        serviceToPort.put("ulistproc/tcp", 372);
        portToService.put(372, "ulistproc/tcp");

        serviceToPort.put("ulistproc/udp", 372);
        portToService.put(372, "ulistproc/udp");

        serviceToPort.put("legent-1/tcp", 373);
        portToService.put(373, "legent-1/tcp");

        serviceToPort.put("legent-1/udp", 373);
        portToService.put(373, "legent-1/udp");

        serviceToPort.put("legent-2/tcp", 374);
        portToService.put(374, "legent-2/tcp");

        serviceToPort.put("legent-2/udp", 374);
        portToService.put(374, "legent-2/udp");

        serviceToPort.put("hassle/tcp", 375);
        portToService.put(375, "hassle/tcp");

        serviceToPort.put("hassle/udp", 375);
        portToService.put(375, "hassle/udp");

        serviceToPort.put("nip/tcp", 376);
        portToService.put(376, "nip/tcp");

        serviceToPort.put("nip/udp", 376);
        portToService.put(376, "nip/udp");

        serviceToPort.put("tnETOS/tcp", 377);
        portToService.put(377, "tnETOS/tcp");

        serviceToPort.put("tnETOS/udp", 377);
        portToService.put(377, "tnETOS/udp");

        serviceToPort.put("dsETOS/tcp", 378);
        portToService.put(378, "dsETOS/tcp");

        serviceToPort.put("dsETOS/udp", 378);
        portToService.put(378, "dsETOS/udp");

        serviceToPort.put("is99c/tcp", 379);
        portToService.put(379, "is99c/tcp");

        serviceToPort.put("is99c/udp", 379);
        portToService.put(379, "is99c/udp");

        serviceToPort.put("is99s/tcp", 380);
        portToService.put(380, "is99s/tcp");

        serviceToPort.put("is99s/udp", 380);
        portToService.put(380, "is99s/udp");

        serviceToPort.put("hp-collector/tcp", 381);
        portToService.put(381, "hp-collector/tcp");

        serviceToPort.put("hp-collector/udp", 381);
        portToService.put(381, "hp-collector/udp");

        serviceToPort.put("hp-managed-node/tcp", 382);
        portToService.put(382, "hp-managed-node/tcp");

        serviceToPort.put("hp-managed-node/udp", 382);
        portToService.put(382, "hp-managed-node/udp");

        serviceToPort.put("hp-alarm-mgr/tcp", 383);
        portToService.put(383, "hp-alarm-mgr/tcp");

        serviceToPort.put("hp-alarm-mgr/udp", 383);
        portToService.put(383, "hp-alarm-mgr/udp");

        serviceToPort.put("arns/tcp", 384);
        portToService.put(384, "arns/tcp");

        serviceToPort.put("arns/udp", 384);
        portToService.put(384, "arns/udp");

        serviceToPort.put("ibm-app/tcp", 385);
        portToService.put(385, "ibm-app/tcp");

        serviceToPort.put("ibm-app/udp", 385);
        portToService.put(385, "ibm-app/udp");

        serviceToPort.put("asa/tcp", 386);
        portToService.put(386, "asa/tcp");

        serviceToPort.put("asa/udp", 386);
        portToService.put(386, "asa/udp");

        serviceToPort.put("aurp/tcp", 387);
        portToService.put(387, "aurp/tcp");

        serviceToPort.put("aurp/udp", 387);
        portToService.put(387, "aurp/udp");

        serviceToPort.put("unidata-ldm/tcp", 388);
        portToService.put(388, "unidata-ldm/tcp");

        serviceToPort.put("unidata-ldm/udp", 388);
        portToService.put(388, "unidata-ldm/udp");

        serviceToPort.put("ldap/tcp", 389);
        portToService.put(389, "ldap/tcp");

        serviceToPort.put("ldap/udp", 389);
        portToService.put(389, "ldap/udp");

        serviceToPort.put("uis/tcp", 390);
        portToService.put(390, "uis/tcp");

        serviceToPort.put("uis/udp", 390);
        portToService.put(390, "uis/udp");

        serviceToPort.put("synotics-relay/tcp", 391);
        portToService.put(391, "synotics-relay/tcp");

        serviceToPort.put("synotics-relay/udp", 391);
        portToService.put(391, "synotics-relay/udp");

        serviceToPort.put("synotics-broker/tcp", 392);
        portToService.put(392, "synotics-broker/tcp");

        serviceToPort.put("synotics-broker/udp", 392);
        portToService.put(392, "synotics-broker/udp");

        serviceToPort.put("meta5/tcp", 393);
        portToService.put(393, "meta5/tcp");

        serviceToPort.put("meta5/udp", 393);
        portToService.put(393, "meta5/udp");

        serviceToPort.put("embl-ndt/tcp", 394);
        portToService.put(394, "embl-ndt/tcp");

        serviceToPort.put("embl-ndt/udp", 394);
        portToService.put(394, "embl-ndt/udp");

        serviceToPort.put("netcp/tcp", 395);
        portToService.put(395, "netcp/tcp");

        serviceToPort.put("netcp/udp", 395);
        portToService.put(395, "netcp/udp");

        serviceToPort.put("netware-ip/tcp", 396);
        portToService.put(396, "netware-ip/tcp");

        serviceToPort.put("netware-ip/udp", 396);
        portToService.put(396, "netware-ip/udp");

        serviceToPort.put("mptn/tcp", 397);
        portToService.put(397, "mptn/tcp");

        serviceToPort.put("mptn/udp", 397);
        portToService.put(397, "mptn/udp");

        serviceToPort.put("kryptolan/tcp", 398);
        portToService.put(398, "kryptolan/tcp");

        serviceToPort.put("kryptolan/udp", 398);
        portToService.put(398, "kryptolan/udp");

        serviceToPort.put("iso-tsap-c2/tcp", 399);
        portToService.put(399, "iso-tsap-c2/tcp");

        serviceToPort.put("iso-tsap-c2/udp", 399);
        portToService.put(399, "iso-tsap-c2/udp");

        serviceToPort.put("work-sol/tcp", 400);
        portToService.put(400, "work-sol/tcp");

        serviceToPort.put("work-sol/udp", 400);
        portToService.put(400, "work-sol/udp");

        serviceToPort.put("ups/tcp", 401);
        portToService.put(401, "ups/tcp");

        serviceToPort.put("ups/udp", 401);
        portToService.put(401, "ups/udp");

        serviceToPort.put("genie/tcp", 402);
        portToService.put(402, "genie/tcp");

        serviceToPort.put("genie/udp", 402);
        portToService.put(402, "genie/udp");

        serviceToPort.put("decap/tcp", 403);
        portToService.put(403, "decap/tcp");

        serviceToPort.put("decap/udp", 403);
        portToService.put(403, "decap/udp");

        serviceToPort.put("nced/tcp", 404);
        portToService.put(404, "nced/tcp");

        serviceToPort.put("nced/udp", 404);
        portToService.put(404, "nced/udp");

        serviceToPort.put("ncld/tcp", 405);
        portToService.put(405, "ncld/tcp");

        serviceToPort.put("ncld/udp", 405);
        portToService.put(405, "ncld/udp");

        serviceToPort.put("imsp/tcp", 406);
        portToService.put(406, "imsp/tcp");

        serviceToPort.put("imsp/udp", 406);
        portToService.put(406, "imsp/udp");

        serviceToPort.put("timbuktu/tcp", 407);
        portToService.put(407, "timbuktu/tcp");

        serviceToPort.put("timbuktu/udp", 407);
        portToService.put(407, "timbuktu/udp");

        serviceToPort.put("prm-sm/tcp", 408);
        portToService.put(408, "prm-sm/tcp");

        serviceToPort.put("prm-sm/udp", 408);
        portToService.put(408, "prm-sm/udp");

        serviceToPort.put("prm-nm/tcp", 409);
        portToService.put(409, "prm-nm/tcp");

        serviceToPort.put("prm-nm/udp", 409);
        portToService.put(409, "prm-nm/udp");

        serviceToPort.put("decladebug/tcp", 410);
        portToService.put(410, "decladebug/tcp");

        serviceToPort.put("decladebug/udp", 410);
        portToService.put(410, "decladebug/udp");

        serviceToPort.put("rmt/tcp", 411);
        portToService.put(411, "rmt/tcp");

        serviceToPort.put("rmt/udp", 411);
        portToService.put(411, "rmt/udp");

        serviceToPort.put("synoptics-trap/tcp", 412);
        portToService.put(412, "synoptics-trap/tcp");

        serviceToPort.put("synoptics-trap/udp", 412);
        portToService.put(412, "synoptics-trap/udp");

        serviceToPort.put("smsp/tcp", 413);
        portToService.put(413, "smsp/tcp");

        serviceToPort.put("smsp/udp", 413);
        portToService.put(413, "smsp/udp");

        serviceToPort.put("infoseek/tcp", 414);
        portToService.put(414, "infoseek/tcp");

        serviceToPort.put("infoseek/udp", 414);
        portToService.put(414, "infoseek/udp");

        serviceToPort.put("bnet/tcp", 415);
        portToService.put(415, "bnet/tcp");

        serviceToPort.put("bnet/udp", 415);
        portToService.put(415, "bnet/udp");

        serviceToPort.put("silverplatter/tcp", 416);
        portToService.put(416, "silverplatter/tcp");

        serviceToPort.put("silverplatter/udp", 416);
        portToService.put(416, "silverplatter/udp");

        serviceToPort.put("onmux/tcp", 417);
        portToService.put(417, "onmux/tcp");

        serviceToPort.put("onmux/udp", 417);
        portToService.put(417, "onmux/udp");

        serviceToPort.put("hyper-g/tcp", 418);
        portToService.put(418, "hyper-g/tcp");

        serviceToPort.put("hyper-g/udp", 418);
        portToService.put(418, "hyper-g/udp");

        serviceToPort.put("ariel1/tcp", 419);
        portToService.put(419, "ariel1/tcp");

        serviceToPort.put("ariel1/udp", 419);
        portToService.put(419, "ariel1/udp");

        serviceToPort.put("smpte/tcp", 420);
        portToService.put(420, "smpte/tcp");

        serviceToPort.put("smpte/udp", 420);
        portToService.put(420, "smpte/udp");

        serviceToPort.put("ariel2/tcp", 421);
        portToService.put(421, "ariel2/tcp");

        serviceToPort.put("ariel2/udp", 421);
        portToService.put(421, "ariel2/udp");

        serviceToPort.put("ariel3/tcp", 422);
        portToService.put(422, "ariel3/tcp");

        serviceToPort.put("ariel3/udp", 422);
        portToService.put(422, "ariel3/udp");

        serviceToPort.put("opc-job-start/tcp", 423);
        portToService.put(423, "opc-job-start/tcp");

        serviceToPort.put("opc-job-start/udp", 423);
        portToService.put(423, "opc-job-start/udp");

        serviceToPort.put("opc-job-track/tcp", 424);
        portToService.put(424, "opc-job-track/tcp");

        serviceToPort.put("opc-job-track/udp", 424);
        portToService.put(424, "opc-job-track/udp");

        serviceToPort.put("icad-el/tcp", 425);
        portToService.put(425, "icad-el/tcp");

        serviceToPort.put("icad-el/udp", 425);
        portToService.put(425, "icad-el/udp");

        serviceToPort.put("smartsdp/tcp", 426);
        portToService.put(426, "smartsdp/tcp");

        serviceToPort.put("smartsdp/udp", 426);
        portToService.put(426, "smartsdp/udp");

        serviceToPort.put("svrloc/tcp", 427);
        portToService.put(427, "svrloc/tcp");

        serviceToPort.put("svrloc/udp", 427);
        portToService.put(427, "svrloc/udp");

        serviceToPort.put("ocs_cmu/tcp", 428);
        portToService.put(428, "ocs_cmu/tcp");

        serviceToPort.put("ocs_cmu/udp", 428);
        portToService.put(428, "ocs_cmu/udp");

        serviceToPort.put("ocs_amu/tcp", 429);
        portToService.put(429, "ocs_amu/tcp");

        serviceToPort.put("ocs_amu/udp", 429);
        portToService.put(429, "ocs_amu/udp");

        serviceToPort.put("utmpsd/tcp", 430);
        portToService.put(430, "utmpsd/tcp");

        serviceToPort.put("utmpsd/udp", 430);
        portToService.put(430, "utmpsd/udp");

        serviceToPort.put("utmpcd/tcp", 431);
        portToService.put(431, "utmpcd/tcp");

        serviceToPort.put("utmpcd/udp", 431);
        portToService.put(431, "utmpcd/udp");

        serviceToPort.put("iasd/tcp", 432);
        portToService.put(432, "iasd/tcp");

        serviceToPort.put("iasd/udp", 432);
        portToService.put(432, "iasd/udp");

        serviceToPort.put("nnsp/tcp", 433);
        portToService.put(433, "nnsp/tcp");

        serviceToPort.put("nnsp/udp", 433);
        portToService.put(433, "nnsp/udp");

        serviceToPort.put("mobileip-agent/tcp", 434);
        portToService.put(434, "mobileip-agent/tcp");

        serviceToPort.put("mobileip-agent/udp", 434);
        portToService.put(434, "mobileip-agent/udp");

        serviceToPort.put("mobilip-mn/tcp", 435);
        portToService.put(435, "mobilip-mn/tcp");

        serviceToPort.put("mobilip-mn/udp", 435);
        portToService.put(435, "mobilip-mn/udp");

        serviceToPort.put("dna-cml/tcp", 436);
        portToService.put(436, "dna-cml/tcp");

        serviceToPort.put("dna-cml/udp", 436);
        portToService.put(436, "dna-cml/udp");

        serviceToPort.put("comscm/tcp", 437);
        portToService.put(437, "comscm/tcp");

        serviceToPort.put("comscm/udp", 437);
        portToService.put(437, "comscm/udp");

        serviceToPort.put("dsfgw/tcp", 438);
        portToService.put(438, "dsfgw/tcp");

        serviceToPort.put("dsfgw/udp", 438);
        portToService.put(438, "dsfgw/udp");

        serviceToPort.put("dasp/tcp", 439);
        portToService.put(439, "dasp/tcp");

        serviceToPort.put("dasp/udp", 439);
        portToService.put(439, "dasp/udp");

        serviceToPort.put("sgcp/tcp", 440);
        portToService.put(440, "sgcp/tcp");

        serviceToPort.put("sgcp/udp", 440);
        portToService.put(440, "sgcp/udp");

        serviceToPort.put("decvms-sysmgt/tcp", 441);
        portToService.put(441, "decvms-sysmgt/tcp");

        serviceToPort.put("decvms-sysmgt/udp", 441);
        portToService.put(441, "decvms-sysmgt/udp");

        serviceToPort.put("cvc_hostd/tcp", 442);
        portToService.put(442, "cvc_hostd/tcp");

        serviceToPort.put("cvc_hostd/udp", 442);
        portToService.put(442, "cvc_hostd/udp");

        serviceToPort.put("https/tcp", 443);
        portToService.put(443, "https/tcp");

        serviceToPort.put("https/udp", 443);
        portToService.put(443, "https/udp");

        serviceToPort.put("https/sctp", 443);
        portToService.put(443, "https/sctp");

        serviceToPort.put("snpp/tcp", 444);
        portToService.put(444, "snpp/tcp");

        serviceToPort.put("snpp/udp", 444);
        portToService.put(444, "snpp/udp");

        serviceToPort.put("microsoft-ds/tcp", 445);
        portToService.put(445, "microsoft-ds/tcp");

        serviceToPort.put("microsoft-ds/udp", 445);
        portToService.put(445, "microsoft-ds/udp");

        serviceToPort.put("ddm-rdb/tcp", 446);
        portToService.put(446, "ddm-rdb/tcp");

        serviceToPort.put("ddm-rdb/udp", 446);
        portToService.put(446, "ddm-rdb/udp");

        serviceToPort.put("ddm-dfm/tcp", 447);
        portToService.put(447, "ddm-dfm/tcp");

        serviceToPort.put("ddm-dfm/udp", 447);
        portToService.put(447, "ddm-dfm/udp");

        serviceToPort.put("ddm-ssl/tcp", 448);
        portToService.put(448, "ddm-ssl/tcp");

        serviceToPort.put("ddm-ssl/udp", 448);
        portToService.put(448, "ddm-ssl/udp");

        serviceToPort.put("as-servermap/tcp", 449);
        portToService.put(449, "as-servermap/tcp");

        serviceToPort.put("as-servermap/udp", 449);
        portToService.put(449, "as-servermap/udp");

        serviceToPort.put("tserver/tcp", 450);
        portToService.put(450, "tserver/tcp");

        serviceToPort.put("tserver/udp", 450);
        portToService.put(450, "tserver/udp");

        serviceToPort.put("sfs-smp-net/tcp", 451);
        portToService.put(451, "sfs-smp-net/tcp");

        serviceToPort.put("sfs-smp-net/udp", 451);
        portToService.put(451, "sfs-smp-net/udp");

        serviceToPort.put("sfs-config/tcp", 452);
        portToService.put(452, "sfs-config/tcp");

        serviceToPort.put("sfs-config/udp", 452);
        portToService.put(452, "sfs-config/udp");

        serviceToPort.put("creativeserver/tcp", 453);
        portToService.put(453, "creativeserver/tcp");

        serviceToPort.put("creativeserver/udp", 453);
        portToService.put(453, "creativeserver/udp");

        serviceToPort.put("contentserver/tcp", 454);
        portToService.put(454, "contentserver/tcp");

        serviceToPort.put("contentserver/udp", 454);
        portToService.put(454, "contentserver/udp");

        serviceToPort.put("creativepartnr/tcp", 455);
        portToService.put(455, "creativepartnr/tcp");

        serviceToPort.put("creativepartnr/udp", 455);
        portToService.put(455, "creativepartnr/udp");

        serviceToPort.put("macon-tcp/tcp", 456);
        portToService.put(456, "macon-tcp/tcp");

        serviceToPort.put("macon-udp/udp", 456);
        portToService.put(456, "macon-udp/udp");

        serviceToPort.put("scohelp/tcp", 457);
        portToService.put(457, "scohelp/tcp");

        serviceToPort.put("scohelp/udp", 457);
        portToService.put(457, "scohelp/udp");

        serviceToPort.put("appleqtc/tcp", 458);
        portToService.put(458, "appleqtc/tcp");

        serviceToPort.put("appleqtc/udp", 458);
        portToService.put(458, "appleqtc/udp");

        serviceToPort.put("ampr-rcmd/tcp", 459);
        portToService.put(459, "ampr-rcmd/tcp");

        serviceToPort.put("ampr-rcmd/udp", 459);
        portToService.put(459, "ampr-rcmd/udp");

        serviceToPort.put("skronk/tcp", 460);
        portToService.put(460, "skronk/tcp");

        serviceToPort.put("skronk/udp", 460);
        portToService.put(460, "skronk/udp");

        serviceToPort.put("datasurfsrv/tcp", 461);
        portToService.put(461, "datasurfsrv/tcp");

        serviceToPort.put("datasurfsrv/udp", 461);
        portToService.put(461, "datasurfsrv/udp");

        serviceToPort.put("datasurfsrvsec/tcp", 462);
        portToService.put(462, "datasurfsrvsec/tcp");

        serviceToPort.put("datasurfsrvsec/udp", 462);
        portToService.put(462, "datasurfsrvsec/udp");

        serviceToPort.put("alpes/tcp", 463);
        portToService.put(463, "alpes/tcp");

        serviceToPort.put("alpes/udp", 463);
        portToService.put(463, "alpes/udp");

        serviceToPort.put("kpasswd/tcp", 464);
        portToService.put(464, "kpasswd/tcp");

        serviceToPort.put("kpasswd/udp", 464);
        portToService.put(464, "kpasswd/udp");

        serviceToPort.put("urd/tcp", 465);
        portToService.put(465, "urd/tcp");

        serviceToPort.put("igmpv3lite/udp", 465);
        portToService.put(465, "igmpv3lite/udp");

        serviceToPort.put("digital-vrc/tcp", 466);
        portToService.put(466, "digital-vrc/tcp");

        serviceToPort.put("digital-vrc/udp", 466);
        portToService.put(466, "digital-vrc/udp");

        serviceToPort.put("mylex-mapd/tcp", 467);
        portToService.put(467, "mylex-mapd/tcp");

        serviceToPort.put("mylex-mapd/udp", 467);
        portToService.put(467, "mylex-mapd/udp");

        serviceToPort.put("photuris/tcp", 468);
        portToService.put(468, "photuris/tcp");

        serviceToPort.put("photuris/udp", 468);
        portToService.put(468, "photuris/udp");

        serviceToPort.put("rcp/tcp", 469);
        portToService.put(469, "rcp/tcp");

        serviceToPort.put("rcp/udp", 469);
        portToService.put(469, "rcp/udp");

        serviceToPort.put("scx-proxy/tcp", 470);
        portToService.put(470, "scx-proxy/tcp");

        serviceToPort.put("scx-proxy/udp", 470);
        portToService.put(470, "scx-proxy/udp");

        serviceToPort.put("mondex/tcp", 471);
        portToService.put(471, "mondex/tcp");

        serviceToPort.put("mondex/udp", 471);
        portToService.put(471, "mondex/udp");

        serviceToPort.put("ljk-login/tcp", 472);
        portToService.put(472, "ljk-login/tcp");

        serviceToPort.put("ljk-login/udp", 472);
        portToService.put(472, "ljk-login/udp");

        serviceToPort.put("hybrid-pop/tcp", 473);
        portToService.put(473, "hybrid-pop/tcp");

        serviceToPort.put("hybrid-pop/udp", 473);
        portToService.put(473, "hybrid-pop/udp");

        serviceToPort.put("tn-tl-w1/tcp", 474);
        portToService.put(474, "tn-tl-w1/tcp");

        serviceToPort.put("tn-tl-w2/udp", 474);
        portToService.put(474, "tn-tl-w2/udp");

        serviceToPort.put("tcpnethaspsrv/tcp", 475);
        portToService.put(475, "tcpnethaspsrv/tcp");

        serviceToPort.put("tcpnethaspsrv/udp", 475);
        portToService.put(475, "tcpnethaspsrv/udp");

        serviceToPort.put("tn-tl-fd1/tcp", 476);
        portToService.put(476, "tn-tl-fd1/tcp");

        serviceToPort.put("tn-tl-fd1/udp", 476);
        portToService.put(476, "tn-tl-fd1/udp");

        serviceToPort.put("ss7ns/tcp", 477);
        portToService.put(477, "ss7ns/tcp");

        serviceToPort.put("ss7ns/udp", 477);
        portToService.put(477, "ss7ns/udp");

        serviceToPort.put("spsc/tcp", 478);
        portToService.put(478, "spsc/tcp");

        serviceToPort.put("spsc/udp", 478);
        portToService.put(478, "spsc/udp");

        serviceToPort.put("iafserver/tcp", 479);
        portToService.put(479, "iafserver/tcp");

        serviceToPort.put("iafserver/udp", 479);
        portToService.put(479, "iafserver/udp");

        serviceToPort.put("iafdbase/tcp", 480);
        portToService.put(480, "iafdbase/tcp");

        serviceToPort.put("iafdbase/udp", 480);
        portToService.put(480, "iafdbase/udp");

        serviceToPort.put("ph/tcp", 481);
        portToService.put(481, "ph/tcp");

        serviceToPort.put("ph/udp", 481);
        portToService.put(481, "ph/udp");

        serviceToPort.put("bgs-nsi/tcp", 482);
        portToService.put(482, "bgs-nsi/tcp");

        serviceToPort.put("bgs-nsi/udp", 482);
        portToService.put(482, "bgs-nsi/udp");

        serviceToPort.put("ulpnet/tcp", 483);
        portToService.put(483, "ulpnet/tcp");

        serviceToPort.put("ulpnet/udp", 483);
        portToService.put(483, "ulpnet/udp");

        serviceToPort.put("integra-sme/tcp", 484);
        portToService.put(484, "integra-sme/tcp");

        serviceToPort.put("integra-sme/udp", 484);
        portToService.put(484, "integra-sme/udp");

        serviceToPort.put("powerburst/tcp", 485);
        portToService.put(485, "powerburst/tcp");

        serviceToPort.put("powerburst/udp", 485);
        portToService.put(485, "powerburst/udp");

        serviceToPort.put("avian/tcp", 486);
        portToService.put(486, "avian/tcp");

        serviceToPort.put("avian/udp", 486);
        portToService.put(486, "avian/udp");

        serviceToPort.put("saft/tcp", 487);
        portToService.put(487, "saft/tcp");

        serviceToPort.put("saft/udp", 487);
        portToService.put(487, "saft/udp");

        serviceToPort.put("gss-http/tcp", 488);
        portToService.put(488, "gss-http/tcp");

        serviceToPort.put("gss-http/udp", 488);
        portToService.put(488, "gss-http/udp");

        serviceToPort.put("nest-protocol/tcp", 489);
        portToService.put(489, "nest-protocol/tcp");

        serviceToPort.put("nest-protocol/udp", 489);
        portToService.put(489, "nest-protocol/udp");

        serviceToPort.put("micom-pfs/tcp", 490);
        portToService.put(490, "micom-pfs/tcp");

        serviceToPort.put("micom-pfs/udp", 490);
        portToService.put(490, "micom-pfs/udp");

        serviceToPort.put("go-login/tcp", 491);
        portToService.put(491, "go-login/tcp");

        serviceToPort.put("go-login/udp", 491);
        portToService.put(491, "go-login/udp");

        serviceToPort.put("ticf-1/tcp", 492);
        portToService.put(492, "ticf-1/tcp");

        serviceToPort.put("ticf-1/udp", 492);
        portToService.put(492, "ticf-1/udp");

        serviceToPort.put("ticf-2/tcp", 493);
        portToService.put(493, "ticf-2/tcp");

        serviceToPort.put("ticf-2/udp", 493);
        portToService.put(493, "ticf-2/udp");

        serviceToPort.put("pov-ray/tcp", 494);
        portToService.put(494, "pov-ray/tcp");

        serviceToPort.put("pov-ray/udp", 494);
        portToService.put(494, "pov-ray/udp");

        serviceToPort.put("intecourier/tcp", 495);
        portToService.put(495, "intecourier/tcp");

        serviceToPort.put("intecourier/udp", 495);
        portToService.put(495, "intecourier/udp");

        serviceToPort.put("pim-rp-disc/tcp", 496);
        portToService.put(496, "pim-rp-disc/tcp");

        serviceToPort.put("pim-rp-disc/udp", 496);
        portToService.put(496, "pim-rp-disc/udp");

        serviceToPort.put("dantz/tcp", 497);
        portToService.put(497, "dantz/tcp");

        serviceToPort.put("dantz/udp", 497);
        portToService.put(497, "dantz/udp");

        serviceToPort.put("siam/tcp", 498);
        portToService.put(498, "siam/tcp");

        serviceToPort.put("siam/udp", 498);
        portToService.put(498, "siam/udp");

        serviceToPort.put("iso-ill/tcp", 499);
        portToService.put(499, "iso-ill/tcp");

        serviceToPort.put("iso-ill/udp", 499);
        portToService.put(499, "iso-ill/udp");

        serviceToPort.put("isakmp/tcp", 500);
        portToService.put(500, "isakmp/tcp");

        serviceToPort.put("isakmp/udp", 500);
        portToService.put(500, "isakmp/udp");

        serviceToPort.put("stmf/tcp", 501);
        portToService.put(501, "stmf/tcp");

        serviceToPort.put("stmf/udp", 501);
        portToService.put(501, "stmf/udp");

        serviceToPort.put("asa-appl-proto/tcp", 502);
        portToService.put(502, "asa-appl-proto/tcp");

        serviceToPort.put("asa-appl-proto/udp", 502);
        portToService.put(502, "asa-appl-proto/udp");

        serviceToPort.put("intrinsa/tcp", 503);
        portToService.put(503, "intrinsa/tcp");

        serviceToPort.put("intrinsa/udp", 503);
        portToService.put(503, "intrinsa/udp");

        serviceToPort.put("citadel/tcp", 504);
        portToService.put(504, "citadel/tcp");

        serviceToPort.put("citadel/udp", 504);
        portToService.put(504, "citadel/udp");

        serviceToPort.put("mailbox-lm/tcp", 505);
        portToService.put(505, "mailbox-lm/tcp");

        serviceToPort.put("mailbox-lm/udp", 505);
        portToService.put(505, "mailbox-lm/udp");

        serviceToPort.put("ohimsrv/tcp", 506);
        portToService.put(506, "ohimsrv/tcp");

        serviceToPort.put("ohimsrv/udp", 506);
        portToService.put(506, "ohimsrv/udp");

        serviceToPort.put("crs/tcp", 507);
        portToService.put(507, "crs/tcp");

        serviceToPort.put("crs/udp", 507);
        portToService.put(507, "crs/udp");

        serviceToPort.put("xvttp/tcp", 508);
        portToService.put(508, "xvttp/tcp");

        serviceToPort.put("xvttp/udp", 508);
        portToService.put(508, "xvttp/udp");

        serviceToPort.put("snare/tcp", 509);
        portToService.put(509, "snare/tcp");

        serviceToPort.put("snare/udp", 509);
        portToService.put(509, "snare/udp");

        serviceToPort.put("fcp/tcp", 510);
        portToService.put(510, "fcp/tcp");

        serviceToPort.put("fcp/udp", 510);
        portToService.put(510, "fcp/udp");

        serviceToPort.put("passgo/tcp", 511);
        portToService.put(511, "passgo/tcp");

        serviceToPort.put("passgo/udp", 511);
        portToService.put(511, "passgo/udp");

        serviceToPort.put("exec/tcp", 512);
        portToService.put(512, "exec/tcp");

        serviceToPort.put("comsat/udp", 512);
        portToService.put(512, "comsat/udp");

        serviceToPort.put("biff/udp", 512);
        portToService.put(512, "biff/udp");

        serviceToPort.put("login/tcp", 513);
        portToService.put(513, "login/tcp");

        serviceToPort.put("who/udp", 513);
        portToService.put(513, "who/udp");

        serviceToPort.put("shell/tcp", 514);
        portToService.put(514, "shell/tcp");

        serviceToPort.put("syslog/udp", 514);
        portToService.put(514, "syslog/udp");

        serviceToPort.put("printer/tcp", 515);
        portToService.put(515, "printer/tcp");

        serviceToPort.put("printer/udp", 515);
        portToService.put(515, "printer/udp");

        serviceToPort.put("videotex/tcp", 516);
        portToService.put(516, "videotex/tcp");

        serviceToPort.put("videotex/udp", 516);
        portToService.put(516, "videotex/udp");

        serviceToPort.put("talk/tcp", 517);
        portToService.put(517, "talk/tcp");

        serviceToPort.put("talk/udp", 517);
        portToService.put(517, "talk/udp");

        serviceToPort.put("ntalk/tcp", 518);
        portToService.put(518, "ntalk/tcp");

        serviceToPort.put("ntalk/udp", 518);
        portToService.put(518, "ntalk/udp");

        serviceToPort.put("utime/tcp", 519);
        portToService.put(519, "utime/tcp");

        serviceToPort.put("utime/udp", 519);
        portToService.put(519, "utime/udp");

        serviceToPort.put("efs/tcp", 520);
        portToService.put(520, "efs/tcp");

        serviceToPort.put("router/udp", 520);
        portToService.put(520, "router/udp");

        serviceToPort.put("ripng/tcp", 521);
        portToService.put(521, "ripng/tcp");

        serviceToPort.put("ripng/udp", 521);
        portToService.put(521, "ripng/udp");

        serviceToPort.put("ulp/tcp", 522);
        portToService.put(522, "ulp/tcp");

        serviceToPort.put("ulp/udp", 522);
        portToService.put(522, "ulp/udp");

        serviceToPort.put("ibm-db2/tcp", 523);
        portToService.put(523, "ibm-db2/tcp");

        serviceToPort.put("ibm-db2/udp", 523);
        portToService.put(523, "ibm-db2/udp");

        serviceToPort.put("ncp/tcp", 524);
        portToService.put(524, "ncp/tcp");

        serviceToPort.put("ncp/udp", 524);
        portToService.put(524, "ncp/udp");

        serviceToPort.put("timed/tcp", 525);
        portToService.put(525, "timed/tcp");

        serviceToPort.put("timed/udp", 525);
        portToService.put(525, "timed/udp");

        serviceToPort.put("tempo/tcp", 526);
        portToService.put(526, "tempo/tcp");

        serviceToPort.put("tempo/udp", 526);
        portToService.put(526, "tempo/udp");

        serviceToPort.put("stx/tcp", 527);
        portToService.put(527, "stx/tcp");

        serviceToPort.put("stx/udp", 527);
        portToService.put(527, "stx/udp");

        serviceToPort.put("custix/tcp", 528);
        portToService.put(528, "custix/tcp");

        serviceToPort.put("custix/udp", 528);
        portToService.put(528, "custix/udp");

        serviceToPort.put("irc-serv/tcp", 529);
        portToService.put(529, "irc-serv/tcp");

        serviceToPort.put("irc-serv/udp", 529);
        portToService.put(529, "irc-serv/udp");

        serviceToPort.put("courier/tcp", 530);
        portToService.put(530, "courier/tcp");

        serviceToPort.put("courier/udp", 530);
        portToService.put(530, "courier/udp");

        serviceToPort.put("conference/tcp", 531);
        portToService.put(531, "conference/tcp");

        serviceToPort.put("conference/udp", 531);
        portToService.put(531, "conference/udp");

        serviceToPort.put("netnews/tcp", 532);
        portToService.put(532, "netnews/tcp");

        serviceToPort.put("netnews/udp", 532);
        portToService.put(532, "netnews/udp");

        serviceToPort.put("netwall/tcp", 533);
        portToService.put(533, "netwall/tcp");

        serviceToPort.put("netwall/udp", 533);
        portToService.put(533, "netwall/udp");

        serviceToPort.put("windream/tcp", 534);
        portToService.put(534, "windream/tcp");

        serviceToPort.put("windream/udp", 534);
        portToService.put(534, "windream/udp");

        serviceToPort.put("iiop/tcp", 535);
        portToService.put(535, "iiop/tcp");

        serviceToPort.put("iiop/udp", 535);
        portToService.put(535, "iiop/udp");

        serviceToPort.put("opalis-rdv/tcp", 536);
        portToService.put(536, "opalis-rdv/tcp");

        serviceToPort.put("opalis-rdv/udp", 536);
        portToService.put(536, "opalis-rdv/udp");

        serviceToPort.put("nmsp/tcp", 537);
        portToService.put(537, "nmsp/tcp");

        serviceToPort.put("nmsp/udp", 537);
        portToService.put(537, "nmsp/udp");

        serviceToPort.put("gdomap/tcp", 538);
        portToService.put(538, "gdomap/tcp");

        serviceToPort.put("gdomap/udp", 538);
        portToService.put(538, "gdomap/udp");

        serviceToPort.put("apertus-ldp/tcp", 539);
        portToService.put(539, "apertus-ldp/tcp");

        serviceToPort.put("apertus-ldp/udp", 539);
        portToService.put(539, "apertus-ldp/udp");

        serviceToPort.put("uucp/tcp", 540);
        portToService.put(540, "uucp/tcp");

        serviceToPort.put("uucp/udp", 540);
        portToService.put(540, "uucp/udp");

        serviceToPort.put("uucp-rlogin/tcp", 541);
        portToService.put(541, "uucp-rlogin/tcp");

        serviceToPort.put("uucp-rlogin/udp", 541);
        portToService.put(541, "uucp-rlogin/udp");

        serviceToPort.put("commerce/tcp", 542);
        portToService.put(542, "commerce/tcp");

        serviceToPort.put("commerce/udp", 542);
        portToService.put(542, "commerce/udp");

        serviceToPort.put("klogin/tcp", 543);
        portToService.put(543, "klogin/tcp");

        serviceToPort.put("klogin/udp", 543);
        portToService.put(543, "klogin/udp");

        serviceToPort.put("kshell/tcp", 544);
        portToService.put(544, "kshell/tcp");

        serviceToPort.put("kshell/udp", 544);
        portToService.put(544, "kshell/udp");

        serviceToPort.put("appleqtcsrvr/tcp", 545);
        portToService.put(545, "appleqtcsrvr/tcp");

        serviceToPort.put("appleqtcsrvr/udp", 545);
        portToService.put(545, "appleqtcsrvr/udp");

        serviceToPort.put("dhcpv6-client/tcp", 546);
        portToService.put(546, "dhcpv6-client/tcp");

        serviceToPort.put("dhcpv6-client/udp", 546);
        portToService.put(546, "dhcpv6-client/udp");

        serviceToPort.put("dhcpv6-server/tcp", 547);
        portToService.put(547, "dhcpv6-server/tcp");

        serviceToPort.put("dhcpv6-server/udp", 547);
        portToService.put(547, "dhcpv6-server/udp");

        serviceToPort.put("afpovertcp/tcp", 548);
        portToService.put(548, "afpovertcp/tcp");

        serviceToPort.put("afpovertcp/udp", 548);
        portToService.put(548, "afpovertcp/udp");

        serviceToPort.put("idfp/tcp", 549);
        portToService.put(549, "idfp/tcp");

        serviceToPort.put("idfp/udp", 549);
        portToService.put(549, "idfp/udp");

        serviceToPort.put("new-rwho/tcp", 550);
        portToService.put(550, "new-rwho/tcp");

        serviceToPort.put("new-rwho/udp", 550);
        portToService.put(550, "new-rwho/udp");

        serviceToPort.put("cybercash/tcp", 551);
        portToService.put(551, "cybercash/tcp");

        serviceToPort.put("cybercash/udp", 551);
        portToService.put(551, "cybercash/udp");

        serviceToPort.put("devshr-nts/tcp", 552);
        portToService.put(552, "devshr-nts/tcp");

        serviceToPort.put("devshr-nts/udp", 552);
        portToService.put(552, "devshr-nts/udp");

        serviceToPort.put("pirp/tcp", 553);
        portToService.put(553, "pirp/tcp");

        serviceToPort.put("pirp/udp", 553);
        portToService.put(553, "pirp/udp");

        serviceToPort.put("rtsp/tcp", 554);
        portToService.put(554, "rtsp/tcp");

        serviceToPort.put("rtsp/udp", 554);
        portToService.put(554, "rtsp/udp");

        serviceToPort.put("dsf/tcp", 555);
        portToService.put(555, "dsf/tcp");

        serviceToPort.put("dsf/udp", 555);
        portToService.put(555, "dsf/udp");

        serviceToPort.put("remotefs/tcp", 556);
        portToService.put(556, "remotefs/tcp");

        serviceToPort.put("remotefs/udp", 556);
        portToService.put(556, "remotefs/udp");

        serviceToPort.put("openvms-sysipc/tcp", 557);
        portToService.put(557, "openvms-sysipc/tcp");

        serviceToPort.put("openvms-sysipc/udp", 557);
        portToService.put(557, "openvms-sysipc/udp");

        serviceToPort.put("sdnskmp/tcp", 558);
        portToService.put(558, "sdnskmp/tcp");

        serviceToPort.put("sdnskmp/udp", 558);
        portToService.put(558, "sdnskmp/udp");

        serviceToPort.put("teedtap/tcp", 559);
        portToService.put(559, "teedtap/tcp");

        serviceToPort.put("teedtap/udp", 559);
        portToService.put(559, "teedtap/udp");

        serviceToPort.put("rmonitor/tcp", 560);
        portToService.put(560, "rmonitor/tcp");

        serviceToPort.put("rmonitor/udp", 560);
        portToService.put(560, "rmonitor/udp");

        serviceToPort.put("monitor/tcp", 561);
        portToService.put(561, "monitor/tcp");

        serviceToPort.put("monitor/udp", 561);
        portToService.put(561, "monitor/udp");

        serviceToPort.put("chshell/tcp", 562);
        portToService.put(562, "chshell/tcp");

        serviceToPort.put("chshell/udp", 562);
        portToService.put(562, "chshell/udp");

        serviceToPort.put("nntps/tcp", 563);
        portToService.put(563, "nntps/tcp");

        serviceToPort.put("nntps/udp", 563);
        portToService.put(563, "nntps/udp");

        serviceToPort.put("9pfs/tcp", 564);
        portToService.put(564, "9pfs/tcp");

        serviceToPort.put("9pfs/udp", 564);
        portToService.put(564, "9pfs/udp");

        serviceToPort.put("whoami/tcp", 565);
        portToService.put(565, "whoami/tcp");

        serviceToPort.put("whoami/udp", 565);
        portToService.put(565, "whoami/udp");

        serviceToPort.put("streettalk/tcp", 566);
        portToService.put(566, "streettalk/tcp");

        serviceToPort.put("streettalk/udp", 566);
        portToService.put(566, "streettalk/udp");

        serviceToPort.put("banyan-rpc/tcp", 567);
        portToService.put(567, "banyan-rpc/tcp");

        serviceToPort.put("banyan-rpc/udp", 567);
        portToService.put(567, "banyan-rpc/udp");

        serviceToPort.put("ms-shuttle/tcp", 568);
        portToService.put(568, "ms-shuttle/tcp");

        serviceToPort.put("ms-shuttle/udp", 568);
        portToService.put(568, "ms-shuttle/udp");

        serviceToPort.put("ms-rome/tcp", 569);
        portToService.put(569, "ms-rome/tcp");

        serviceToPort.put("ms-rome/udp", 569);
        portToService.put(569, "ms-rome/udp");

        serviceToPort.put("meter/tcp", 570);
        portToService.put(570, "meter/tcp");

        serviceToPort.put("meter/udp", 570);
        portToService.put(570, "meter/udp");

        serviceToPort.put("meter/tcp", 571);
        portToService.put(571, "meter/tcp");

        serviceToPort.put("meter/udp", 571);
        portToService.put(571, "meter/udp");

        serviceToPort.put("sonar/tcp", 572);
        portToService.put(572, "sonar/tcp");

        serviceToPort.put("sonar/udp", 572);
        portToService.put(572, "sonar/udp");

        serviceToPort.put("banyan-vip/tcp", 573);
        portToService.put(573, "banyan-vip/tcp");

        serviceToPort.put("banyan-vip/udp", 573);
        portToService.put(573, "banyan-vip/udp");

        serviceToPort.put("ftp-agent/tcp", 574);
        portToService.put(574, "ftp-agent/tcp");

        serviceToPort.put("ftp-agent/udp", 574);
        portToService.put(574, "ftp-agent/udp");

        serviceToPort.put("vemmi/tcp", 575);
        portToService.put(575, "vemmi/tcp");

        serviceToPort.put("vemmi/udp", 575);
        portToService.put(575, "vemmi/udp");

        serviceToPort.put("ipcd/tcp", 576);
        portToService.put(576, "ipcd/tcp");

        serviceToPort.put("ipcd/udp", 576);
        portToService.put(576, "ipcd/udp");

        serviceToPort.put("vnas/tcp", 577);
        portToService.put(577, "vnas/tcp");

        serviceToPort.put("vnas/udp", 577);
        portToService.put(577, "vnas/udp");

        serviceToPort.put("ipdd/tcp", 578);
        portToService.put(578, "ipdd/tcp");

        serviceToPort.put("ipdd/udp", 578);
        portToService.put(578, "ipdd/udp");

        serviceToPort.put("decbsrv/tcp", 579);
        portToService.put(579, "decbsrv/tcp");

        serviceToPort.put("decbsrv/udp", 579);
        portToService.put(579, "decbsrv/udp");

        serviceToPort.put("sntp-heartbeat/tcp", 580);
        portToService.put(580, "sntp-heartbeat/tcp");

        serviceToPort.put("sntp-heartbeat/udp", 580);
        portToService.put(580, "sntp-heartbeat/udp");

        serviceToPort.put("bdp/tcp", 581);
        portToService.put(581, "bdp/tcp");

        serviceToPort.put("bdp/udp", 581);
        portToService.put(581, "bdp/udp");

        serviceToPort.put("scc-security/tcp", 582);
        portToService.put(582, "scc-security/tcp");

        serviceToPort.put("scc-security/udp", 582);
        portToService.put(582, "scc-security/udp");

        serviceToPort.put("philips-vc/tcp", 583);
        portToService.put(583, "philips-vc/tcp");

        serviceToPort.put("philips-vc/udp", 583);
        portToService.put(583, "philips-vc/udp");

        serviceToPort.put("keyserver/tcp", 584);
        portToService.put(584, "keyserver/tcp");

        serviceToPort.put("keyserver/udp", 584);
        portToService.put(584, "keyserver/udp");

        serviceToPort.put("password-chg/tcp", 586);
        portToService.put(586, "password-chg/tcp");

        serviceToPort.put("password-chg/udp", 586);
        portToService.put(586, "password-chg/udp");

        serviceToPort.put("submission/tcp", 587);
        portToService.put(587, "submission/tcp");

        serviceToPort.put("submission/udp", 587);
        portToService.put(587, "submission/udp");

        serviceToPort.put("cal/tcp", 588);
        portToService.put(588, "cal/tcp");

        serviceToPort.put("cal/udp", 588);
        portToService.put(588, "cal/udp");

        serviceToPort.put("eyelink/tcp", 589);
        portToService.put(589, "eyelink/tcp");

        serviceToPort.put("eyelink/udp", 589);
        portToService.put(589, "eyelink/udp");

        serviceToPort.put("tns-cml/tcp", 590);
        portToService.put(590, "tns-cml/tcp");

        serviceToPort.put("tns-cml/udp", 590);
        portToService.put(590, "tns-cml/udp");

        serviceToPort.put("http-alt/tcp", 591);
        portToService.put(591, "http-alt/tcp");

        serviceToPort.put("http-alt/udp", 591);
        portToService.put(591, "http-alt/udp");

        serviceToPort.put("eudora-set/tcp", 592);
        portToService.put(592, "eudora-set/tcp");

        serviceToPort.put("eudora-set/udp", 592);
        portToService.put(592, "eudora-set/udp");

        serviceToPort.put("http-rpc-epmap/tcp", 593);
        portToService.put(593, "http-rpc-epmap/tcp");

        serviceToPort.put("http-rpc-epmap/udp", 593);
        portToService.put(593, "http-rpc-epmap/udp");

        serviceToPort.put("tpip/tcp", 594);
        portToService.put(594, "tpip/tcp");

        serviceToPort.put("tpip/udp", 594);
        portToService.put(594, "tpip/udp");

        serviceToPort.put("cab-protocol/tcp", 595);
        portToService.put(595, "cab-protocol/tcp");

        serviceToPort.put("cab-protocol/udp", 595);
        portToService.put(595, "cab-protocol/udp");

        serviceToPort.put("smsd/tcp", 596);
        portToService.put(596, "smsd/tcp");

        serviceToPort.put("smsd/udp", 596);
        portToService.put(596, "smsd/udp");

        serviceToPort.put("ptcnameservice/tcp", 597);
        portToService.put(597, "ptcnameservice/tcp");

        serviceToPort.put("ptcnameservice/udp", 597);
        portToService.put(597, "ptcnameservice/udp");

        serviceToPort.put("sco-websrvrmg3/tcp", 598);
        portToService.put(598, "sco-websrvrmg3/tcp");

        serviceToPort.put("sco-websrvrmg3/udp", 598);
        portToService.put(598, "sco-websrvrmg3/udp");

        serviceToPort.put("acp/tcp", 599);
        portToService.put(599, "acp/tcp");

        serviceToPort.put("acp/udp", 599);
        portToService.put(599, "acp/udp");

        serviceToPort.put("ipcserver/tcp", 600);
        portToService.put(600, "ipcserver/tcp");

        serviceToPort.put("ipcserver/udp", 600);
        portToService.put(600, "ipcserver/udp");

        serviceToPort.put("syslog-conn/tcp", 601);
        portToService.put(601, "syslog-conn/tcp");

        serviceToPort.put("syslog-conn/udp", 601);
        portToService.put(601, "syslog-conn/udp");

        serviceToPort.put("xmlrpc-beep/tcp", 602);
        portToService.put(602, "xmlrpc-beep/tcp");

        serviceToPort.put("xmlrpc-beep/udp", 602);
        portToService.put(602, "xmlrpc-beep/udp");

        serviceToPort.put("idxp/tcp", 603);
        portToService.put(603, "idxp/tcp");

        serviceToPort.put("idxp/udp", 603);
        portToService.put(603, "idxp/udp");

        serviceToPort.put("tunnel/tcp", 604);
        portToService.put(604, "tunnel/tcp");

        serviceToPort.put("tunnel/udp", 604);
        portToService.put(604, "tunnel/udp");

        serviceToPort.put("soap-beep/tcp", 605);
        portToService.put(605, "soap-beep/tcp");

        serviceToPort.put("soap-beep/udp", 605);
        portToService.put(605, "soap-beep/udp");

        serviceToPort.put("urm/tcp", 606);
        portToService.put(606, "urm/tcp");

        serviceToPort.put("urm/udp", 606);
        portToService.put(606, "urm/udp");

        serviceToPort.put("nqs/tcp", 607);
        portToService.put(607, "nqs/tcp");

        serviceToPort.put("nqs/udp", 607);
        portToService.put(607, "nqs/udp");

        serviceToPort.put("sift-uft/tcp", 608);
        portToService.put(608, "sift-uft/tcp");

        serviceToPort.put("sift-uft/udp", 608);
        portToService.put(608, "sift-uft/udp");

        serviceToPort.put("npmp-trap/tcp", 609);
        portToService.put(609, "npmp-trap/tcp");

        serviceToPort.put("npmp-trap/udp", 609);
        portToService.put(609, "npmp-trap/udp");

        serviceToPort.put("npmp-local/tcp", 610);
        portToService.put(610, "npmp-local/tcp");

        serviceToPort.put("npmp-local/udp", 610);
        portToService.put(610, "npmp-local/udp");

        serviceToPort.put("npmp-gui/tcp", 611);
        portToService.put(611, "npmp-gui/tcp");

        serviceToPort.put("npmp-gui/udp", 611);
        portToService.put(611, "npmp-gui/udp");

        serviceToPort.put("hmmp-ind/tcp", 612);
        portToService.put(612, "hmmp-ind/tcp");

        serviceToPort.put("hmmp-ind/udp", 612);
        portToService.put(612, "hmmp-ind/udp");

        serviceToPort.put("hmmp-op/tcp", 613);
        portToService.put(613, "hmmp-op/tcp");

        serviceToPort.put("hmmp-op/udp", 613);
        portToService.put(613, "hmmp-op/udp");

        serviceToPort.put("sshell/tcp", 614);
        portToService.put(614, "sshell/tcp");

        serviceToPort.put("sshell/udp", 614);
        portToService.put(614, "sshell/udp");

        serviceToPort.put("sco-inetmgr/tcp", 615);
        portToService.put(615, "sco-inetmgr/tcp");

        serviceToPort.put("sco-inetmgr/udp", 615);
        portToService.put(615, "sco-inetmgr/udp");

        serviceToPort.put("sco-sysmgr/tcp", 616);
        portToService.put(616, "sco-sysmgr/tcp");

        serviceToPort.put("sco-sysmgr/udp", 616);
        portToService.put(616, "sco-sysmgr/udp");

        serviceToPort.put("sco-dtmgr/tcp", 617);
        portToService.put(617, "sco-dtmgr/tcp");

        serviceToPort.put("sco-dtmgr/udp", 617);
        portToService.put(617, "sco-dtmgr/udp");

        serviceToPort.put("dei-icda/tcp", 618);
        portToService.put(618, "dei-icda/tcp");

        serviceToPort.put("dei-icda/udp", 618);
        portToService.put(618, "dei-icda/udp");

        serviceToPort.put("compaq-evm/tcp", 619);
        portToService.put(619, "compaq-evm/tcp");

        serviceToPort.put("compaq-evm/udp", 619);
        portToService.put(619, "compaq-evm/udp");

        serviceToPort.put("sco-websrvrmgr/tcp", 620);
        portToService.put(620, "sco-websrvrmgr/tcp");

        serviceToPort.put("sco-websrvrmgr/udp", 620);
        portToService.put(620, "sco-websrvrmgr/udp");

        serviceToPort.put("escp-ip/tcp", 621);
        portToService.put(621, "escp-ip/tcp");

        serviceToPort.put("escp-ip/udp", 621);
        portToService.put(621, "escp-ip/udp");

        serviceToPort.put("collaborator/tcp", 622);
        portToService.put(622, "collaborator/tcp");

        serviceToPort.put("collaborator/udp", 622);
        portToService.put(622, "collaborator/udp");

        serviceToPort.put("oob-ws-http/tcp", 623);
        portToService.put(623, "oob-ws-http/tcp");

        serviceToPort.put("asf-rmcp/udp", 623);
        portToService.put(623, "asf-rmcp/udp");

        serviceToPort.put("cryptoadmin/tcp", 624);
        portToService.put(624, "cryptoadmin/tcp");

        serviceToPort.put("cryptoadmin/udp", 624);
        portToService.put(624, "cryptoadmin/udp");

        serviceToPort.put("dec_dlm/tcp", 625);
        portToService.put(625, "dec_dlm/tcp");

        serviceToPort.put("dec_dlm/udp", 625);
        portToService.put(625, "dec_dlm/udp");

        serviceToPort.put("asia/tcp", 626);
        portToService.put(626, "asia/tcp");

        serviceToPort.put("asia/udp", 626);
        portToService.put(626, "asia/udp");

        serviceToPort.put("passgo-tivoli/tcp", 627);
        portToService.put(627, "passgo-tivoli/tcp");

        serviceToPort.put("passgo-tivoli/udp", 627);
        portToService.put(627, "passgo-tivoli/udp");

        serviceToPort.put("qmqp/tcp", 628);
        portToService.put(628, "qmqp/tcp");

        serviceToPort.put("qmqp/udp", 628);
        portToService.put(628, "qmqp/udp");

        serviceToPort.put("3com-amp3/tcp", 629);
        portToService.put(629, "3com-amp3/tcp");

        serviceToPort.put("3com-amp3/udp", 629);
        portToService.put(629, "3com-amp3/udp");

        serviceToPort.put("rda/tcp", 630);
        portToService.put(630, "rda/tcp");

        serviceToPort.put("rda/udp", 630);
        portToService.put(630, "rda/udp");

        serviceToPort.put("ipp/tcp", 631);
        portToService.put(631, "ipp/tcp");

        serviceToPort.put("ipp/udp", 631);
        portToService.put(631, "ipp/udp");

        serviceToPort.put("bmpp/tcp", 632);
        portToService.put(632, "bmpp/tcp");

        serviceToPort.put("bmpp/udp", 632);
        portToService.put(632, "bmpp/udp");

        serviceToPort.put("servstat/tcp", 633);
        portToService.put(633, "servstat/tcp");

        serviceToPort.put("servstat/udp", 633);
        portToService.put(633, "servstat/udp");

        serviceToPort.put("ginad/tcp", 634);
        portToService.put(634, "ginad/tcp");

        serviceToPort.put("ginad/udp", 634);
        portToService.put(634, "ginad/udp");

        serviceToPort.put("rlzdbase/tcp", 635);
        portToService.put(635, "rlzdbase/tcp");

        serviceToPort.put("rlzdbase/udp", 635);
        portToService.put(635, "rlzdbase/udp");

        serviceToPort.put("ldaps/tcp", 636);
        portToService.put(636, "ldaps/tcp");

        serviceToPort.put("ldaps/udp", 636);
        portToService.put(636, "ldaps/udp");

        serviceToPort.put("lanserver/tcp", 637);
        portToService.put(637, "lanserver/tcp");

        serviceToPort.put("lanserver/udp", 637);
        portToService.put(637, "lanserver/udp");

        serviceToPort.put("mcns-sec/tcp", 638);
        portToService.put(638, "mcns-sec/tcp");

        serviceToPort.put("mcns-sec/udp", 638);
        portToService.put(638, "mcns-sec/udp");

        serviceToPort.put("msdp/tcp", 639);
        portToService.put(639, "msdp/tcp");

        serviceToPort.put("msdp/udp", 639);
        portToService.put(639, "msdp/udp");

        serviceToPort.put("entrust-sps/tcp", 640);
        portToService.put(640, "entrust-sps/tcp");

        serviceToPort.put("entrust-sps/udp", 640);
        portToService.put(640, "entrust-sps/udp");

        serviceToPort.put("repcmd/tcp", 641);
        portToService.put(641, "repcmd/tcp");

        serviceToPort.put("repcmd/udp", 641);
        portToService.put(641, "repcmd/udp");

        serviceToPort.put("esro-emsdp/tcp", 642);
        portToService.put(642, "esro-emsdp/tcp");

        serviceToPort.put("esro-emsdp/udp", 642);
        portToService.put(642, "esro-emsdp/udp");

        serviceToPort.put("sanity/tcp", 643);
        portToService.put(643, "sanity/tcp");

        serviceToPort.put("sanity/udp", 643);
        portToService.put(643, "sanity/udp");

        serviceToPort.put("dwr/tcp", 644);
        portToService.put(644, "dwr/tcp");

        serviceToPort.put("dwr/udp", 644);
        portToService.put(644, "dwr/udp");

        serviceToPort.put("pssc/tcp", 645);
        portToService.put(645, "pssc/tcp");

        serviceToPort.put("pssc/udp", 645);
        portToService.put(645, "pssc/udp");

        serviceToPort.put("ldp/tcp", 646);
        portToService.put(646, "ldp/tcp");

        serviceToPort.put("ldp/udp", 646);
        portToService.put(646, "ldp/udp");

        serviceToPort.put("dhcp-failover/tcp", 647);
        portToService.put(647, "dhcp-failover/tcp");

        serviceToPort.put("dhcp-failover/udp", 647);
        portToService.put(647, "dhcp-failover/udp");

        serviceToPort.put("rrp/tcp", 648);
        portToService.put(648, "rrp/tcp");

        serviceToPort.put("rrp/udp", 648);
        portToService.put(648, "rrp/udp");

        serviceToPort.put("cadview-3d/tcp", 649);
        portToService.put(649, "cadview-3d/tcp");

        serviceToPort.put("cadview-3d/udp", 649);
        portToService.put(649, "cadview-3d/udp");

        serviceToPort.put("obex/tcp", 650);
        portToService.put(650, "obex/tcp");

        serviceToPort.put("obex/udp", 650);
        portToService.put(650, "obex/udp");

        serviceToPort.put("ieee-mms/tcp", 651);
        portToService.put(651, "ieee-mms/tcp");

        serviceToPort.put("ieee-mms/udp", 651);
        portToService.put(651, "ieee-mms/udp");

        serviceToPort.put("hello-port/tcp", 652);
        portToService.put(652, "hello-port/tcp");

        serviceToPort.put("hello-port/udp", 652);
        portToService.put(652, "hello-port/udp");

        serviceToPort.put("repscmd/tcp", 653);
        portToService.put(653, "repscmd/tcp");

        serviceToPort.put("repscmd/udp", 653);
        portToService.put(653, "repscmd/udp");

        serviceToPort.put("aodv/tcp", 654);
        portToService.put(654, "aodv/tcp");

        serviceToPort.put("aodv/udp", 654);
        portToService.put(654, "aodv/udp");

        serviceToPort.put("tinc/tcp", 655);
        portToService.put(655, "tinc/tcp");

        serviceToPort.put("tinc/udp", 655);
        portToService.put(655, "tinc/udp");

        serviceToPort.put("spmp/tcp", 656);
        portToService.put(656, "spmp/tcp");

        serviceToPort.put("spmp/udp", 656);
        portToService.put(656, "spmp/udp");

        serviceToPort.put("rmc/tcp", 657);
        portToService.put(657, "rmc/tcp");

        serviceToPort.put("rmc/udp", 657);
        portToService.put(657, "rmc/udp");

        serviceToPort.put("tenfold/tcp", 658);
        portToService.put(658, "tenfold/tcp");

        serviceToPort.put("tenfold/udp", 658);
        portToService.put(658, "tenfold/udp");

        serviceToPort.put("mac-srvr-admin/tcp", 660);
        portToService.put(660, "mac-srvr-admin/tcp");

        serviceToPort.put("mac-srvr-admin/udp", 660);
        portToService.put(660, "mac-srvr-admin/udp");

        serviceToPort.put("hap/tcp", 661);
        portToService.put(661, "hap/tcp");

        serviceToPort.put("hap/udp", 661);
        portToService.put(661, "hap/udp");

        serviceToPort.put("pftp/tcp", 662);
        portToService.put(662, "pftp/tcp");

        serviceToPort.put("pftp/udp", 662);
        portToService.put(662, "pftp/udp");

        serviceToPort.put("purenoise/tcp", 663);
        portToService.put(663, "purenoise/tcp");

        serviceToPort.put("purenoise/udp", 663);
        portToService.put(663, "purenoise/udp");

        serviceToPort.put("oob-ws-https/tcp", 664);
        portToService.put(664, "oob-ws-https/tcp");

        serviceToPort.put("asf-secure-rmcp/udp", 664);
        portToService.put(664, "asf-secure-rmcp/udp");

        serviceToPort.put("sun-dr/tcp", 665);
        portToService.put(665, "sun-dr/tcp");

        serviceToPort.put("sun-dr/udp", 665);
        portToService.put(665, "sun-dr/udp");

        serviceToPort.put("mdqs/tcp", 666);
        portToService.put(666, "mdqs/tcp");

        serviceToPort.put("mdqs/udp", 666);
        portToService.put(666, "mdqs/udp");

        serviceToPort.put("doom/tcp", 666);
        portToService.put(666, "doom/tcp");

        serviceToPort.put("doom/udp", 666);
        portToService.put(666, "doom/udp");

        serviceToPort.put("disclose/tcp", 667);
        portToService.put(667, "disclose/tcp");

        serviceToPort.put("disclose/udp", 667);
        portToService.put(667, "disclose/udp");

        serviceToPort.put("mecomm/tcp", 668);
        portToService.put(668, "mecomm/tcp");

        serviceToPort.put("mecomm/udp", 668);
        portToService.put(668, "mecomm/udp");

        serviceToPort.put("meregister/tcp", 669);
        portToService.put(669, "meregister/tcp");

        serviceToPort.put("meregister/udp", 669);
        portToService.put(669, "meregister/udp");

        serviceToPort.put("vacdsm-sws/tcp", 670);
        portToService.put(670, "vacdsm-sws/tcp");

        serviceToPort.put("vacdsm-sws/udp", 670);
        portToService.put(670, "vacdsm-sws/udp");

        serviceToPort.put("vacdsm-app/tcp", 671);
        portToService.put(671, "vacdsm-app/tcp");

        serviceToPort.put("vacdsm-app/udp", 671);
        portToService.put(671, "vacdsm-app/udp");

        serviceToPort.put("vpps-qua/tcp", 672);
        portToService.put(672, "vpps-qua/tcp");

        serviceToPort.put("vpps-qua/udp", 672);
        portToService.put(672, "vpps-qua/udp");

        serviceToPort.put("cimplex/tcp", 673);
        portToService.put(673, "cimplex/tcp");

        serviceToPort.put("cimplex/udp", 673);
        portToService.put(673, "cimplex/udp");

        serviceToPort.put("acap/tcp", 674);
        portToService.put(674, "acap/tcp");

        serviceToPort.put("acap/udp", 674);
        portToService.put(674, "acap/udp");

        serviceToPort.put("dctp/tcp", 675);
        portToService.put(675, "dctp/tcp");

        serviceToPort.put("dctp/udp", 675);
        portToService.put(675, "dctp/udp");

        serviceToPort.put("vpps-via/tcp", 676);
        portToService.put(676, "vpps-via/tcp");

        serviceToPort.put("vpps-via/udp", 676);
        portToService.put(676, "vpps-via/udp");

        serviceToPort.put("vpp/tcp", 677);
        portToService.put(677, "vpp/tcp");

        serviceToPort.put("vpp/udp", 677);
        portToService.put(677, "vpp/udp");

        serviceToPort.put("ggf-ncp/tcp", 678);
        portToService.put(678, "ggf-ncp/tcp");

        serviceToPort.put("ggf-ncp/udp", 678);
        portToService.put(678, "ggf-ncp/udp");

        serviceToPort.put("mrm/tcp", 679);
        portToService.put(679, "mrm/tcp");

        serviceToPort.put("mrm/udp", 679);
        portToService.put(679, "mrm/udp");

        serviceToPort.put("entrust-aaas/tcp", 680);
        portToService.put(680, "entrust-aaas/tcp");

        serviceToPort.put("entrust-aaas/udp", 680);
        portToService.put(680, "entrust-aaas/udp");

        serviceToPort.put("entrust-aams/tcp", 681);
        portToService.put(681, "entrust-aams/tcp");

        serviceToPort.put("entrust-aams/udp", 681);
        portToService.put(681, "entrust-aams/udp");

        serviceToPort.put("xfr/tcp", 682);
        portToService.put(682, "xfr/tcp");

        serviceToPort.put("xfr/udp", 682);
        portToService.put(682, "xfr/udp");

        serviceToPort.put("corba-iiop/tcp", 683);
        portToService.put(683, "corba-iiop/tcp");

        serviceToPort.put("corba-iiop/udp", 683);
        portToService.put(683, "corba-iiop/udp");

        serviceToPort.put("corba-iiop-ssl/tcp", 684);
        portToService.put(684, "corba-iiop-ssl/tcp");

        serviceToPort.put("corba-iiop-ssl/udp", 684);
        portToService.put(684, "corba-iiop-ssl/udp");

        serviceToPort.put("mdc-portmapper/tcp", 685);
        portToService.put(685, "mdc-portmapper/tcp");

        serviceToPort.put("mdc-portmapper/udp", 685);
        portToService.put(685, "mdc-portmapper/udp");

        serviceToPort.put("hcp-wismar/tcp", 686);
        portToService.put(686, "hcp-wismar/tcp");

        serviceToPort.put("hcp-wismar/udp", 686);
        portToService.put(686, "hcp-wismar/udp");

        serviceToPort.put("asipregistry/tcp", 687);
        portToService.put(687, "asipregistry/tcp");

        serviceToPort.put("asipregistry/udp", 687);
        portToService.put(687, "asipregistry/udp");

        serviceToPort.put("realm-rusd/tcp", 688);
        portToService.put(688, "realm-rusd/tcp");

        serviceToPort.put("realm-rusd/udp", 688);
        portToService.put(688, "realm-rusd/udp");

        serviceToPort.put("nmap/tcp", 689);
        portToService.put(689, "nmap/tcp");

        serviceToPort.put("nmap/udp", 689);
        portToService.put(689, "nmap/udp");

        serviceToPort.put("vatp/tcp", 690);
        portToService.put(690, "vatp/tcp");

        serviceToPort.put("vatp/udp", 690);
        portToService.put(690, "vatp/udp");

        serviceToPort.put("msexch-routing/tcp", 691);
        portToService.put(691, "msexch-routing/tcp");

        serviceToPort.put("msexch-routing/udp", 691);
        portToService.put(691, "msexch-routing/udp");

        serviceToPort.put("hyperwave-isp/tcp", 692);
        portToService.put(692, "hyperwave-isp/tcp");

        serviceToPort.put("hyperwave-isp/udp", 692);
        portToService.put(692, "hyperwave-isp/udp");

        serviceToPort.put("connendp/tcp", 693);
        portToService.put(693, "connendp/tcp");

        serviceToPort.put("connendp/udp", 693);
        portToService.put(693, "connendp/udp");

        serviceToPort.put("ha-cluster/tcp", 694);
        portToService.put(694, "ha-cluster/tcp");

        serviceToPort.put("ha-cluster/udp", 694);
        portToService.put(694, "ha-cluster/udp");

        serviceToPort.put("ieee-mms-ssl/tcp", 695);
        portToService.put(695, "ieee-mms-ssl/tcp");

        serviceToPort.put("ieee-mms-ssl/udp", 695);
        portToService.put(695, "ieee-mms-ssl/udp");

        serviceToPort.put("rushd/tcp", 696);
        portToService.put(696, "rushd/tcp");

        serviceToPort.put("rushd/udp", 696);
        portToService.put(696, "rushd/udp");

        serviceToPort.put("uuidgen/tcp", 697);
        portToService.put(697, "uuidgen/tcp");

        serviceToPort.put("uuidgen/udp", 697);
        portToService.put(697, "uuidgen/udp");

        serviceToPort.put("olsr/tcp", 698);
        portToService.put(698, "olsr/tcp");

        serviceToPort.put("olsr/udp", 698);
        portToService.put(698, "olsr/udp");

        serviceToPort.put("accessnetwork/tcp", 699);
        portToService.put(699, "accessnetwork/tcp");

        serviceToPort.put("accessnetwork/udp", 699);
        portToService.put(699, "accessnetwork/udp");

        serviceToPort.put("epp/tcp", 700);
        portToService.put(700, "epp/tcp");

        serviceToPort.put("epp/udp", 700);
        portToService.put(700, "epp/udp");

        serviceToPort.put("lmp/tcp", 701);
        portToService.put(701, "lmp/tcp");

        serviceToPort.put("lmp/udp", 701);
        portToService.put(701, "lmp/udp");

        serviceToPort.put("iris-beep/tcp", 702);
        portToService.put(702, "iris-beep/tcp");

        serviceToPort.put("iris-beep/udp", 702);
        portToService.put(702, "iris-beep/udp");

        serviceToPort.put("elcsd/tcp", 704);
        portToService.put(704, "elcsd/tcp");

        serviceToPort.put("elcsd/udp", 704);
        portToService.put(704, "elcsd/udp");

        serviceToPort.put("agentx/tcp", 705);
        portToService.put(705, "agentx/tcp");

        serviceToPort.put("agentx/udp", 705);
        portToService.put(705, "agentx/udp");

        serviceToPort.put("silc/tcp", 706);
        portToService.put(706, "silc/tcp");

        serviceToPort.put("silc/udp", 706);
        portToService.put(706, "silc/udp");

        serviceToPort.put("borland-dsj/tcp", 707);
        portToService.put(707, "borland-dsj/tcp");

        serviceToPort.put("borland-dsj/udp", 707);
        portToService.put(707, "borland-dsj/udp");

        serviceToPort.put("entrust-kmsh/tcp", 709);
        portToService.put(709, "entrust-kmsh/tcp");

        serviceToPort.put("entrust-kmsh/udp", 709);
        portToService.put(709, "entrust-kmsh/udp");

        serviceToPort.put("entrust-ash/tcp", 710);
        portToService.put(710, "entrust-ash/tcp");

        serviceToPort.put("entrust-ash/udp", 710);
        portToService.put(710, "entrust-ash/udp");

        serviceToPort.put("cisco-tdp/tcp", 711);
        portToService.put(711, "cisco-tdp/tcp");

        serviceToPort.put("cisco-tdp/udp", 711);
        portToService.put(711, "cisco-tdp/udp");

        serviceToPort.put("tbrpf/tcp", 712);
        portToService.put(712, "tbrpf/tcp");

        serviceToPort.put("tbrpf/udp", 712);
        portToService.put(712, "tbrpf/udp");

        serviceToPort.put("iris-xpc/tcp", 713);
        portToService.put(713, "iris-xpc/tcp");

        serviceToPort.put("iris-xpc/udp", 713);
        portToService.put(713, "iris-xpc/udp");

        serviceToPort.put("iris-xpcs/tcp", 714);
        portToService.put(714, "iris-xpcs/tcp");

        serviceToPort.put("iris-xpcs/udp", 714);
        portToService.put(714, "iris-xpcs/udp");

        serviceToPort.put("iris-lwz/tcp", 715);
        portToService.put(715, "iris-lwz/tcp");

        serviceToPort.put("iris-lwz/udp", 715);
        portToService.put(715, "iris-lwz/udp");

        serviceToPort.put("pana/udp", 716);
        portToService.put(716, "pana/udp");

        serviceToPort.put("netviewdm1/tcp", 729);
        portToService.put(729, "netviewdm1/tcp");

        serviceToPort.put("netviewdm1/udp", 729);
        portToService.put(729, "netviewdm1/udp");

        serviceToPort.put("netviewdm2/tcp", 730);
        portToService.put(730, "netviewdm2/tcp");

        serviceToPort.put("netviewdm2/udp", 730);
        portToService.put(730, "netviewdm2/udp");

        serviceToPort.put("netviewdm3/tcp", 731);
        portToService.put(731, "netviewdm3/tcp");

        serviceToPort.put("netviewdm3/udp", 731);
        portToService.put(731, "netviewdm3/udp");

        serviceToPort.put("netgw/tcp", 741);
        portToService.put(741, "netgw/tcp");

        serviceToPort.put("netgw/udp", 741);
        portToService.put(741, "netgw/udp");

        serviceToPort.put("netrcs/tcp", 742);
        portToService.put(742, "netrcs/tcp");

        serviceToPort.put("netrcs/udp", 742);
        portToService.put(742, "netrcs/udp");

        serviceToPort.put("flexlm/tcp", 744);
        portToService.put(744, "flexlm/tcp");

        serviceToPort.put("flexlm/udp", 744);
        portToService.put(744, "flexlm/udp");

        serviceToPort.put("fujitsu-dev/tcp", 747);
        portToService.put(747, "fujitsu-dev/tcp");

        serviceToPort.put("fujitsu-dev/udp", 747);
        portToService.put(747, "fujitsu-dev/udp");

        serviceToPort.put("ris-cm/tcp", 748);
        portToService.put(748, "ris-cm/tcp");

        serviceToPort.put("ris-cm/udp", 748);
        portToService.put(748, "ris-cm/udp");

        serviceToPort.put("kerberos-adm/tcp", 749);
        portToService.put(749, "kerberos-adm/tcp");

        serviceToPort.put("kerberos-adm/udp", 749);
        portToService.put(749, "kerberos-adm/udp");

        serviceToPort.put("rfile/tcp", 750);
        portToService.put(750, "rfile/tcp");

        serviceToPort.put("loadav/udp", 750);
        portToService.put(750, "loadav/udp");

        serviceToPort.put("kerberos-iv/udp", 750);
        portToService.put(750, "kerberos-iv/udp");

        serviceToPort.put("pump/tcp", 751);
        portToService.put(751, "pump/tcp");

        serviceToPort.put("pump/udp", 751);
        portToService.put(751, "pump/udp");

        serviceToPort.put("qrh/tcp", 752);
        portToService.put(752, "qrh/tcp");

        serviceToPort.put("qrh/udp", 752);
        portToService.put(752, "qrh/udp");

        serviceToPort.put("rrh/tcp", 753);
        portToService.put(753, "rrh/tcp");

        serviceToPort.put("rrh/udp", 753);
        portToService.put(753, "rrh/udp");

        serviceToPort.put("tell/tcp", 754);
        portToService.put(754, "tell/tcp");

        serviceToPort.put("tell/udp", 754);
        portToService.put(754, "tell/udp");

        serviceToPort.put("nlogin/tcp", 758);
        portToService.put(758, "nlogin/tcp");

        serviceToPort.put("nlogin/udp", 758);
        portToService.put(758, "nlogin/udp");

        serviceToPort.put("con/tcp", 759);
        portToService.put(759, "con/tcp");

        serviceToPort.put("con/udp", 759);
        portToService.put(759, "con/udp");

        serviceToPort.put("ns/tcp", 760);
        portToService.put(760, "ns/tcp");

        serviceToPort.put("ns/udp", 760);
        portToService.put(760, "ns/udp");

        serviceToPort.put("rxe/tcp", 761);
        portToService.put(761, "rxe/tcp");

        serviceToPort.put("rxe/udp", 761);
        portToService.put(761, "rxe/udp");

        serviceToPort.put("quotad/tcp", 762);
        portToService.put(762, "quotad/tcp");

        serviceToPort.put("quotad/udp", 762);
        portToService.put(762, "quotad/udp");

        serviceToPort.put("cycleserv/tcp", 763);
        portToService.put(763, "cycleserv/tcp");

        serviceToPort.put("cycleserv/udp", 763);
        portToService.put(763, "cycleserv/udp");

        serviceToPort.put("omserv/tcp", 764);
        portToService.put(764, "omserv/tcp");

        serviceToPort.put("omserv/udp", 764);
        portToService.put(764, "omserv/udp");

        serviceToPort.put("webster/tcp", 765);
        portToService.put(765, "webster/tcp");

        serviceToPort.put("webster/udp", 765);
        portToService.put(765, "webster/udp");

        serviceToPort.put("phonebook/tcp", 767);
        portToService.put(767, "phonebook/tcp");

        serviceToPort.put("phonebook/udp", 767);
        portToService.put(767, "phonebook/udp");

        serviceToPort.put("vid/tcp", 769);
        portToService.put(769, "vid/tcp");

        serviceToPort.put("vid/udp", 769);
        portToService.put(769, "vid/udp");

        serviceToPort.put("cadlock/tcp", 770);
        portToService.put(770, "cadlock/tcp");

        serviceToPort.put("cadlock/udp", 770);
        portToService.put(770, "cadlock/udp");

        serviceToPort.put("rtip/tcp", 771);
        portToService.put(771, "rtip/tcp");

        serviceToPort.put("rtip/udp", 771);
        portToService.put(771, "rtip/udp");

        serviceToPort.put("cycleserv2/tcp", 772);
        portToService.put(772, "cycleserv2/tcp");

        serviceToPort.put("cycleserv2/udp", 772);
        portToService.put(772, "cycleserv2/udp");

        serviceToPort.put("submit/tcp", 773);
        portToService.put(773, "submit/tcp");

        serviceToPort.put("notify/udp", 773);
        portToService.put(773, "notify/udp");

        serviceToPort.put("rpasswd/tcp", 774);
        portToService.put(774, "rpasswd/tcp");

        serviceToPort.put("acmaint_dbd/udp", 774);
        portToService.put(774, "acmaint_dbd/udp");

        serviceToPort.put("entomb/tcp", 775);
        portToService.put(775, "entomb/tcp");

        serviceToPort.put("acmaint_transd/udp", 775);
        portToService.put(775, "acmaint_transd/udp");

        serviceToPort.put("wpages/tcp", 776);
        portToService.put(776, "wpages/tcp");

        serviceToPort.put("wpages/udp", 776);
        portToService.put(776, "wpages/udp");

        serviceToPort.put("multiling-http/tcp", 777);
        portToService.put(777, "multiling-http/tcp");

        serviceToPort.put("multiling-http/udp", 777);
        portToService.put(777, "multiling-http/udp");

        serviceToPort.put("wpgs/tcp", 780);
        portToService.put(780, "wpgs/tcp");

        serviceToPort.put("wpgs/udp", 780);
        portToService.put(780, "wpgs/udp");

        serviceToPort.put("mdbs_daemon/tcp", 800);
        portToService.put(800, "mdbs_daemon/tcp");

        serviceToPort.put("mdbs_daemon/udp", 800);
        portToService.put(800, "mdbs_daemon/udp");

        serviceToPort.put("device/tcp", 801);
        portToService.put(801, "device/tcp");

        serviceToPort.put("device/udp", 801);
        portToService.put(801, "device/udp");

        serviceToPort.put("fcp-udp/tcp", 810);
        portToService.put(810, "fcp-udp/tcp");

        serviceToPort.put("fcp-udp/udp", 810);
        portToService.put(810, "fcp-udp/udp");

        serviceToPort.put("itm-mcell-s/tcp", 828);
        portToService.put(828, "itm-mcell-s/tcp");

        serviceToPort.put("itm-mcell-s/udp", 828);
        portToService.put(828, "itm-mcell-s/udp");

        serviceToPort.put("pkix-3-ca-ra/tcp", 829);
        portToService.put(829, "pkix-3-ca-ra/tcp");

        serviceToPort.put("pkix-3-ca-ra/udp", 829);
        portToService.put(829, "pkix-3-ca-ra/udp");

        serviceToPort.put("netconf-ssh/tcp", 830);
        portToService.put(830, "netconf-ssh/tcp");

        serviceToPort.put("netconf-ssh/udp", 830);
        portToService.put(830, "netconf-ssh/udp");

        serviceToPort.put("netconf-beep/tcp", 831);
        portToService.put(831, "netconf-beep/tcp");

        serviceToPort.put("netconf-beep/udp", 831);
        portToService.put(831, "netconf-beep/udp");

        serviceToPort.put("netconfsoaphttp/tcp", 832);
        portToService.put(832, "netconfsoaphttp/tcp");

        serviceToPort.put("netconfsoaphttp/udp", 832);
        portToService.put(832, "netconfsoaphttp/udp");

        serviceToPort.put("netconfsoapbeep/tcp", 833);
        portToService.put(833, "netconfsoapbeep/tcp");

        serviceToPort.put("netconfsoapbeep/udp", 833);
        portToService.put(833, "netconfsoapbeep/udp");

        serviceToPort.put("dhcp-failover2/tcp", 847);
        portToService.put(847, "dhcp-failover2/tcp");

        serviceToPort.put("dhcp-failover2/udp", 847);
        portToService.put(847, "dhcp-failover2/udp");

        serviceToPort.put("gdoi/tcp", 848);
        portToService.put(848, "gdoi/tcp");

        serviceToPort.put("gdoi/udp", 848);
        portToService.put(848, "gdoi/udp");

        serviceToPort.put("iscsi/tcp", 860);
        portToService.put(860, "iscsi/tcp");

        serviceToPort.put("iscsi/udp", 860);
        portToService.put(860, "iscsi/udp");

        serviceToPort.put("owamp-control/tcp", 861);
        portToService.put(861, "owamp-control/tcp");

        serviceToPort.put("owamp-control/udp", 861);
        portToService.put(861, "owamp-control/udp");

        serviceToPort.put("rsync/tcp", 873);
        portToService.put(873, "rsync/tcp");

        serviceToPort.put("rsync/udp", 873);
        portToService.put(873, "rsync/udp");

        serviceToPort.put("iclcnet-locate/tcp", 886);
        portToService.put(886, "iclcnet-locate/tcp");

        serviceToPort.put("iclcnet-locate/udp", 886);
        portToService.put(886, "iclcnet-locate/udp");

        serviceToPort.put("iclcnet_svinfo/tcp", 887);
        portToService.put(887, "iclcnet_svinfo/tcp");

        serviceToPort.put("iclcnet_svinfo/udp", 887);
        portToService.put(887, "iclcnet_svinfo/udp");

        serviceToPort.put("accessbuilder/tcp", 888);
        portToService.put(888, "accessbuilder/tcp");

        serviceToPort.put("accessbuilder/udp", 888);
        portToService.put(888, "accessbuilder/udp");

        serviceToPort.put("cddbp/tcp", 888);
        portToService.put(888, "cddbp/tcp");

        serviceToPort.put("omginitialrefs/tcp", 900);
        portToService.put(900, "omginitialrefs/tcp");

        serviceToPort.put("omginitialrefs/udp", 900);
        portToService.put(900, "omginitialrefs/udp");

        serviceToPort.put("smpnameres/tcp", 901);
        portToService.put(901, "smpnameres/tcp");

        serviceToPort.put("smpnameres/udp", 901);
        portToService.put(901, "smpnameres/udp");

        serviceToPort.put("ideafarm-door/tcp", 902);
        portToService.put(902, "ideafarm-door/tcp");

        serviceToPort.put("ideafarm-door/udp", 902);
        portToService.put(902, "ideafarm-door/udp");

        serviceToPort.put("ideafarm-panic/tcp", 903);
        portToService.put(903, "ideafarm-panic/tcp");

        serviceToPort.put("ideafarm-panic/udp", 903);
        portToService.put(903, "ideafarm-panic/udp");

        serviceToPort.put("kink/tcp", 910);
        portToService.put(910, "kink/tcp");

        serviceToPort.put("kink/udp", 910);
        portToService.put(910, "kink/udp");

        serviceToPort.put("xact-backup/tcp", 911);
        portToService.put(911, "xact-backup/tcp");

        serviceToPort.put("xact-backup/udp", 911);
        portToService.put(911, "xact-backup/udp");

        serviceToPort.put("apex-mesh/tcp", 912);
        portToService.put(912, "apex-mesh/tcp");

        serviceToPort.put("apex-mesh/udp", 912);
        portToService.put(912, "apex-mesh/udp");

        serviceToPort.put("apex-edge/tcp", 913);
        portToService.put(913, "apex-edge/tcp");

        serviceToPort.put("apex-edge/udp", 913);
        portToService.put(913, "apex-edge/udp");

        serviceToPort.put("ftps-data/tcp", 989);
        portToService.put(989, "ftps-data/tcp");

        serviceToPort.put("ftps-data/udp", 989);
        portToService.put(989, "ftps-data/udp");

        serviceToPort.put("ftps/tcp", 990);
        portToService.put(990, "ftps/tcp");

        serviceToPort.put("ftps/udp", 990);
        portToService.put(990, "ftps/udp");

        serviceToPort.put("nas/tcp", 991);
        portToService.put(991, "nas/tcp");

        serviceToPort.put("nas/udp", 991);
        portToService.put(991, "nas/udp");

        serviceToPort.put("telnets/tcp", 992);
        portToService.put(992, "telnets/tcp");

        serviceToPort.put("telnets/udp", 992);
        portToService.put(992, "telnets/udp");

        serviceToPort.put("imaps/tcp", 993);
        portToService.put(993, "imaps/tcp");

        serviceToPort.put("imaps/udp", 993);
        portToService.put(993, "imaps/udp");

        serviceToPort.put("ircs/tcp", 994);
        portToService.put(994, "ircs/tcp");

        serviceToPort.put("ircs/udp", 994);
        portToService.put(994, "ircs/udp");

        serviceToPort.put("pop3s/tcp", 995);
        portToService.put(995, "pop3s/tcp");

        serviceToPort.put("pop3s/udp", 995);
        portToService.put(995, "pop3s/udp");

        serviceToPort.put("vsinet/tcp", 996);
        portToService.put(996, "vsinet/tcp");

        serviceToPort.put("vsinet/udp", 996);
        portToService.put(996, "vsinet/udp");

        serviceToPort.put("maitrd/tcp", 997);
        portToService.put(997, "maitrd/tcp");

        serviceToPort.put("maitrd/udp", 997);
        portToService.put(997, "maitrd/udp");

        serviceToPort.put("busboy/tcp", 998);
        portToService.put(998, "busboy/tcp");

        serviceToPort.put("puparp/udp", 998);
        portToService.put(998, "puparp/udp");

        serviceToPort.put("garcon/tcp", 999);
        portToService.put(999, "garcon/tcp");

        serviceToPort.put("applix/udp", 999);
        portToService.put(999, "applix/udp");

        serviceToPort.put("puprouter/tcp", 999);
        portToService.put(999, "puprouter/tcp");

        serviceToPort.put("puprouter/udp", 999);
        portToService.put(999, "puprouter/udp");

        serviceToPort.put("cadlock2/tcp", 1000);
        portToService.put(1000, "cadlock2/tcp");

        serviceToPort.put("cadlock2/udp", 1000);
        portToService.put(1000, "cadlock2/udp");

        serviceToPort.put("surf/tcp", 1010);
        portToService.put(1010, "surf/tcp");

        serviceToPort.put("surf/udp", 1010);
        portToService.put(1010, "surf/udp");

        serviceToPort.put("exp1/tcp", 1021);
        portToService.put(1021, "exp1/tcp");

        serviceToPort.put("exp1/udp", 1021);
        portToService.put(1021, "exp1/udp");

        serviceToPort.put("exp2/tcp", 1022);
        portToService.put(1022, "exp2/tcp");

        serviceToPort.put("exp2/udp", 1022);
        portToService.put(1022, "exp2/udp");
    }
}
