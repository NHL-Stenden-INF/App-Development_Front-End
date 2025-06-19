package com.nhlstenden.appdev.features.task.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nhlstenden.appdev.R

class WordAdapter(
    private var words: List<String>,
    private val selectedPositions: MutableSet<Int>,
    private val correctPositions: Set<Int>,
    private val maxSelectedWords: Int,
) : RecyclerView.Adapter<WordAdapter.WordViewHolder>() {
    inner class WordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.wordTextView)
    }

    private var isCorrect: Boolean? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_word, parent, false)
        return WordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val word = words[position]

        holder.textView.text = word

        if (isCorrect === null){
            if (selectedPositions.contains(position)){
                holder.textView.setTextColor(0xFF1E88E5.toInt())
            }
            else {
                holder.textView.setTextColor(0xFF000000.toInt())
            }
        }
        else {
            if (correctPositions.contains(position)){
                if (selectedPositions.contains(position)){
                    holder.textView.setTextColor(0xFF00FF00.toInt())
                }
                else {
                    holder.textView.setTextColor(0xFFFF0000.toInt())
                }
            }
            else {
                holder.textView.setTextColor(0xFF000000.toInt())
            }
        }


        holder.textView.setOnClickListener {
            if (isCorrect != null) return@setOnClickListener

            if (selectedPositions.contains(position)) {
                selectedPositions.remove(position)
            } else if (selectedPositions.size < maxSelectedWords) {
                selectedPositions.add(position)
            }

            notifyItemChanged(position)
        }
    }

    fun showAnswer(isAnswerCorrect: Boolean) {
        isCorrect = isAnswerCorrect
        notifyDataSetChanged()
    }

    fun reset(){
        selectedPositions.clear()
    }

    override fun getItemCount() = words.size
}