package dev.kwasi.echoservercomplete.network

import android.util.Log
import dev.kwasi.echoservercomplete.adapters.AttendeeListAdapter
import dev.kwasi.echoservercomplete.models.ContentModel
import dev.kwasi.echoservercomplete.models.SocketModel
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.Exception
import kotlin.concurrent.thread

/// The [Server] class has all the functionality that is responsible for the 'server' connection.
/// This is implemented using TCP. This Server class is intended to be run on the GO.

class Server(private val iFaceImpl:NetworkMessageInterface, attendees: AttendeeListAdapter) {
    companion object {
        const val PORT: Int = 9999

    }
    var ip: String = "192.168.49.1"
    private val svrSocket: ServerSocket = ServerSocket(PORT, 0, InetAddress.getByName(ip))
    private val clientMap: HashMap<String, SocketModel> = HashMap()
    private val attendeeListAdapter: AttendeeListAdapter = attendees

    init {
        thread{
            while(true){try{
                val client = SocketModel(svrSocket.accept())
                //val clientAddress: String = client.socket.inetAddress.hostAddress!!
                handleClient(client)
            }catch(e: Exception){}}
        }
    }

    private fun handleClient(client: SocketModel){thread{
        var clientStudentID: String = ""
        try{
            //serverside handshake start
            if(client.read()!="I am here")
                throw Exception("Client Handshake Failed");
            client.send(client.cipher.makeNonce())
            if( !client.cipher.verify(client.read()) )
                throw Exception("Client Handshake Failed");
            //serverside handshake stop

            clientStudentID = client.cipher.getStudentID()!!
            clientMap.set(clientStudentID,client) //added to the map after handshake complete
            updateAttendeeList()
            Log.e("SERVER", "The server has accepted a connection")
            while(client.socket.isConnected){
                iFaceImpl.onContent( client.readMessage(true) )
            }
        }
        catch (e: Exception){
            if(client.socket.isConnected) client.socket.close();
            clientMap.remove(clientStudentID)
            updateAttendeeList()
            Log.e("SERVER", "An error occurred while handling a client")
            e.printStackTrace()
        }
    }}

    private fun updateAttendeeList(){
        val students: MutableList<String> = mutableListOf()
        Log.e("SERVER","students in hashmap "+clientMap.size)
        for(key in clientMap.keys){
            students.add(key)
            Log.e("SERVER","student id of $key")
        }
        attendeeListAdapter.updateAttendeesList(students)
    }

    fun sendMessage(clientStudentID: String, text: String){
        thread{
            val content: ContentModel = ContentModel(text,ip)
            clientMap.get(clientStudentID)?.sendMessage(content,true)
        }
    }
    fun sendMessage(clientStudentID: String, content: ContentModel){
        thread{
            clientMap.get(clientStudentID)?.sendMessage(content,true)
        }
    }

    fun close(){
        svrSocket.close()
        clientMap.clear()
    }

}