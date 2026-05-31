package com.clock3.pet.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.clock3.pet.R
import com.clock3.pet.data.model.Alarm

class AlarmAdapter(
    private var alarms: List<Alarm>,
    private val onAction: (Alarm, String) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeText: TextView = itemView.findViewById(R.id.alarmTime)
        val labelText: TextView = itemView.findViewById(R.id.alarmLabel)
        val contentText: TextView = itemView.findViewById(R.id.alarmContent)
        val enabledSwitch: SwitchCompat = itemView.findViewById(R.id.alarmEnabled)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
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
        holder.contentText.text = alarm.content.ifEmpty {
            holder.itemView.context.getString(R.string.alarm_no_content)
        }

        holder.enabledSwitch.setOnCheckedChangeListener(null)
        holder.enabledSwitch.isChecked = alarm.enabled
        holder.enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            @Suppress("DEPRECATION")
            val adapterPos = holder.adapterPosition
            val currentAlarm = alarms.getOrNull(adapterPos) ?: return@setOnCheckedChangeListener
            if (currentAlarm.enabled != isChecked) {
                onAction(currentAlarm, "toggle")
            }
        }

        holder.deleteButton.setOnClickListener {
            @Suppress("DEPRECATION")
            val adapterPos = holder.adapterPosition
            val currentAlarm = alarms.getOrNull(adapterPos) ?: return@setOnClickListener
            onAction(currentAlarm, "delete")
        }
    }

    override fun getItemCount(): Int = alarms.size

    fun updateAlarms(newAlarms: List<Alarm>) {
        alarms = newAlarms
        notifyDataSetChanged()
    }
}
