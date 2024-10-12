package dev.kwasi.echoservercomplete.models

import com.google.gson.Gson
import java.net.Socket
import java.io.BufferedReader
import java.io.BufferedWriter
import dev.kwasi.echoservercomplete.network.ApplicationCipher
import dev.kwasi.echoservercomplete.models.ContentModel
import kotlin.concurrent.thread
/// The [ContentModel] class represents data that is transferred between devices when multiple
/// devices communicate with each other.
//data class SocketModel(val reader: BufferedReader, val writer: BufferedWriter, val socket: Socket, val cipher: ApplicationCipher)

class SocketModel(givenSocket: Socket){
    val socket: Socket = givenSocket
    val reader: BufferedReader = socket.inputStream.bufferedReader()
    val writer: BufferedWriter = socket.outputStream.bufferedWriter()
    val cipher: ApplicationCipher = ApplicationCipher()

    fun send(text: String, encrypt: Boolean = false){
        if(!socket.isConnected) throw Exception("Socket is NOT connected");
        var data: String = text
        if(encrypt) data = cipher.encrypt(data);
        writer.write("$data\n")
        writer.flush()
    }
    fun sendMessage(content: ContentModel, encrypt: Boolean = false){
        send( Gson().toJson(content),encrypt )
    }

    fun read(encrypted: Boolean = false): String {
        var data: String = reader.readLine()
        if(encrypted) data = cipher.decrypt(data);
        return data
    }
    fun readMessage(encrypted: Boolean): ContentModel {
        return Gson().fromJson(read(encrypted),ContentModel::class.java)
    }
}