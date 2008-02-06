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
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby;
/**
 * Error numbers.
 * @fixme
 * this interface is a big hack defining a bunch of arbitrary valor as system call error numbers
 * this is actually because I need them but will probably need to be changed to something smarter 
 * sooner or later.
 * The purpose of this class it to help implement the Errno module which in turn in needed by rubicon.
 * @author Benoit Cerrina
 **/
public interface IErrno
{
    int EPERM = 1;
    int ENOENT = 2;
    int ESRCH = 3;
    int EINTR = 4;
    int EIO = 5;
    int ENXIO = 6;
    int E2BIG = 7;
    int ENOEXEC = 8;
    int EBADF = 9;
    int ECHILD = 10;
    int EDEADLK = 11;
    int ENOMEM = 12;
    int EACCES = 13;
    int EFAULT = 14;
    int ENOTBLK = 15;
    int EBUSY = 16;
    int EEXIST = 17;
    int EXDEV = 18;
    int ENODEV = 19;
    int ENOTDIR = 20;
    int EISDIR = 21;
    int EINVAL = 22;
    int ENFILE = 23;
    int EMFILE = 24;
    int ENOTTY = 25;
    int ETXTBSY = 26;
    int EFBIG = 27;
    int ENOSPC = 28;
    int ESPIPE = 29;
    int EROFS = 30;
    int EMLINK = 31;
    int EPIPE = 32;
    int EDOM = 33;
    int ERANGE = 34;
    int EWOULDBLOCK = 35;
    int EAGAIN = 35;
    int EINPROGRESS = 36;
    int EALREADY = 37;
    int ENOTSOCK = 38;
    int EDESTADDRREQ = 39;
    int EMSGSIZE = 40;
    int EPROTOTYPE = 41;
    int ENOPROTOOPT = 42;
    int EPROTONOSUPPORT = 43;
    int ESOCKTNOSUPPORT = 44;
    int EOPNOTSUPP = 45;
    int EPFNOSUPPORT = 46;
    int EAFNOSUPPORT = 47;
    int EADDRINUSE = 48;
    int EADDRNOTAVAIL = 49;
    int ENETDOWN = 50;
    int ENETUNREACH = 51;
    int ENETRESET = 52;
    int ECONNABORTED = 53;
    int ECONNRESET = 54;
    int ENOBUFS = 55;
    int EISCONN = 56;
    int ENOTCONN = 57;
    int ESHUTDOWN = 58;
    int ETOOMANYREFS = 59;
    int ETIMEDOUT = 60;
    int ECONNREFUSED = 61;
    int ELOOP = 62;
    int ENAMETOOLONG = 63;
    int EHOSTDOWN = 64;
    int EHOSTUNREACH = 65;
    int ENOTEMPTY = 66;
    int EUSERS = 68;
    int EDQUOT = 69;
    int ESTALE = 70;
    int EREMOTE = 71;
    int ENOLCK = 77;
    int ENOSYS = 78;
    int EOVERFLOW = 84;
    int EIDRM = 90;
    int ENOMSG = 91;
    int EILSEQ = 92;
    int EBADMSG = 94;
    int EMULTIHOP = 95;
    int ENODATA = 96;
    int ENOLINK = 97;
    int ENOSR = 98;
    int ENOSTR = 99;
    int EPROTO = 100;
    int ETIME = 101;
    int EOPNOTSUPP_DARWIN = 102;
}
