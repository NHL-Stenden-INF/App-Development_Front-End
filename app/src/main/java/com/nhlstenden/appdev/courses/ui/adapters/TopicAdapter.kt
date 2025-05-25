package com.nhlstenden.appdev.courses.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.courses.domain.models.Topic

class TopicAdapter(
    private val onTopicClick: (Topic) -> Unit
) : ListAdapter<Topic, TopicAdapter.TopicViewHolder>(TopicDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_topic, parent, false)
        return TopicViewHolder(view, onTopicClick)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TopicViewHolder(
        itemView: View,
        private val onTopicClick: (Topic) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.topicTitle)
        private val descriptionText: TextView = itemView.findViewById(R.id.topicDescription)
        private val difficultyText: TextView = itemView.findViewById(R.id.difficultyLevel)
        private val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.progressBar)

        fun bind(topic: Topic) {
            titleText.text = topic.title
            descriptionText.text = topic.description
            difficultyText.text = topic.difficulty
            progressBar.progress = topic.progress
            
            itemView.setOnClickListener {
                onTopicClick(topic)
            }
        }
    }

    private class TopicDiffCallback : DiffUtil.ItemCallback<Topic>() {
        override fun areItemsTheSame(oldItem: Topic, newItem: Topic): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Topic, newItem: Topic): Boolean {
            return oldItem == newItem
        }
    }
} 