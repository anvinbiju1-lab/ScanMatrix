package com.example.securityapp.data.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocalVpnService : VpnService() {

    @Inject lateinit var dnsLogRepository: com.example.securityapp.data.repository.DnsLogRepository

    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    // Cache to prevent hitting VirusTotal for every single request
    private val domainCache = mutableMapOf<String, Boolean>()
    
    // Whitelist of common safe domains to skip API checks
    private val safeDomains = setOf("google.com", "googleapis.com", "whatsapp.net", "gstatic.com", "apple.com", "cloudflare.com")

    private val upstreamDns = "8.8.8.8"
    private val vpnIp = "10.0.0.2"
    private val dnsIp = "10.0.0.1"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_VPN) {
            createNotificationChannel()
            val notification = createNotification()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            
            startVpn()
        } else if (intent?.action == ACTION_STOP_VPN) {
            stopVpn()
        }
        return Service.START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN Protection",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ScanMatrix Connected")
            .setContentText("Connection protection is active")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .build()
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        try {
            val builder = Builder()
            builder.addAddress(vpnIp, 32)
            builder.addRoute(dnsIp, 32) // Route ONLY DNS queries to the VPN
            builder.addDnsServer(dnsIp)
            builder.setSession("ScanMatrix Monitor")
            
            // For newer Android versions, specifically allow apps to bypass VPN if needed
            // to avoid blocking all traffic if the local proxy fails
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                Log.e("LocalVpnService", "Failed to establish VPN: builder.establish() returned null")
                stopVpn()
                return
            }
            Log.d("LocalVpnService", "VPN established successfully")

            isRunning = true
            startTunLoop()
        } catch (e: Exception) {
            Log.e("LocalVpnService", "Exception during VPN establish", e)
            stopVpn()
        }
    }

    private fun startTunLoop() {
        scope.launch {
            val vpnFd = vpnInterface?.fileDescriptor ?: return@launch
            val inputStream = FileInputStream(vpnFd)
            val outputStream = FileOutputStream(vpnFd)

            val buffer = ByteArray(32767)

            // We don't need a global UDP listener because we'll use per-query sockets
            // for simpler and more reliable DNS forwarding.

            while (isRunning) {
                try {
                    val length = inputStream.read(buffer)
                    if (length > 0) {
                        val packet = buffer.copyOf(length)
                        handleTunPacket(packet, outputStream)
                    }
                } catch (e: Exception) {
                    if (isRunning) Log.e("VPN", "TUN read error", e)
                }
            }
        }
    }

    private fun handleTunPacket(packet: ByteArray, out: FileOutputStream) {
        if (packet.size < 28) return // IP + UDP header min size
        val version = (packet[0].toInt() and 0xF0) shr 4
        if (version != 4) return // IPv4 only

        val protocol = packet[9].toInt()
        if (protocol != 17) return // UDP only (17)

        val ipHeaderLen = (packet[0].toInt() and 0x0F) * 4
        
        val srcPort = ((packet[ipHeaderLen].toInt() and 0xFF) shl 8) or (packet[ipHeaderLen + 1].toInt() and 0xFF)
        val dstPort = ((packet[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or (packet[ipHeaderLen + 3].toInt() and 0xFF)

        if (dstPort != 53) return // DNS only

        val udpHeaderLen = 8
        val payloadOffset = ipHeaderLen + udpHeaderLen
        val payloadLen = packet.size - payloadOffset
        
        if (payloadLen <= 0) return

        val dnsPayload = packet.copyOfRange(payloadOffset, packet.size)
        val domain = extractDnsQueryName(dnsPayload)

        Log.d("VPN", "Intercepted DNS query for: $domain")

        // 1. Check if domain is definitely safe (whitelist)
        val isSafe = safeDomains.any { domain.endsWith(it) }
        
        var isSuspicious = false
        if (!isSafe) {
            // 2. Check cache
            if (domainCache.containsKey(domain)) {
                isSuspicious = domainCache[domain] == true
            } else {
                // 3. Fallback to VirusTotal / Heuristics
                // We simulate a fast heuristic check here for the demo because a real VT API call 
                // blockingly inside this network loop without proper async buffering can drop packets.
                val blockList = listOf(
                    "tracker.suspicious.net", 
                    "malware.example.com", 
                    "ads.test", 
                    "pixel.test", 
                    "phishing-login.com",
                    "google.test",
                    "eicar.org"
                )
                isSuspicious = blockList.any { domain.contains(it) }
                domainCache[domain] = isSuspicious
            }
        }
        
        dnsLogRepository.addLog(domain, isSuspicious)

        if (isSuspicious) {
            Log.d("VPN", "BLOCKED DNS: $domain")
            // Send NXDOMAIN
            val responsePayload = createNxDomainResponse(dnsPayload)
            sendDnsResponse(packet, responsePayload, ipHeaderLen, srcPort, out)
            
            // Trigger the Popup Alert
            launchSafeBrowsingPopup(domain)
        } else {
            // Forward DNS
            scope.launch {
                var querySocket: DatagramSocket? = null
                try {
                    querySocket = DatagramSocket()
                    protect(querySocket)
                    querySocket.soTimeout = 5000 // 5 second timeout

                    val upstreamAddr = InetAddress.getByName(upstreamDns)
                    val dp = DatagramPacket(dnsPayload, dnsPayload.size, upstreamAddr, 53)
                    querySocket.send(dp)
                    
                    val recvBuf = ByteArray(4096)
                    val replyDp = DatagramPacket(recvBuf, recvBuf.size)
                    querySocket.receive(replyDp)
                    
                    sendDnsResponse(packet, recvBuf.copyOf(replyDp.length), ipHeaderLen, srcPort, out)
                } catch (e: Exception) {
                    Log.e("VPN", "Upstream DNS error for $domain", e)
                } finally {
                    try { querySocket?.close() } catch (e: Exception) {}
                }
            }
        }
    }

    private fun launchSafeBrowsingPopup(domain: String) {
        val intent = Intent(this, com.example.securityapp.ui.screens.safebrowsing.SafeBrowsingActivity::class.java).apply {
            action = "com.example.securityapp.ACTION_VPN_BLOCK"
            putExtra("BLOCKED_URL", "https://$domain")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("VPN", "Failed to launch SafeBrowsingActivity from background", e)
        }
    }

    private fun sendDnsResponse(originalPacket: ByteArray, dnsPayload: ByteArray, ipHeaderLen: Int, originalSrcPort: Int, out: FileOutputStream) {
        val totalLen = 20 + 8 + dnsPayload.size
        val response = ByteArray(totalLen)
        
        // --- IPv4 Header ---
        response[0] = 0x45 // Version 4, IHL 5
        response[1] = 0x00 // TOS
        response[2] = (totalLen shr 8).toByte()
        response[3] = (totalLen and 0xFF).toByte()
        response[4] = 0x00 // ID
        response[5] = 0x00
        response[6] = 0x40 // Flags (DF)
        response[7] = 0x00 // Fragment offset
        response[8] = 0x40 // TTL 64
        response[9] = 17   // UDP
        // Checksum later
        // Source IP matches original Dest IP (10.0.0.1)
        System.arraycopy(originalPacket, 16, response, 12, 4)
        // Dest IP matches original Source IP (10.0.0.2)
        System.arraycopy(originalPacket, 12, response, 16, 4)
        
        // Calculate IP checksum
        var ipChecksum = 0
        for (i in 0..19 step 2) {
            ipChecksum += ((response[i].toInt() and 0xFF) shl 8) or (response[i+1].toInt() and 0xFF)
        }
        ipChecksum = (ipChecksum shr 16) + (ipChecksum and 0xFFFF)
        ipChecksum += (ipChecksum shr 16)
        ipChecksum = ipChecksum.inv() and 0xFFFF
        response[10] = (ipChecksum shr 8).toByte()
        response[11] = (ipChecksum and 0xFF).toByte()
        
        // --- UDP Header ---
        val udpLen = 8 + dnsPayload.size
        response[20] = 0x00 // Src Port 53
        response[21] = 53
        response[22] = (originalSrcPort shr 8).toByte() // Dst Port
        response[23] = (originalSrcPort and 0xFF).toByte()
        response[24] = (udpLen shr 8).toByte()
        response[25] = (udpLen and 0xFF).toByte()
        response[26] = 0x00 // Checksum (optional in IPv4, set 0)
        response[27] = 0x00
        
        // --- Payload ---
        System.arraycopy(dnsPayload, 0, response, 28, dnsPayload.size)
        
        try {
            out.write(response)
        } catch (e: Exception) {
            Log.e("VPN", "Error writing back to TUN", e)
        }
    }

    private fun extractDnsQueryName(payload: ByteArray): String {
        try {
            var i = 12 // Skip DNS header
            val sb = StringBuilder()
            while (i < payload.size) {
                val len = payload[i].toInt() and 0xFF
                if (len == 0) break
                if ((len and 0xC0) == 0xC0) break // Pointer, ignore
                i++
                sb.append(String(payload.copyOfRange(i, i + len)))
                sb.append(".")
                i += len
            }
            return if (sb.isNotEmpty()) sb.dropLast(1).toString() else ""
        } catch (e: Exception) {
            return ""
        }
    }

    private fun createNxDomainResponse(query: ByteArray): ByteArray {
        val resp = query.copyOf()
        resp[2] = (resp[2].toInt() or 0x80).toByte() // Response flag (QR=1)
        resp[3] = (resp[3].toInt() or 0x03).toByte() // RCODE = 3 (NXDOMAIN)
        return resp
    }

    private fun stopVpn() {
        isRunning = false
        scope.coroutineContext.cancelChildren()
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e("LocalVpnService", "Failed to close VPN interface", e)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }

    companion object {
        const val ACTION_START_VPN = "com.example.securityapp.START_VPN"
        const val ACTION_STOP_VPN = "com.example.securityapp.STOP_VPN"
        private const val CHANNEL_ID = "vpn_channel"
        private const val NOTIFICATION_ID = 1
    }
}
