package dev.kwasi.echoservercomplete.adapters

import android.net.wifi.p2p.WifiP2pDevice

interface PeerListAdapterInterface {
    fun onPeerClicked(peer:WifiP2pDevice)
}