package dev.kwasi.echoservercomplete.network

import android.util.Log
import dev.kwasi.echoservercomplete.models.ContentModel
import dev.kwasi.echoservercomplete.models.SocketModel
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.Exception
import kotlin.concurrent.thread

/// The [Server] class has all the functionality that is responsible for the 'server' connection.
/// This is implemented using TCP. This Server class is intended to be run on the GO.

class Server(private val iFaceImpl:NetworkMessageInterface) {
    companion object {
        const val PORT: Int = 9999

    }
    var ip: String = "192.168.49.1"
    private val svrSocket: ServerSocket = ServerSocket(PORT, 0, InetAddress.getByName(ip))
    private val clientMap: HashMap<String, SocketModel> = HashMap()

    init {
        thread{
            while(true){
                val client = SocketModel(svrSocket.accept())
                //val clientAddress: String = client.socket.inetAddress.hostAddress!!
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
                    Log.e("SERVER", "The server has accepted a connection")
                    while(client.socket.isConnected){
                        iFaceImpl.onContent( client.readMessage(true) )
                    }
                }
                catch (e: Exception){
                    if(client.socket.isConnected) client.socket.close();
                    clientMap.remove(clientStudentID)
                    Log.e("SERVER", "An error occurred while handling a client")
                    e.printStackTrace()
                }
            }
        }
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