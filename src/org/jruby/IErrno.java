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
    int    ENOTEMPTY    = 1;
    int    ERANGE       = 2;
    int    ESPIPE       = 3;
    int    ENFILE       = 4;
    int    EXDEV        = 5;
    int    ENOMEM       = 6;
    int    E2BIG        = 7;
    int    ENOENT       = 8;
    int    ENOSYS       = 9;
    int    EDOM         = 10;
    int    ENOSPC       = 11;
    int    EINVAL       = 42;
    int    EEXIST       = 43;
    int    EAGAIN       = 44;
    int    ENXIO        = 45;
    int    EILSEQ       = 46;
    int    ENOLCK       = 47;
    int    EPIPE        = 48;
    int    EFBIG        = 49;
    int    EISDIR       = 50;
    int    EBUSY        = 51;
    int    ECHILD       = 52;
    int    EIO          = 53;
    int    EPERM        = 54;
    int    EDEADLOCK    = 55;
    int    ENAMETOOLONG = 56;
    int    EMLINK       = 57;
    int    ENOTTY       = 58;
    int    ENOTDIR      = 59;
    int    EFAULT       = 60;
    int    EBADF        = 61;
    int    EINTR        = 62;
    int    EWOULDBLOCK  = 63;
    int    EDEADLK      = 64;
    int    EROFS        = 65;
    int    EMFILE       = 66;
    int    ENODEV       = 67;
    int    EACCES       = 68;
    int    ENOEXEC      = 69;
    int    ESRCH        = 70;
    int    ECONNREFUSED = 71;
    int    ECONNRESET   = 72;
    int    EADDRINUSE   = 73;
}
