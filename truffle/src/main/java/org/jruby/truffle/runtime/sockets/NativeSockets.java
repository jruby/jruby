/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.sockets;

import jnr.ffi.Pointer;

public interface NativeSockets {

    /*
     * int
     * getaddrinfo(const char *hostname, const char *servname,
     *             const struct addrinfo *hints, struct addrinfo **res);
     */

    int getaddrinfo(CharSequence hostname, CharSequence servname, Pointer hints, Pointer res);

    /*
     * void
     * freeaddrinfo(struct addrinfo *ai);
     */

    void freeaddrinfo(Pointer ai);

    /*
     * int
     * getnameinfo(const struct sockaddr *sa, socklen_t salen, char *host,
     *             socklen_t hostlen, char *serv, socklen_t servlen, int flags);
     */

    int getnameinfo(Pointer sa, int salen, Pointer host, int hostlen, Pointer serv, int servlen, int flags);

}
