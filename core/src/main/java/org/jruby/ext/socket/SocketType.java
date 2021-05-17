/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ext.socket;

import jnr.constants.platform.Sock;
import jnr.constants.platform.SocketOption;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Encapsulates behavior across the primary socket types Socket,
 * ServerSocket, and Datagramsocket.
*/
public enum SocketType {
    SOCKET(Sock.SOCK_STREAM) {
        private Socket toSocket(Channel channel) {
            return ((SocketChannel)channel).socket();
        }

        public int getSoTimeout(Channel channel) throws IOException {
            return toSocket(channel).getSoTimeout();
        }

        public void setSoTimeout(Channel channel, int timeout) throws IOException {
            toSocket(channel).setSoTimeout(timeout);
        }

        public boolean getReuseAddress(Channel channel) throws IOException {
            return toSocket(channel).getReuseAddress();
        }

        public void setReuseAddress(Channel channel, boolean reuse) throws IOException {
            toSocket(channel).setReuseAddress(reuse);
        }

        public int getSendBufferSize(Channel channel) throws IOException {
            return toSocket(channel).getSendBufferSize();
        }

        public void setSendBufferSize(Channel channel, int size) throws IOException {
            toSocket(channel).setSendBufferSize(size);
        }

        public int getReceiveBufferSize(Channel channel) throws IOException {
            return toSocket(channel).getReceiveBufferSize();
        }

        public void setReceiveBufferSize(Channel channel, int i) throws IOException {
            toSocket(channel).setReceiveBufferSize(i);
        }

        public boolean getOOBInline(Channel channel) throws IOException {
            return toSocket(channel).getOOBInline();
        }

        public void setOOBInline(Channel channel, boolean b) throws IOException {
            toSocket(channel).setOOBInline(b);
        }

        public int getSoLinger(Channel channel) throws IOException {
            return toSocket(channel).getSoLinger();
        }

        public void setSoLinger(Channel channel, boolean b, int i) throws IOException {
            toSocket(channel).setSoLinger(b, i);
        }

        public boolean getKeepAlive(Channel channel) throws IOException {
            return toSocket(channel).getKeepAlive();
        }

        public void setKeepAlive(Channel channel, boolean b) throws IOException {
            toSocket(channel).setKeepAlive(b);
        }

        public boolean getTcpNoDelay(Channel channel) throws IOException {
            return toSocket(channel).getTcpNoDelay();
        }

        public void setTcpNoDelay(Channel channel, boolean b) throws IOException {
            toSocket(channel).setTcpNoDelay(b);
        }

        public SocketAddress getRemoteSocketAddress(Channel channel) {
            return toSocket(channel).getRemoteSocketAddress();
        }

        public SocketAddress getLocalSocketAddress(Channel channel) {
            return toSocket(channel).getLocalSocketAddress();
        }

        public void shutdownInput(Channel channel)throws IOException {
            toSocket(channel).shutdownInput();
        }

        public void shutdownOutput(Channel channel)throws IOException {
            toSocket(channel).shutdownOutput();
        }
    },

    SERVER(Sock.SOCK_STREAM) {
        private ServerSocket toSocket(Channel channel) {
            return ((ServerSocketChannel)channel).socket();
        }

        public int getSoTimeout(Channel channel) throws IOException {
            return toSocket(channel).getSoTimeout();
        }

        public void setSoTimeout(Channel channel, int timeout) throws IOException {
            toSocket(channel).setSoTimeout(timeout);
        }

        public boolean getReuseAddress(Channel channel) throws IOException {
            return toSocket(channel).getReuseAddress();
        }

        public void setReuseAddress(Channel channel, boolean reuse) throws IOException {
            toSocket(channel).setReuseAddress(reuse);
        }

        public int getSendBufferSize(Channel channel) throws IOException {
            return 0;
        }

        public void setSendBufferSize(Channel channel, int size) throws IOException {
        }

        public int getReceiveBufferSize(Channel channel) throws IOException {
            return toSocket(channel).getReceiveBufferSize();
        }

        public void setReceiveBufferSize(Channel channel, int i) throws IOException {
            toSocket(channel).setReceiveBufferSize(i);
        }

        public SocketAddress getLocalSocketAddress(Channel channel) {
            return toSocket(channel).getLocalSocketAddress();
        }
    },

    DATAGRAM(Sock.SOCK_DGRAM) {
        private DatagramSocket toSocket(Channel channel) {
            return ((DatagramChannel)channel).socket();
        }

        public int getSoTimeout(Channel channel) throws IOException {
            return toSocket(channel).getSoTimeout();
        }

        public void setSoTimeout(Channel channel, int timeout) throws IOException {
            toSocket(channel).setSoTimeout(timeout);
        }

        public boolean getReuseAddress(Channel channel) throws IOException {
            return toSocket(channel).getReuseAddress();
        }

        public void setReuseAddress(Channel channel, boolean reuse) throws IOException {
            toSocket(channel).setReuseAddress(reuse);
        }

        public int getSendBufferSize(Channel channel) throws IOException {
            return toSocket(channel).getSendBufferSize();
        }

        public void setSendBufferSize(Channel channel, int size) throws IOException {
            toSocket(channel).setSendBufferSize(size);
        }

        public int getReceiveBufferSize(Channel channel) throws IOException {
            return toSocket(channel).getReceiveBufferSize();
        }

        public void setReceiveBufferSize(Channel channel, int i) throws IOException {
            toSocket(channel).setReceiveBufferSize(i);
        }

        public boolean getBroadcast(Channel channel) throws IOException {
            return toSocket(channel).getBroadcast();
        }
        public void setBroadcast(Channel channel, boolean b) throws IOException {
            toSocket(channel).setBroadcast(b);
        }

        public SocketAddress getRemoteSocketAddress(Channel channel) {
            return toSocket(channel).getRemoteSocketAddress();
        }

        public SocketAddress getLocalSocketAddress(Channel channel) {
            return toSocket(channel).getLocalSocketAddress();
        }
    },

