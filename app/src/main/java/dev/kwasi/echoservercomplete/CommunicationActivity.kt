package dev.kwasi.echoservercomplete

import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.kwasi.echoservercomplete.adapters.ChatListAdapter
import dev.kwasi.echoservercomplete.models.ContentModel
import dev.kwasi.echoservercomplete.adapters.PeerListAdapter
import dev.kwasi.echoservercomplete.adapters.PeerListAdapterInterface
import dev.kwasi.echoservercomplete.adapters.AttendeeListAdapter
import dev.kwasi.echoservercomplete.adapters.AttendeeListAdapterInterface
import dev.kwasi.echoservercomplete.network.ApplicationCipher
import dev.kwasi.echoservercomplete.network.Client
import dev.kwasi.echoservercomplete.network.NetworkMessageInterface
import dev.kwasi.echoservercomplete.network.Server
import dev.kwasi.echoservercomplete.network.WifiDirectInterface
import dev.kwasi.echoservercomplete.network.WifiDirectManager

//treated as student side
class CommunicationActivity : AppCompatActivity(), WifiDirectInterface, PeerListAdapterInterface,
    NetworkMessageInterface, AttendeeListAdapterInterface {
    private var wfdManager: WifiDirectManager? = null

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }
    private val testCipher: ApplicationCipher = ApplicationCipher()

    private var peerListAdapter: PeerListAdapter? = null
    private var chatListAdapter:ChatListAdapter? = null
    private var attendeeListAdapter: AttendeeListAdapter? = null

    private var wfdAdapterEnabled = false
    private var wfdHasConnection = false
    private var hasDevices = false
    private var server: Server? = null
    private var client: Client? = null
    private var deviceIp: String = ""
    private var student_id: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_communication)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val manager: WifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = manager.initialize(this, mainLooper, null)
        wfdManager = WifiDirectManager(manager, channel, this)

        peerListAdapter = PeerListAdapter(this)
        val rvPeerList: RecyclerView= findViewById(R.id.rvPeerListing)
        rvPeerList.adapter = peerListAdapter
        rvPeerList.layoutManager = LinearLayoutManager(this)

        chatListAdapter = ChatListAdapter(true) //studentcode
        val rvChatList: RecyclerView = findViewById(R.id.rvStudentChat) //studentcode
        //chatListAdapter = ChatListAdapter(false) //servercode
        //val rvChatList: RecyclerView = findViewById(R.id.rvServerChat) //servercode
        rvChatList.adapter = chatListAdapter
        rvChatList.layoutManager = LinearLayoutManager(this)

        attendeeListAdapter = AttendeeListAdapter(this)
        val attendees: RecyclerView = findViewById(R.id.attendees)
        attendees.adapter = attendeeListAdapter
        attendees.layoutManager = LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()
        wfdManager?.also {
            registerReceiver(it, intentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        wfdManager?.also {
            unregisterReceiver(it)
        }
    }

    fun createGroup(view: View) {
        wfdManager?.createGroup()
    }

    fun endGroup(view: View){
        wfdManager?.disconnect()
        wfdHasConnection=false
        wfdAdapterEnabled=true
    }

    fun discoverNearbyPeers(view: View) {
        val studentID: String = findViewById<EditText>(R.id.studentID).text.toString()
        if(setStudentID(studentID))
            wfdManager?.discoverPeers();
    }

    fun setStudentID(id: String): Boolean{
        if(testCipher.setStudentID(id)){
            student_id = id
            return true
        }
        val toast: Toast = Toast.makeText(this,"Given Student ID is NOT valid",Toast.LENGTH_SHORT)
        toast.show()
        return false
    }

    private fun updateUI(){
        //make them all invisible at first
        findViewById<ConstraintLayout>(R.id.clWfdAdapterDisabled).visibility = View.GONE
        findViewById<ConstraintLayout>(R.id.studentOnboarding).visibility = View.GONE
        findViewById<ConstraintLayout>(R.id.serverOnboarding).visibility = View.GONE
        findViewById<ConstraintLayout>(R.id.studentChat).visibility = View.GONE
        findViewById<ConstraintLayout>(R.id.serverChat).visibility = View.GONE
        //The rules for updating the UI are as follows:
        // IF the WFD adapter is NOT enabled then
        //      Show UI that says turn on the wifi adapter
        // ELSE IF there is NO WFD connection then i need to show a view that allows the user to either
        // 1) create a group with them as the group owner OR
        // 2) discover nearby groups
        // ELSE IF there are nearby groups found, i need to show them in a list
        // ELSE IF i have a WFD connection i need to show a chat interface where i can send/receive messages
        val wfdAdapterErrorView:ConstraintLayout = findViewById(R.id.clWfdAdapterDisabled)
        wfdAdapterErrorView.visibility = if (!wfdAdapterEnabled) View.VISIBLE else View.GONE

        val wfdNoConnectionView:ConstraintLayout = findViewById(R.id.studentOnboarding) //studentcode
        wfdNoConnectionView.visibility = if (wfdAdapterEnabled && !wfdHasConnection) View.VISIBLE else View.GONE //studentcode
        //val wfdNoConnectionView:ConstraintLayout = findViewById(R.id.serverOnboarding) //servercode
        //wfdNoConnectionView.visibility = if (wfdAdapterEnabled && !wfdHasConnection) View.VISIBLE else View.GONE

        val rvPeerList: RecyclerView= findViewById(R.id.rvPeerListing)
        rvPeerList.visibility = if (wfdAdapterEnabled && !wfdHasConnection && hasDevices) View.VISIBLE else View.GONE

        val wfdConnectedView:ConstraintLayout = findViewById(R.id.studentChat) //studentcode
        wfdConnectedView.visibility = if(wfdHasConnection)View.VISIBLE else View.GONE //studentcode
        //val wfdConnectedView:ConstraintLayout = findViewById(R.id.serverChat) //servercode
        //wfdConnectedView.visibility = if(wfdHasConnection)View.VISIBLE else View.GONE //servercode
    }

    fun clientSendMessage(view: View){
        val elem: EditText = findViewById(R.id.clientETMessage)
        val text: String = elem.text.toString()
        elem.text.clear()
        val content: ContentModel = ContentModel(text,student_id)
        client?.sendMessage(content)
        chatListAdapter?.addItemToEnd(content)
    }
    fun serverSendMessage(view: View){
        val elem: EditText = findViewById(R.id.serverETMessage)
        val text: String = elem.text.toString()
        elem.text.clear()
        val content: ContentModel = ContentModel(text,server?.ip!!)
        server?.sendMessage(student_id,content)
        chatListAdapter?.addItemToEnd(content)
    }

    override fun onWiFiDirectStateChanged(isEnabled: Boolean) {
        wfdAdapterEnabled = isEnabled
        var text = "There was a state change in the WiFi Direct. Currently it is "
        text = if (isEnabled){
            "$text enabled!"
        } else {
            "$text disabled! Try turning on the WiFi adapter"
        }

        val toast = Toast.makeText(this, text, Toast.LENGTH_SHORT)
        toast.show()
        updateUI()
    }

    override fun onPeerListUpdated(deviceList: Collection<WifiP2pDevice>) {
        val toast = Toast.makeText(this, "Updated listing of nearby WiFi Direct devices", Toast.LENGTH_SHORT)
        toast.show()
        hasDevices = deviceList.isNotEmpty()
        peerListAdapter?.updateList(deviceList)
        updateUI()
    }

    override fun onGroupStatusChanged(groupInfo: WifiP2pGroup?, suppressToast: Boolean) {
        val text = if (groupInfo == null){
            "Class failed to form; try toggling (turning off then on) the wifi button"
        } else {
            if(groupInfo.isGroupOwner){"Class has been formed"}
            else{"Class has been joined"}
        }
        val toast = Toast.makeText(this, text , Toast.LENGTH_SHORT)
        if(!suppressToast) toast.show();
        wfdHasConnection = groupInfo != null

        if (groupInfo == null){
            server?.close()
            client?.close()
            server = null
            client = null
        } else if (groupInfo.isGroupOwner && server == null){
            server = Server(this,attendeeListAdapter!!)
            deviceIp = "192.168.49.1"
        } else if (!groupInfo.isGroupOwner && client == null) {
            client = Client(this, student_id)
            deviceIp = client!!.ip
        }
        runOnUiThread {
            val ssid: String = if(groupInfo==null){""}else{groupInfo.networkName}
            val pass: String = if(groupInfo==null){""}else{groupInfo.passphrase}
            findViewById<TextView>(R.id.network_info).text = "Network: $ssid\nPassword: $pass"
        }
        updateUI()
    }

    override fun onDeviceStatusChanged(thisDevice: WifiP2pDevice) {
        val toast = Toast.makeText(this, "Device parameters have been updated" , Toast.LENGTH_SHORT)
        toast.show()
    }

    override fun onPeerClicked(peer: WifiP2pDevice) {
        wfdManager?.connectToPeer(peer)
    }


    override fun onContent(content: ContentModel) {
        runOnUiThread{
            chatListAdapter?.addItemToEnd(content)
        }
    }

    override fun onNewStudent(student: String){
        student_id = student
        //runOnUiThread{
            findViewById<TextView>(R.id.student_chat_header).text = "Student Chat - $student"
        //}
    }

}