package dev.kwasi.echoservercomplete.network


import dev.kwasi.echoservercomplete.models.ContentModel
import java.security.MessageDigest
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.Cipher
import kotlin.io.encoding.Base64
import java.util.UUID

fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }
fun getFirstNChars(str: String, n:Int) = str.substring(0,n)

class ApplicationCipher (){ //do note that AES_ENC(student_id,data) encrypts with hash(student_id) :D
    private val student_ids: Array<String> = arrayOf("816033518","816035169","816117992") //list of all ids
    private var student_id: String? = null
    private var nonce: String? = null
    fun hash(content: ContentModel): ContentModel{
        val hashedMessage: String =
            MessageDigest.getInstance("SHA-256")
            .digest(content.message.toByteArray()).toHex()
        return ContentModel(hashedMessage, content.student_id)
    }
    fun hash(content: String): String{
        return MessageDigest.getInstance("SHA-256")
            .digest(content.toByteArray()).toHex()
    }

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    fun AES_ENC(password: String, plaintext: String): String{
        val seed: String = getFirstNChars(this.hash(password),16)
        val key: SecretKeySpec = SecretKeySpec(seed.toByteArray(),"AES")
        val iv: IvParameterSpec = IvParameterSpec(seed.toByteArray())
        val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        return Base64.Default.encode(
            cipher.doFinal(plaintext.toByteArray())
        )

    }

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    fun AES_DEC(password: String, ciphertext: String): String{
        val seed: String = getFirstNChars(this.hash(password),16)
        val key: SecretKeySpec = SecretKeySpec(seed.toByteArray(),"AES")
        val iv: IvParameterSpec = IvParameterSpec(seed.toByteArray())
        val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        return String(cipher.doFinal(
            Base64.Default.decode(ciphertext)
        ))
    }

    fun setStudentID(id: String): Boolean{
        var exists: Boolean = id.length == 9 && id.startsWith("81")
        /*for(ID in student_ids){
            if(ID==id){
                exists = true
                break
            }
        }*/
        if(!exists) return false;
        student_id = id
        return true
    }
    fun getStudentID(): String?{
        return student_id
    }

    fun makeNonce(): String {
        nonce = UUID.randomUUID().toString()
        return nonce!!
    }

    fun sign(uuid: String): String{
        return AES_ENC(student_id!!,uuid)
    }

    fun verify(text: String): Boolean{
        for(id in student_ids){
            if(AES_DEC(id,text)==nonce){
                setStudentID(id)
                return true
            }
        }
        return false
    }

    fun encrypt(text: String): String {
        return AES_ENC(student_id!!,text)
    }
    fun encrypt(text: ContentModel): ContentModel {
        return ContentModel(
            AES_ENC(student_id!!,text.message),
            text.student_id
        )
    }

    fun decrypt(text: String): String {
        return AES_DEC(student_id!!,text)
    }
    fun decrypt(text: ContentModel): ContentModel {
        return ContentModel(
            AES_DEC(student_id!!,text.message),
            text.student_id
        )
    }
}