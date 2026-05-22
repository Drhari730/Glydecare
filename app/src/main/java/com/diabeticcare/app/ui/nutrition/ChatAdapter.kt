package com.diabeticcare.app.ui.nutrition

import android.view.LayoutInflater
import android.view.ViewGroup
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.diabeticcare.app.R
import com.diabeticcare.app.data.model.ChatMessage

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(a: ChatMessage, b: ChatMessage) = a.timestamp == b.timestamp
            override fun areContentsTheSame(a: ChatMessage, b: ChatMessage) = a == b
        }
        private const val VIEW_USER = 0
        private const val VIEW_AI = 1
    }

    override fun getItemViewType(position: Int) = if (getItem(position).isUser) VIEW_USER else VIEW_AI

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_USER) {
            UserVH(inflater.inflate(R.layout.item_chat_user, parent, false))
        } else {
            AiVH(inflater.inflate(R.layout.item_chat_ai, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is UserVH -> holder.tv.text = msg.text
            is AiVH -> {
                holder.tv.text = msg.text
                Linkify.addLinks(holder.tv, Linkify.WEB_URLS)
                holder.tv.movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    class UserVH(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val tv: TextView = v.findViewById(R.id.tv_message)
    }
    class AiVH(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val tv: TextView = v.findViewById(R.id.tv_message)
    }
}
