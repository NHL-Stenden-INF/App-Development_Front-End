package com.nhlstenden.appdev

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhlstenden.appdev.TaskActivity.Question

class MultipleChoiceFragment : Fragment() {
    private lateinit var questionText: TextView
    private lateinit var questionList: RecyclerView

    private lateinit var question: Question.MultipleChoiceQuestion

    private var taskCompleteListener: OnTaskCompleteListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnTaskCompleteListener) {
            taskCompleteListener = context
        } else {
            throw RuntimeException("$context must implement OnTaskCompleteListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        taskCompleteListener = null
    }

    companion object {
        fun newInstance(question: Question.MultipleChoiceQuestion): MultipleChoiceFragment {
            val fragment = MultipleChoiceFragment()
            val args = Bundle()
            args.putSerializable("question_data", question)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        question = arguments?.getSerializable("question_data") as Question.MultipleChoiceQuestion

        questionText = view.findViewById<TextView>(R.id.question_text)
        questionList = view.findViewById<RecyclerView>(R.id.question_list)

        questionText.text = question.question
        val options = question.options

        questionList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = OptionAdapter(options)
        }

    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_multiple_choice, container, false)

        val confirmButton = view.findViewById<Button>(R.id.confirm_button)

        confirmButton.setOnClickListener(

            {taskCompleteListener?.onTaskCompleted(true)}
        )

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