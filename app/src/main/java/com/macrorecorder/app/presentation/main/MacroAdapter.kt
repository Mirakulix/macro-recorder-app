package com.macrorecorder.app.presentation.main

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.macrorecorder.app.databinding.ItemMacroBinding
import com.macrorecorder.app.domain.model.Macro
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MacroAdapter(
    private val onEdit:   (Macro) -> Unit,
    private val onPlay:   (Macro) -> Unit,
    private val onDelete: (Macro) -> Unit
) : ListAdapter<Macro, MacroAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Macro>() {
            override fun areItemsTheSame(old: Macro, new: Macro) = old.id == new.id
            override fun areContentsTheSame(old: Macro, new: Macro) = old == new
        }
    }

    inner class ViewHolder(private val b: ItemMacroBinding) :
        RecyclerView.ViewHolder(b.root) {

        private var thumbnailJob: Job? = null

        fun bind(macro: Macro) {
            b.tvMacroName.text = macro.name
            b.tvDuration.text  = buildSubtitle(macro)
            b.btnEdit.setOnClickListener   { onEdit(macro) }
            b.btnPlay.setOnClickListener   { onPlay(macro) }
            b.btnDelete.setOnClickListener { onDelete(macro) }
            loadThumbnail(macro.thumbnailPath)
        }

        private fun loadThumbnail(path: String?) {
            thumbnailJob?.cancel()
            b.imgThumbnail.setImageBitmap(null)
            if (path == null) return
            thumbnailJob = CoroutineScope(Dispatchers.IO).launch {
                val bmp = BitmapFactory.decodeFile(path)
                if (isActive) withContext(Dispatchers.Main) {
                    b.imgThumbnail.setImageBitmap(bmp)
                }
            }
        }

        fun onRecycled() { thumbnailJob?.cancel() }

        private fun buildSubtitle(macro: Macro): String {
            val totalSeconds = macro.duration / 1_000
            val min = totalSeconds / 60
            val sec = totalSeconds % 60
            val dur = "%d:%02d".format(min, sec)
            return "$dur · ${macro.eventCount} events"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemMacroBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.onRecycled()
    }
}
