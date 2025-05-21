package com.nhlstenden.appdev

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhlstenden.appdev.TaskActivity.Question
import okhttp3.internal.notify
import okhttp3.internal.notifyAll

class MultipleChoiceFragment : Fragment() {
    private lateinit var questionText: TextView
    private lateinit var questionList: RecyclerView
    private lateinit var adapter: OptionAdapter

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

        adapter = OptionAdapter(options)
        questionList.layoutManager = LinearLayoutManager(context)
        questionList.adapter = adapter

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_multiple_choice, container, false)

        val confirmButton = view.findViewById<Button>(R.id.confirm_button)

        confirmButton.setOnClickListener({
            val selectedOption = (questionList.adapter as? OptionAdapter)?.getSelectedOption()

            if (!adapter.clickEnabled){
                taskCompleteListener?.onTaskCompleted(question, selectedOption?.isCorrect == true)
            }
            else
            {
                if (selectedOption != null) {
                    confirmButton.text = "Next"
                    adapter.clickEnabled = false
                    adapter.notifyDataSetChanged()
                }
            }
        })

        return view
    }
}

data class Option(
    val text: String,
    val isCorrect: Boolean
)

class OptionAdapter(private val options: List<Option>) :
    RecyclerView.Adapter<OptionAdapter.ViewHolder>() {

    var selectedPosition = RecyclerView.NO_POSITION
    var clickEnabled : Boolean = true;

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
        holder.multipleChoiceButton.text = option.text

        holder.multipleChoiceButton.setOnClickListener {
            if (clickEnabled){
                val previous = selectedPosition
                selectedPosition = position
                notifyItemChanged(previous)
                notifyItemChanged(position)
            }
        }

        holder.multipleChoiceButton.setBackgroundColor(
            if (clickEnabled)
                if (position == selectedPosition)
                    0xFF6200EE.toInt()
                else
                    0xFFD0D0D0.toInt()
            else
                if (option.isCorrect)
                    0xFF388E3C.toInt()
                else
                    if (position == selectedPosition)
                        0xFFB71C1C.toInt()
                    else
                        0xFFD0D0D0.toInt()
        )
    }

    fun getSelectedOption(): Option? {
        return if (selectedPosition != RecyclerView.NO_POSITION) {
            options[selectedPosition]
        } else null
    }

    override fun getItemCount() = options.size
}