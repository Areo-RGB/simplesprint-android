package com.paul.simplesprint.core.services

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NsdServiceCoordinatorTest {
    private val serviceType = "_simplesprint._tcp."

    @Test
    fun `startHosting reports success after service registration`() {
        val backend = FakeNsdBackend()
        val coordinator = NsdServiceCoordinator(backend = backend, serviceType = serviceType)
        var success = false

        coordinator.startHosting(port = 9000) { result ->
            success = result.isSuccess
        }
        assertNotNull(backend.registrationListener)
        backend.registrationListener?.onServiceRegistered(
            NsdServiceInfo().apply {
                serviceName = "SimpleSprintHost"
                this.serviceType = this@NsdServiceCoordinatorTest.serviceType
                setPort(9000)
            },
        )

        assertTrue(success)
    }

    @Test
    fun `startDiscovery resolves host and deduplicates repeated records`() {
        val backend = FakeNsdBackend()
        val coordinator = NsdServiceCoordinator(backend = backend, serviceType = serviceType)
        val found = mutableListOf<NsdServiceCoordinator.DiscoveredHost>()
        var started = false

        coordinator.startDiscovery(
            onHostResolved = { found += it },
            onComplete = { result -> started = result.isSuccess },
        )
        backend.discoveryListener?.onDiscoveryStarted(serviceType)
        assertTrue(started)

        val resolved = NsdServiceInfo().apply {
            serviceName = "tablet-host"
            this.serviceType = this@NsdServiceCoordinatorTest.serviceType
            setPort(9000)
            host = InetAddress.getByName("192.168.43.1")
        }

        backend.triggerServiceFound(resolved)
        backend.triggerServiceFound(resolved)

        assertEquals(1, found.size)
        assertEquals("192.168.43.1", found.first().endpointId)
    }

    @Test
    fun `reset stops discovery and unregisters hosting`() {
        val backend = FakeNsdBackend()
        val coordinator = NsdServiceCoordinator(backend = backend, serviceType = serviceType)

        coordinator.startHosting(port = 9000) { }
        backend.registrationListener?.onServiceRegistered(
            NsdServiceInfo().apply {
                serviceName = "SimpleSprintHost"
                this.serviceType = this@NsdServiceCoordinatorTest.serviceType
                setPort(9000)
            },
        )
        coordinator.startDiscovery(onHostResolved = { }, onComplete = { })
        backend.discoveryListener?.onDiscoveryStarted(serviceType)

        coordinator.reset()

        assertTrue(backend.stopDiscoveryCalled)
        assertTrue(backend.unregisterCalled)
    }
}

private class FakeNsdBackend : NsdBackend {
    var registrationListener: NsdManager.RegistrationListener? = null
    var discoveryListener: NsdManager.DiscoveryListener? = null
    var resolveListener: NsdManager.ResolveListener? = null
    var stopDiscoveryCalled: Boolean = false
    var unregisterCalled: Boolean = false

    override fun registerService(serviceInfo: NsdServiceInfo, listener: NsdManager.RegistrationListener) {
        registrationListener = listener
    }

    override fun unregisterService(listener: NsdManager.RegistrationListener) {
        unregisterCalled = true
        if (registrationListener == listener) {
            registrationListener = null
        }
    }

    override fun discoverServices(serviceType: String, listener: NsdManager.DiscoveryListener) {
        discoveryListener = listener
    }

    override fun stopServiceDiscovery(listener: NsdManager.DiscoveryListener) {
        stopDiscoveryCalled = true
        if (discoveryListener == listener) {
            discoveryListener = null
        }
    }

    override fun resolveService(serviceInfo: NsdServiceInfo, listener: NsdManager.ResolveListener) {
        resolveListener = listener
        listener.onServiceResolved(serviceInfo)
    }

    fun triggerServiceFound(serviceInfo: NsdServiceInfo) {
        discoveryListener?.onServiceFound(serviceInfo)
    }
}
