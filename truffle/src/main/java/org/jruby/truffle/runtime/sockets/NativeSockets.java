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
import jnr.ffi.byref.IntByReference;

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

    /*
     * int
     * socket(int domain, int type, int protocol);
     */

    int socket(int domain, int type, int protocol);

    /*
     * int
     * setsockopt(int socket, int level, int option_name,
     *            const void *option_value, socklen_t option_len);
     */

    int setsockopt(int socket, int level, int option_name, Pointer option_value, int option_len);

    /*
     * int
     * bind(int socket, const struct sockaddr *address, socklen_t address_len);
     */

    int bind(int socket, Pointer address, int address_len);

    /*
     * int
     * listen(int socket, int backlog);
     */

    int listen(int socket, int backlog);

    /*
     * int
     * accept(int socket, struct sockaddr *restrict address,
     *        socklen_t *restrict address_len);
     */

    int accept(int socket, Pointer address, IntByReference addressLength);

    /*
     * int
     * gethostname(char *name, size_t namelen);
     */

    int gethostname(Pointer name, int namelen);

    /*
     * int
     * select(int nfds, fd_set *restrict readfds, fd_set *restrict writefds,
     *        fd_set *restrict errorfds, struct timeval *restrict timeout);
     */

    int select(int nfds, Pointer readfds, Pointer writefds, Pointer errorfds, Pointer timeout);

    /*
     * int
     * getpeername(int socket, struct sockaddr *restrict address,
     *             socklen_t *restrict address_len);
     */

    int getpeername(int socket, Pointer address, Pointer address_len);

    /*
     * int
     * getsockname(int socket, struct sockaddr *restrict address,
     *             socklen_t *restrict address_len);
     */

    int getsockname(int socket, Pointer address, Pointer address_len);

    /**
     * int connect(int sockfd, const struct sockaddr *addr,
     * socklen_t addrlen);
     */

    int connect(int socket, Pointer address, int address_len);

}
