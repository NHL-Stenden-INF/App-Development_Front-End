package com.nhlstenden.appdev.features.task.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.camera.core.Logger
import androidx.recyclerview.widget.RecyclerView
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.databinding.FragmentPressMistakesBinding
import com.nhlstenden.appdev.features.task.models.Question
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.JustifyContent

class PressMistakesFragment : BaseTaskFragment() {
    private var _binding: FragmentPressMistakesBinding? = null
    private val binding get() = _binding!!
    private val pressMistakeQuestion: Question.PressMistakeQuestion
        get() = question as? Question.PressMistakeQuestion
            ?: throw IllegalStateException("Question must be of type PressMistakeQuestion")
    private val selectedPositions = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        if (question !is Question.PressMistakeQuestion)
            throw IllegalArgumentException("Question must be of type PressMistakeQuestion")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPressMistakesBinding.inflate(inflater, container, false)

        val words = pressMistakeQuestion.displayedText.trim().split(" ")
        val adapter = WordAdapter(words, selectedPositions, getMistakeCount())

        binding.recyclerView.adapter = adapter
        val layoutManager = FlexboxLayoutManager(requireContext()).apply {
            flexWrap = FlexWrap.WRAP
            justifyContent = JustifyContent.FLEX_START
        }
        binding.recyclerView.layoutManager = layoutManager

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun setupViews() {
        binding.questionText.text = pressMistakeQuestion.question
        binding.mistakesCounter.text = this.getMistakeCount().toString()

    }

    override fun bindQuestion() {

    }

    fun getMistakeCount(): Int {
        return pressMistakeQuestion.mistakes.size
    }

    class WordAdapter(
        private val words: List<String>,
        private val selectedPositions: MutableSet<Int>,
        private val maxSelectedWords: Int,
    ) : RecyclerView.Adapter<WordAdapter.WordViewHolder>() {
        inner class WordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(R.id.wordTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_word, parent, false)
            return WordViewHolder(view)
        }

        override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
            val word = words[position]

            holder.textView.text = word

            if (selectedPositions.contains(position)){
                holder.textView.setTextColor(0xFF1E88E5.toInt())
            }
            else {
                holder.textView.setTextColor(0xFF000000.toInt())
            }

            holder.textView.setOnClickListener {
                if (selectedPositions.contains(position)) {
                    selectedPositions.remove(position)
                } else if (selectedPositions.size < maxSelectedWords) {
                    selectedPositions.add(position)
                }

                notifyItemChanged(position)
            }
        }

        override fun getItemCount() = words.size
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(question: Question): PressMistakesFragment {
            return PressMistakesFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_QUESTION, question)
                }
            }
        }
    }
}