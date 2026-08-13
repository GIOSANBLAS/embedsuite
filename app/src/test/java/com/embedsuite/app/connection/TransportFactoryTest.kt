package com.embedsuite.app.connection

import android.bluetooth.BluetoothManager
import android.content.Context
import com.embedsuite.app.UsbSerialManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransportFactoryTest {

    private val usb = mockk<UsbSerialManager>(relaxed = true)

    @Test
    fun createsUsbTransport() {
        val context = mockk<Context>(relaxed = true)
        val t = TransportFactory.create(TransportType.USB, usb, context)
        assertTrue(t is UsbTransport)
        assertEquals(TransportType.USB, t.type)
    }

    @Test
    fun createsTcpForWifi() {
        val context = mockk<Context>(relaxed = true)
        val t = TransportFactory.create(TransportType.WIFI, usb, context)
        assertTrue(t is TcpTransport)
        assertEquals(TransportType.WIFI, t.type)
    }

    @Test
    fun createsBleTransport() {
        val btManager = mockk<BluetoothManager>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns btManager
        val t = TransportFactory.create(TransportType.BLE, usb, context)
        assertTrue(t is BleTransport)
        assertEquals(TransportType.BLE, t.type)
    }

    @Test
    fun mockWhenRequested() {
        val context = mockk<Context>(relaxed = true)
        val t = TransportFactory.create(TransportType.USB, usb, context, useMock = true)
        assertTrue(t is MockTransport)
    }

    @Test
    fun supportedTypesListsAll() {
        assertEquals(3, TransportFactory.supportedTypes().size)
    }
}
