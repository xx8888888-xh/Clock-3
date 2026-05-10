package com.clock3.pet.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.clock3.pet.R
import com.clock3.pet.data.model.Alarm

class AlarmAdapter(
    private val alarms: List<Alarm>,
    private val activity: MainActivity,
    private val onAction: (Alarm, String) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeText: TextView = itemView.findViewById(R.id.alarmTime)
        val labelText: TextView = itemView.findViewById(R.id.alarmLabel)
        val contentText: TextView = itemView.findViewById(R.id.alarmContent)
        val enabledSwitch: Switch = itemView.findViewById(R.id.alarmEnabled)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarms[position]
        holder.timeText.text = alarm.time
        holder.labelText.text = alarm.label
        holder.contentText.text = alarm.content.ifEmpty { "无内容" }
        holder.enabledSwitch.isChecked = alarm.enabled

        holder.enabledSwitch.setOnCheckedChangeListener { _, _ ->
            onAction(alarm, "toggle")
        }

        holder.deleteButton.setOnClickListener {
            onAction(alarm, "delete")
        }
    }

    override fun getItemCount(): Int = alarms.size
}
