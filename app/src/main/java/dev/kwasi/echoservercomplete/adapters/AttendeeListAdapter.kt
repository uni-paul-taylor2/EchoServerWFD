package dev.kwasi.echoservercomplete.adapters

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract.Attendees
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.kwasi.echoservercomplete.R
import dev.kwasi.echoservercomplete.adapters.PeerListAdapter.ViewHolder
import dev.kwasi.echoservercomplete.models.ContentModel
import kotlin.concurrent.thread

class AttendeeListAdapter(private val attendeeInterface: AttendeeListAdapterInterface): RecyclerView.Adapter<AttendeeListAdapter.ViewHolder>(){
    private val attendees:MutableList<String> = mutableListOf()

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val attendeeTextView: TextView = itemView.findViewById(R.id.attendeeTextView)
        val attendeeButton: Button = itemView.findViewById(R.id.attendeeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.attendee_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int){
        val attendee = attendees[position]
        holder.attendeeTextView.text = attendee
        holder.attendeeButton.setOnClickListener {
            attendeeInterface.onNewStudent(attendee)
        }
    }

    override fun getItemCount(): Int {
        return attendees.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateAttendeesList(newAttendees: MutableList<String>){
        attendees.clear()
        attendees.addAll(newAttendees)
        Log.e("AttendeeListAdapter","size of items: "+newAttendees.size)
        Handler(Looper.getMainLooper()).post {notifyDataSetChanged()} //chatgpt inspired code, wow, we reached here
    }
}