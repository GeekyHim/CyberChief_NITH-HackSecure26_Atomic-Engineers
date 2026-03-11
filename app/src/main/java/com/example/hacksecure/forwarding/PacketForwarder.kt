package com.example.hacksecure.forwarding

import android.net.VpnService
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * NIO-based packet forwarder. All outbound channels are protected with
 * VpnService.protect() to avoid routing loops. Runs on a single selector thread.
 */
class PacketForwarder(
    private val vpnService: VpnService,
    private val tunOutput: FileOutputStream
) {
    private val selector = Selector.open()
    private val running = AtomicBoolean(true)
    private val udpPending = ConcurrentLinkedQueue<UdpPending>()
    private val tcpPending = ConcurrentLinkedQueue<TcpPending>()
    private val udpReverseMap = mutableMapOf<String, ClientKey>() // key: "localPort:remoteIp:remotePort"
    private val tcpChannels = mutableMapOf<String, SocketChannel>() // key: "srcIp:srcPort:dstIp:dstPort"
    private val tcpReverseMap = mutableMapOf<SocketChannel, ClientKey>()

    private data class ClientKey(val srcIp: String, val srcPort: Int, val dstIp: String, val dstPort: Int)
    private data class UdpPending(val payload: ByteArray, val client: ClientKey, val dstIp: String, val dstPort: Int)
    private data class TcpPending(val payload: ByteArray, val client: ClientKey, val dstIp: String, val dstPort: Int)

    fun start() {
        thread(name = "PacketForwarder") {
            while (running.get()) {
                try {
                    processPendingUdp()
                    processPendingTcp()
                    val n = selector.select(50)
                    if (n > 0) {
                        val keys = selector.selectedKeys().iterator()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            keys.remove()
                            if (!key.isValid) continue
                            when {
                                key.isReadable -> handleRead(key)
                                key.isWritable -> handleTcpWrite(key)
                                key.isAcceptable -> { /* not used */ }
                                key.isConnectable -> handleTcpConnect(key)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (running.get()) {
                        android.util.Log.e("VPN_FORWARD", "Selector loop error", e)
                    }
                }
            }
        }
    }

    fun stop() {
        running.set(false)
        selector.wakeup()
    }

    /** Call from TUN read loop after dedup/logging. Does not block. */
    fun forwardUdp(
        packet: ByteArray,
        length: Int,
        ipHeaderLength: Int,
        srcIp: String,
        srcPort: Int,
        dstIp: String,
        dstPort: Int
    ) {
        val udpHeaderLen = 8
        val payloadOffset = ipHeaderLength + udpHeaderLen
        if (length <= payloadOffset) return
        val payloadLen = length - payloadOffset
        val payload = packet.copyOfRange(payloadOffset, length)
        udpPending.offer(UdpPending(payload, ClientKey(srcIp, srcPort, dstIp, dstPort), dstIp, dstPort))
        selector.wakeup()
    }

    /** Call from TUN read loop after dedup/logging. Does not block. */
    fun forwardTcp(
        packet: ByteArray,
        length: Int,
        ipHeaderLength: Int,
        srcIp: String,
        srcPort: Int,
        dstIp: String,
        dstPort: Int
    ) {
        val tcpHeaderLen = ((packet[ipHeaderLength + 12].toInt() and 0xFF) shr 4) * 4
        val payloadOffset = ipHeaderLength + tcpHeaderLen
        if (length <= payloadOffset) return
        val payload = packet.copyOfRange(payloadOffset, length)
        tcpPending.offer(TcpPending(payload, ClientKey(srcIp, srcPort, dstIp, dstPort), dstIp, dstPort))
        selector.wakeup()
    }

    private fun processPendingUdp() {
        while (true) {
            val p = udpPending.poll() ?: break
            try {
                val channel = DatagramChannel.open()
                channel.configureBlocking(false)
                channel.bind(null)
                vpnService.protect(channel.socket())
                val remote = InetSocketAddress(p.dstIp, p.dstPort)
                channel.send(ByteBuffer.wrap(p.payload), remote)
                val localPort = (channel.localAddress as? InetSocketAddress)?.port ?: 0
                val key = "$localPort:${p.dstIp}:${p.dstPort}"
                synchronized(udpReverseMap) { udpReverseMap[key] = p.client }
                channel.register(selector, SelectionKey.OP_READ, key)
            } catch (e: Exception) {
                android.util.Log.e("VPN_FORWARD", "UDP send failed", e)
            }
        }
    }

    private fun processPendingTcp() {
        while (true) {
            val p = tcpPending.poll() ?: break
            val key = "${p.client.srcIp}:${p.client.srcPort}:${p.client.dstIp}:${p.client.dstPort}"
            try {
                val channel = synchronized(tcpChannels) { tcpChannels[key] }
                if (channel != null && channel.isConnected) {
                    val buf = ByteBuffer.wrap(p.payload)
                    channel.write(buf)
                    continue
                }
                val newChannel = SocketChannel.open()
                newChannel.configureBlocking(false)
                vpnService.protect(newChannel.socket())
                newChannel.connect(InetSocketAddress(p.dstIp, p.dstPort))
                synchronized(tcpChannels) { tcpChannels[key] = newChannel }
                synchronized(tcpReverseMap) { tcpReverseMap[newChannel] = p.client }
                newChannel.register(selector, SelectionKey.OP_CONNECT, Pair(key, p.payload))
            } catch (e: Exception) {
                android.util.Log.e("VPN_FORWARD", "TCP connect/send failed", e)
            }
        }
    }

    private fun handleRead(key: SelectionKey) {
        val k = key.attachment()
        if (k is String) {
            // UDP
            val channel = key.channel() as DatagramChannel
            val buf = ByteBuffer.allocate(4096)
            val from = channel.receive(buf) as? InetSocketAddress ?: return
            buf.flip()
            if (buf.remaining() == 0) return
            val data = ByteArray(buf.remaining())
            buf.get(data)
            val localPort = (channel.localAddress as? InetSocketAddress)?.port ?: return
            val mapKey = "$localPort:${from.address.hostAddress}:${from.port}"
            val client = synchronized(udpReverseMap) { udpReverseMap.remove(mapKey) } ?: return
            val packet = buildUdpResponse(client.srcIp, client.srcPort, from.address.hostAddress ?: "", from.port, data)
            key.cancel()
            channel.close()
            writeToTun(packet)
        } else if (key.channel() is SocketChannel) {
            // TCP read
            val channel = key.channel() as SocketChannel
            val buf = ByteBuffer.allocate(16384)
            val n = channel.read(buf)
            if (n <= 0) return
            buf.flip()
            val data = ByteArray(buf.remaining())
            buf.get(data)
            val client = synchronized(tcpReverseMap) { tcpReverseMap[channel] } ?: return
            val packet = buildTcpResponse(client.srcIp, client.srcPort, client.dstIp, client.dstPort, data)
            writeToTun(packet)
        }
    }

    private fun handleTcpConnect(key: SelectionKey) {
        val channel = key.channel() as SocketChannel
        @Suppress("UNCHECKED_CAST")
        val attachment = key.attachment() as Pair<String, ByteArray>
        val (mapKey, payload) = attachment
        try {
            if (channel.finishConnect()) {
                key.interestOps(SelectionKey.OP_READ)
                val buf = ByteBuffer.wrap(payload)
                channel.write(buf)
            }
        } catch (e: Exception) {
            key.cancel()
            channel.close()
            synchronized(tcpChannels) { tcpChannels.remove(mapKey) }
            synchronized(tcpReverseMap) { tcpReverseMap.remove(channel) }
        }
    }

    private fun handleTcpWrite(key: SelectionKey) {
        key.interestOps(SelectionKey.OP_READ)
    }

    private fun writeToTun(packet: ByteArray) {
        try {
            tunOutput.write(packet)
            tunOutput.flush()
        } catch (e: Exception) {
            android.util.Log.e("VPN_FORWARD", "TUN write failed", e)
        }
    }

    private fun buildUdpResponse(
        destIp: String,
        destPort: Int,
        srcIp: String,
        srcPort: Int,
        payload: ByteArray
    ): ByteArray {
        val udpLen = 8 + payload.size
        val totalLen = 20 + udpLen
        val packet = ByteArray(totalLen)
        // IP header
        packet[0] = 0x45
        packet[1] = 0
        packet[2] = (totalLen shr 8).toByte()
        packet[3] = (totalLen and 0xFF).toByte()
        packet[4] = 0
        packet[5] = 0
        packet[6] = 0x40
        packet[7] = 0
        packet[8] = 64
        packet[9] = 17 // UDP
        packet[10] = 0
        packet[11] = 0
        writeIp(packet, 12, srcIp)
        writeIp(packet, 16, destIp)
        val ipChecksum = ipChecksum(packet, 0, 20)
        packet[10] = (ipChecksum shr 8).toByte()
        packet[11] = (ipChecksum and 0xFF).toByte()
        // UDP header
        packet[20] = (srcPort shr 8).toByte()
        packet[21] = (srcPort and 0xFF).toByte()
        packet[22] = (destPort shr 8).toByte()
        packet[23] = (destPort and 0xFF).toByte()
        packet[24] = (udpLen shr 8).toByte()
        packet[25] = (udpLen and 0xFF).toByte()
        packet[26] = 0
        packet[27] = 0
        System.arraycopy(payload, 0, packet, 28, payload.size)
        val udpChecksum = udpChecksum(srcIp, destIp, packet, 20, udpLen)
        packet[26] = (udpChecksum shr 8).toByte()
        packet[27] = (udpChecksum and 0xFF).toByte()
        return packet
    }

    private fun buildTcpResponse(
        destIp: String,
        destPort: Int,
        srcIp: String,
        srcPort: Int,
        payload: ByteArray
    ): ByteArray {
        val tcpLen = 20 + payload.size
        val totalLen = 20 + tcpLen
        val packet = ByteArray(totalLen)
        // IP header
        packet[0] = 0x45
        packet[1] = 0
        packet[2] = (totalLen shr 8).toByte()
        packet[3] = (totalLen and 0xFF).toByte()
        packet[4] = 0
        packet[5] = 0
        packet[6] = 0x40
        packet[7] = 0
        packet[8] = 64
        packet[9] = 6 // TCP
        packet[10] = 0
        packet[11] = 0
        writeIp(packet, 12, srcIp)
        writeIp(packet, 16, destIp)
        val ipChecksum = ipChecksum(packet, 0, 20)
        packet[10] = (ipChecksum shr 8).toByte()
        packet[11] = (ipChecksum and 0xFF).toByte()
        // TCP header (minimal: seq=0, ack=0, flags=0x10 PSH, window=65535)
        packet[20] = (srcPort shr 8).toByte()
        packet[21] = (srcPort and 0xFF).toByte()
        packet[22] = (destPort shr 8).toByte()
        packet[23] = (destPort and 0xFF).toByte()
        packet[24] = 0
        packet[25] = 0
        packet[26] = 0
        packet[27] = 0
        packet[28] = 0
        packet[29] = 0
        packet[30] = 0
        packet[31] = 0
        packet[32] = 0x50 // data offset 5 words
        packet[33] = 0x10 // PSH
        packet[34] = (65535 shr 8).toByte()
        packet[35] = (65535 and 0xFF).toByte()
        packet[36] = 0
        packet[37] = 0
        packet[38] = 0
        packet[39] = 0
        System.arraycopy(payload, 0, packet, 40, payload.size)
        val tcpChecksum = tcpChecksum(srcIp, destIp, packet, 20, tcpLen)
        packet[36] = (tcpChecksum shr 8).toByte()
        packet[37] = (tcpChecksum and 0xFF).toByte()
        return packet
    }

    private fun writeIp(packet: ByteArray, off: Int, ip: String) {
        val parts = ip.split(".")
        for (i in 0..3) {
            packet[off + i] = (parts.getOrNull(i)?.toIntOrNull() ?: 0).toByte()
        }
    }

    private fun ipChecksum(buf: ByteArray, off: Int, len: Int): Int {
        var sum = 0
        var i = off
        while (i < off + len - 1) {
            sum += ((buf[i].toInt() and 0xFF) shl 8) or (buf[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < off + len) sum += (buf[i].toInt() and 0xFF) shl 8
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv() and 0xFFFF
    }

    private fun udpChecksum(srcIp: String, dstIp: String, udpSegment: ByteArray, off: Int, len: Int): Int {
        var sum = 0
        for (part in srcIp.split(".")) sum += part.toIntOrNull() ?: 0
        for (part in dstIp.split(".")) sum += part.toIntOrNull() ?: 0
        sum += 17 // protocol
        sum += len
        var i = off
        while (i < off + len - 1) {
            sum += ((udpSegment[i].toInt() and 0xFF) shl 8) or (udpSegment[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < off + len) sum += (udpSegment[i].toInt() and 0xFF) shl 8
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv() and 0xFFFF
    }

    private fun tcpChecksum(srcIp: String, dstIp: String, tcpSegment: ByteArray, off: Int, len: Int): Int {
        var sum = 0
        for (part in srcIp.split(".")) sum += part.toIntOrNull() ?: 0
        for (part in dstIp.split(".")) sum += part.toIntOrNull() ?: 0
        sum += 6 // protocol
        sum += len
        var i = off
        while (i < off + len - 1) {
            sum += ((tcpSegment[i].toInt() and 0xFF) shl 8) or (tcpSegment[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < off + len) sum += (tcpSegment[i].toInt() and 0xFF) shl 8
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv() and 0xFFFF
    }
}