    UNIX(Sock.SOCK_STREAM) {
        private UnixSocketChannel toSocket(Channel channel) {
            return (UnixSocketChannel)channel;
        }

        public void shutdownInput(Channel channel)throws IOException {
            toSocket(channel).shutdownInput();
        }

        public void shutdownOutput(Channel channel) throws IOException {
            toSocket(channel).shutdownOutput();
        }

        public SocketAddress getRemoteSocketAddress(Channel channel) {
            return toSocket(channel).getRemoteSocketAddress();
        }

        public SocketAddress getLocalSocketAddress(Channel channel) {
            return new UnixSocketAddress(new File(""));
        }
    },

    UNKNOWN(Sock.SOCK_STREAM);

    public static SocketType forChannel(Channel channel) {
        if (channel instanceof SocketChannel) {
            return SOCKET;

        } else if (channel instanceof ServerSocketChannel) {
            return SERVER;

        } else if (channel instanceof DatagramChannel) {
            return DATAGRAM;

        } else if (channel instanceof UnixSocketChannel) {
            return UNIX;

        }

        return UNKNOWN;
    }

    public int getSoTimeout(Channel channel) throws IOException { return 0; }
    public void setSoTimeout(Channel channel, int timeout) throws IOException {}

    public boolean getReuseAddress(Channel channel) throws IOException { return false; }
    public void setReuseAddress(Channel channel, boolean reuse) throws IOException {}

    public int getSendBufferSize(Channel channel) throws IOException { return 0; }
    public void setSendBufferSize(Channel channel, int size) throws IOException {}

    public int getReceiveBufferSize(Channel channel) throws IOException { return 0; }
    public void setReceiveBufferSize(Channel channel, int i) throws IOException {}

    public boolean getOOBInline(Channel channel) throws IOException { return false; }
    public void setOOBInline(Channel channel, boolean b) throws IOException {}

    public int getSoLinger(Channel channel) throws IOException { return 0; }
    public void setSoLinger(Channel channel, boolean b, int i) throws IOException {}

    public boolean getKeepAlive(Channel channel) throws IOException { return false; }
    public void setKeepAlive(Channel channel, boolean b) throws IOException {}

    public boolean getTcpNoDelay(Channel channel) throws IOException { return false; }
    public void setTcpNoDelay(Channel channel, boolean b) throws IOException {}

    public boolean getBroadcast(Channel channel) throws IOException { return false; }
    public void setBroadcast(Channel channel, boolean b) throws IOException {}

    public void shutdownInput(Channel channel) throws IOException {}
    public void shutdownOutput(Channel channel) throws IOException {}

    public SocketAddress getRemoteSocketAddress(Channel channel) {
        return null;
    }

    public SocketAddress getLocalSocketAddress(Channel channel) {
        return null;
    }

    public Sock getSocketType() {
        return sock;
    }

    public int getSocketOption(Channel channel, SocketOption option) throws IOException {
        switch (option) {

            case SO_BROADCAST:
                return getBroadcast(channel) ? 1 : 0;

            case SO_KEEPALIVE:
                return getKeepAlive(channel) ? 1 : 0;

            case SO_LINGER: {
                return getSoLinger(channel);
            }

            case SO_OOBINLINE:
                return getOOBInline(channel) ? 1 : 0;

            case SO_RCVBUF:
                return getReceiveBufferSize(channel);

            case SO_REUSEADDR:
                return getReuseAddress(channel) ? 1 : 0;

            case SO_SNDBUF:
                return getSendBufferSize(channel);

            case SO_RCVTIMEO:
            case SO_SNDTIMEO:
                return getSoTimeout(channel);

            case SO_TYPE:
                return getSocketType().intValue();

            // Can't support the rest with Java
            case SO_RCVLOWAT:
                return 1;

            case SO_SNDLOWAT:
                return 2048;

            case SO_DEBUG:
            case SO_ERROR:
            case SO_DONTROUTE:
            case SO_TIMESTAMP:
                return 0;

        }

        return 0;
    }

    public void setSocketOption(Channel channel, SocketOption option, int value) throws IOException {
        switch (option) {

            case SO_BROADCAST:
                setBroadcast(channel, asBoolean(value));
                break;

            case SO_KEEPALIVE:
                setKeepAlive(channel, asBoolean(value));
                break;

            case SO_LINGER:
                setSoLinger(channel, value <= 0, value);
                break;

            case SO_OOBINLINE:
                setOOBInline(channel, asBoolean(value));
                break;

            case SO_RCVBUF:
                setReceiveBufferSize(channel, value);
                break;

            case SO_REUSEADDR:
                setReuseAddress(channel, asBoolean(value));
                break;

            case SO_SNDBUF:
                setSendBufferSize(channel, value);
                break;

            case SO_RCVTIMEO:
            case SO_SNDTIMEO:
                setSoTimeout(channel, value);
                break;

            // can't set these
            case SO_TYPE:
            case SO_RCVLOWAT:
            case SO_SNDLOWAT:
            case SO_DEBUG:
            case SO_ERROR:
            case SO_DONTROUTE:
            case SO_TIMESTAMP:
        }
    }

    private static boolean asBoolean(int value) {
        return value == 0 ? false : true;
    }

    private SocketType(Sock sock) {
        this.sock = sock;
    }

    private final Sock sock;
}
