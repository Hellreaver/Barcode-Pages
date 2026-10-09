package com.hellreaver.barcodepages

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.util.UUID

/**
 * Sends ZPL to a Zebra printer over Bluetooth Classic, Serial Port Profile: the printer treats
 * the link like a serial cable, so ZPL goes over as plain bytes. No Zebra SDK.
 *
 * Blocking; call it off the main thread. Needs BLUETOOTH_CONNECT on Android 12+.
 */
object BluetoothPrinter {
    private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private const val BOND_TIMEOUT_MS = 60_000L

    /**
     * Pairs if needed, connects, optionally asks the printer its label size (falling back to
     * the 576 x 603 stock), then sends what [build] makes for that size.
     */
    fun send(
        device: BluetoothDevice,
        log: (String) -> Unit,
        askSize: Boolean,
        build: (LabelZpl.LabelSize) -> ByteArray,
    ) {
        bond(device, log)
        val socket = open(device, log)
        try {
            val size = (if (askSize) readSize(socket, log) else null) ?: LabelZpl.DEFAULT_LABEL
            val data = build(size)
            socket.outputStream.apply {
                write(data)
                flush()
            }
            log("Sent ${data.size} bytes")
            // Closing right after the write can drop the end of the job before the radio sends it.
            Thread.sleep(1500)
        } finally {
            runCatching { socket.close() }
        }
    }

    /**
     * Pairs with a printer by address. Printers kept out of discovery never show in Android's
     * pairing list, but they answer a phone that knows the address. With numeric comparison,
     * the phone and the printer's screen both show a 6-digit code to confirm.
     */
    @SuppressLint("MissingPermission")
    private fun bond(device: BluetoothDevice, log: (String) -> Unit) {
        if (device.bondState == BluetoothDevice.BOND_BONDED) return
        log("Pairing: confirm the same code on the phone and on the printer's screen")
        if (device.bondState != BluetoothDevice.BOND_BONDING && !device.createBond()) {
            throw IOException("Android refused to start pairing")
        }
        val deadline = System.currentTimeMillis() + BOND_TIMEOUT_MS
        var sawBonding = false
        while (System.currentTimeMillis() < deadline) {
            when (device.bondState) {
                BluetoothDevice.BOND_BONDED -> return log("Paired")
                BluetoothDevice.BOND_BONDING -> sawBonding = true
                BluetoothDevice.BOND_NONE -> if (sawBonding) throw IOException("Pairing was cancelled or failed")
            }
            Thread.sleep(250)
        }
        throw IOException("Pairing didn't finish within ${BOND_TIMEOUT_MS / 1000} s")
    }

    private fun readSize(socket: BluetoothSocket, log: (String) -> Unit): LabelZpl.LabelSize? {
        socket.outputStream.apply {
            write(LabelZpl.SIZE_QUERY.toByteArray(Charsets.US_ASCII))
            flush()
        }
        val input = socket.inputStream
        val reply = StringBuilder()
        val deadline = System.currentTimeMillis() + 3000
        while (System.currentTimeMillis() < deadline) {
            val n = input.available()
            if (n > 0) {
                val buf = ByteArray(n)
                val read = input.read(buf)
                if (read > 0) reply.append(String(buf, 0, read, Charsets.US_ASCII))
                LabelZpl.parseSize(reply.toString())?.let { size ->
                    log("Printer reports ${size.width} x ${size.length} dots")
                    if (size.length < LabelZpl.DEFAULT_LABEL.length * 2 / 3) {
                        log("That's short for the 75 mm labels; if labels come out cut off, use Calibrate")
                    }
                    return size
                }
            } else {
                Thread.sleep(50)
            }
        }
        log("Printer didn't report its size; using 576 x 603")
        return null
    }

    @SuppressLint("MissingPermission")
    private fun open(device: BluetoothDevice, log: (String) -> Unit): BluetoothSocket {
        val secure = device.createRfcommSocketToServiceRecord(SPP)
        try {
            secure.connect()
            log("Connected")
            return secure
        } catch (e: IOException) {
            runCatching { secure.close() }
        }
        val insecure = device.createInsecureRfcommSocketToServiceRecord(SPP)
        try {
            insecure.connect()
        } catch (e: IOException) {
            runCatching { insecure.close() }
            throw e
        }
        log("Connected")
        return insecure
    }
}
