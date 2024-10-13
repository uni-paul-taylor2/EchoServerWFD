package dev.kwasi.echoservercomplete.network

import android.util.Log
import com.google.gson.Gson
import dev.kwasi.echoservercomplete.models.ContentModel
import dev.kwasi.echoservercomplete.models.SocketModel
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.Socket
import kotlin.concurrent.thread
import dev.kwasi.echoservercomplete.network.ApplicationCipher

class Client (private val networkMessageInterface: NetworkMessageInterface, student_id: String){
    private lateinit var client: SocketModel
    private var groupOwnerIP: String = "192.168.49.1"
    private var serverPort: Int = 9999
    var ip:String = ""

    init {
        thread {
            client = SocketModel( Socket(groupOwnerIP, serverPort) )
            ip = client.socket.inetAddress.hostAddress!!
            client.cipher.setStudentID(student_id)
            try {
                //clientside handshake start
                client.send("I am here")
                val nonce: String = client.read()
                client.send(client.cipher.sign(nonce)) //enc(hash(student_id), nonce)
                //clientside handshake stop

                while (client.socket.isConnected) {
                    networkMessageInterface.onContent( client.readMessage(true) )
                }
            }
            catch(e: Exception){
                Log.e("CLIENT", "An error has occurred in the client")
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(text: String){
        thread{
            client.sendMessage(ContentModel(text,groupOwnerIP),true)
        }
    }
    fun sendMessage(content: ContentModel){
        thread{
            client.sendMessage(content,true)
        }
    }

    fun close(){
        client.socket.close()
    }
}