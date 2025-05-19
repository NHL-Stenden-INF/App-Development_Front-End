package com.nhlstenden.appdev

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MultipleChoiceFragment : Fragment() {
    private lateinit var questionText: TextView
    private lateinit var questionList: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_multiple_choice, container, false)

        // Initialize views
        var questionText = view.findViewById<TextView>(R.id.question_text)
        var questionList = view.findViewById<RecyclerView>(R.id.question_list)

        questionText.text = "Question"

        val options = listOf<Option>(
            Option("Hyper-Text Markup Language", true),
            Option("Heiko, Theo, Mayo and Leo", false),
            Option("High-Transfer Marking Language", false),
            Option("High-Temperature Machine Learning", false)
        )

        questionList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = OptionAdapter(options)
        }

        return view
    }
}

data class Option(
    val text: String,
    val isCorrect: Boolean
)

class OptionAdapter(private val options: List<Option>) :
    RecyclerView.Adapter<OptionAdapter.ViewHolder>(){

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val multipleChoiceButton: Button = view.findViewById(R.id.multiple_choice_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_multiple_choice_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = options[position]
        holder.apply {
            multipleChoiceButton.text = option.text
        }
    }

    override fun getItemCount() = options.size
}