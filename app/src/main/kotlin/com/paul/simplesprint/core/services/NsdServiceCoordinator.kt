package com.paul.simplesprint.core.services

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import java.util.concurrent.ConcurrentHashMap

internal interface NsdBackend {
    fun registerService(serviceInfo: NsdServiceInfo, listener: NsdManager.RegistrationListener)
    fun unregisterService(listener: NsdManager.RegistrationListener)
    fun discoverServices(serviceType: String, listener: NsdManager.DiscoveryListener)
    fun stopServiceDiscovery(listener: NsdManager.DiscoveryListener)
    fun resolveService(serviceInfo: NsdServiceInfo, listener: NsdManager.ResolveListener)
}

internal class AndroidNsdBackend(
    context: Context,
) : NsdBackend {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    override fun registerService(serviceInfo: NsdServiceInfo, listener: NsdManager.RegistrationListener) {
        val manager = requireNotNull(nsdManager) { "NSD manager unavailable" }
        manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    override fun unregisterService(listener: NsdManager.RegistrationListener) {
        val manager = nsdManager ?: return
        runCatching { manager.unregisterService(listener) }
    }

    override fun discoverServices(serviceType: String, listener: NsdManager.DiscoveryListener) {
        val manager = requireNotNull(nsdManager) { "NSD manager unavailable" }
        manager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    override fun stopServiceDiscovery(listener: NsdManager.DiscoveryListener) {
        val manager = nsdManager ?: return
        runCatching { manager.stopServiceDiscovery(listener) }
    }

    override fun resolveService(serviceInfo: NsdServiceInfo, listener: NsdManager.ResolveListener) {
        val manager = requireNotNull(nsdManager) { "NSD manager unavailable" }
        manager.resolveService(serviceInfo, listener)
    }
}

internal class NsdServiceCoordinator(
    private val backend: NsdBackend,
    private val serviceType: String,
    private val serviceNamePrefix: String = "SimpleSprintHost",
) {
    data class DiscoveredHost(
        val endpointId: String,
        val endpointName: String,
        val hostAddress: String,
        val port: Int,
    )

    @Volatile
    private var registrationListener: NsdManager.RegistrationListener? = null

    @Volatile
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    @Volatile
    private var discoveredHostCallback: ((DiscoveredHost) -> Unit)? = null

    @Volatile
    private var advertisedServiceName: String? = null

    private val discoveredHostKeys = ConcurrentHashMap.newKeySet<String>()

    fun startHosting(port: Int, onComplete: (Result<Unit>) -> Unit) {
        stopHosting()
        val listener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                registrationListener = null
                onComplete(Result.failure(IllegalStateException("nsd register failed: $errorCode")))
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                registrationListener = null
            }

            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                advertisedServiceName = serviceInfo.serviceName
                onComplete(Result.success(Unit))
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                advertisedServiceName = null
            }
        }

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = serviceNamePrefix
            this.serviceType = this@NsdServiceCoordinator.serviceType
            setPort(port)
        }

        registrationListener = listener
        try {
            backend.registerService(serviceInfo, listener)
        } catch (error: Throwable) {
            registrationListener = null
            onComplete(Result.failure(error))
        }
    }

    fun stopHosting() {
        val listener = registrationListener ?: return
        registrationListener = null
        advertisedServiceName = null
        backend.unregisterService(listener)
    }

    fun startDiscovery(onHostResolved: (DiscoveredHost) -> Unit, onComplete: (Result<Unit>) -> Unit) {
        stopDiscovery()
        discoveredHostCallback = onHostResolved
        discoveredHostKeys.clear()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                backend.stopServiceDiscovery(this)
                discoveryListener = null
                onComplete(Result.failure(IllegalStateException("nsd discovery failed: $errorCode")))
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                backend.stopServiceDiscovery(this)
                discoveryListener = null
            }

            override fun onDiscoveryStarted(serviceType: String) {
                onComplete(Result.success(Unit))
            }

            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != this@NsdServiceCoordinator.serviceType) {
                    return
                }
                if (serviceInfo.serviceName == advertisedServiceName) {
                    return
                }
                resolveDiscoveredService(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
        }

        discoveryListener = listener
        try {
            backend.discoverServices(serviceType, listener)
        } catch (error: Throwable) {
            discoveryListener = null
            onComplete(Result.failure(error))
        }
    }

    fun stopDiscovery() {
        val listener = discoveryListener ?: return
        discoveryListener = null
        discoveredHostCallback = null
        discoveredHostKeys.clear()
        backend.stopServiceDiscovery(listener)
    }

    fun reset() {
        stopDiscovery()
        stopHosting()
    }

    private fun resolveDiscoveredService(serviceInfo: NsdServiceInfo) {
        try {
            backend.resolveService(
                serviceInfo,
                object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val hostAddress = resolvedHostAddress(serviceInfo) ?: return
                        val port = serviceInfo.port
                        if (port <= 0) {
                            return
                        }
                        val key = "$hostAddress:$port"
                        if (!discoveredHostKeys.add(key)) {
                            return
                        }
                        discoveredHostCallback?.invoke(
                            DiscoveredHost(
                                endpointId = hostAddress,
                                endpointName = serviceInfo.serviceName ?: hostAddress,
                                hostAddress = hostAddress,
                                port = port,
                            ),
                        )
                    }
                },
            )
        } catch (_: Throwable) {
            // Discovery should continue even when one resolve fails.
        }
    }
}

internal fun resolvedHostAddress(serviceInfo: NsdServiceInfo): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val fromList = serviceInfo.hostAddresses
            ?.firstOrNull()
            ?.hostAddress
            ?.trim()
            .orEmpty()
        if (fromList.isNotEmpty()) {
            return fromList
        }
    }
    val fromHost = serviceInfo.host
        ?.hostAddress
        ?.trim()
        .orEmpty()
    return fromHost.ifEmpty { null }
}
