package com.nhlstenden.appdev.models

import android.content.Context
import android.util.Log
import com.nhlstenden.appdev.Option
import com.nhlstenden.appdev.TaskActivity.Question
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class QuestionParser(private val context: Context) {
    
    fun loadQuestionsForTopic(topicTitle: String): List<Question> {
        val resourceId = getResourceIdForTopic(topicTitle)
        if (resourceId == 0) {
            Log.e("QuestionParser", "No resource found for topic: $topicTitle")
            return emptyList()
        }
        
        return try {
            val inputStream = context.resources.openRawResource(resourceId)
            parseQuestionsXml(inputStream)
        } catch (e: Exception) {
            Log.e("QuestionParser", "Error loading questions for topic: $topicTitle", e)
            emptyList()
        }
    }
    
    private fun getResourceIdForTopic(topicTitle: String): Int {
        val resourceName = when {
            topicTitle.contains("HTML", ignoreCase = true) -> {
                when {
                    topicTitle.contains("Basic", ignoreCase = true) -> "html_basics_questions"
                    topicTitle.contains("Element", ignoreCase = true) -> "html_elements_questions"
                    topicTitle.contains("Form", ignoreCase = true) -> "html_forms_questions"
                    topicTitle.contains("Structure", ignoreCase = true) -> "html_structure_questions"
                    else -> "html_basics_questions" // Default HTML questions
                }
            }
            topicTitle.contains("CSS", ignoreCase = true) -> {
                when {
                    topicTitle.contains("Basic", ignoreCase = true) -> "css_basics_questions"
                    topicTitle.contains("Layout", ignoreCase = true) -> "css_layout_questions"
                    topicTitle.contains("Selector", ignoreCase = true) -> "css_selectors_questions"
                    topicTitle.contains("Animation", ignoreCase = true) -> "css_animation_questions"
                    else -> "css_basics_questions" // Default CSS questions
                }
            }
            topicTitle.contains("SQL", ignoreCase = true) -> {
                when {
                    topicTitle.contains("Basic", ignoreCase = true) -> "sql_basics_questions"
                    topicTitle.contains("Join", ignoreCase = true) -> "sql_joins_questions"
                    topicTitle.contains("Quer", ignoreCase = true) -> "sql_queries_questions"
                    topicTitle.contains("Database", ignoreCase = true) -> "sql_database_questions"
                    else -> "sql_basics_questions" // Default SQL questions
                }
            }
            else -> "default_questions" // Default fallback
        }
        
        return context.resources.getIdentifier(resourceName, "raw", context.packageName)
    }
    
    private fun parseQuestionsXml(inputStream: InputStream): List<Question> {
        val questions = mutableListOf<Question>()
        
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(inputStream)
            document.documentElement.normalize()
            
            // Parse multiple choice questions
            val mcqNodes = document.getElementsByTagName("multiple_choice_question")
            for (i in 0 until mcqNodes.length) {
                val questionElement = mcqNodes.item(i) as Element
                val questionText = questionElement.getElementsByTagName("question_text").item(0).textContent
                val optionsNodeList = questionElement.getElementsByTagName("option")
                
                val options = parseOptions(optionsNodeList)
                if (options.isNotEmpty()) {
                    // Shuffle options to randomize answer order
                    options.shuffle()
                    questions.add(Question.MultipleChoiceQuestion(questionText, options))
                }
            }
            
            // Parse flip card questions
            val flipCardNodes = document.getElementsByTagName("flip_card_question")
            for (i in 0 until flipCardNodes.length) {
                val questionElement = flipCardNodes.item(i) as Element
                val frontText = questionElement.getElementsByTagName("front_text").item(0).textContent
                val backText = questionElement.getElementsByTagName("back_text").item(0).textContent
                
                questions.add(Question.FlipCardQuestion(Pair(frontText, backText)))
            }
            
            // Parse press mistakes questions
            val pressMistakesNodes = document.getElementsByTagName("press_mistakes_question")
            for (i in 0 until pressMistakesNodes.length) {
                val questionElement = pressMistakesNodes.item(i) as Element
                val questionText = questionElement.getElementsByTagName("question_text").item(0).textContent
                val sentence = questionElement.getElementsByTagName("sentence").item(0).textContent
                
                val mistakesNodeList = questionElement.getElementsByTagName("mistake")
                val mistakes = mutableListOf<String>()
                for (j in 0 until mistakesNodeList.length) {
                    mistakes.add(mistakesNodeList.item(j).textContent)
                }
                
                questions.add(Question.PressMistakesQuestion(questionText, sentence, mistakes))
            }
            
            // Parse edit text questions
            val editTextNodes = document.getElementsByTagName("edit_text_question")
            for (i in 0 until editTextNodes.length) {
                val questionElement = editTextNodes.item(i) as Element
                val questionText = questionElement.getElementsByTagName("question_text").item(0).textContent
                val incorrectText = questionElement.getElementsByTagName("incorrect_text").item(0).textContent
                val correctText = questionElement.getElementsByTagName("correct_text").item(0).textContent
                
                questions.add(Question.EditTextQuestion(questionText, incorrectText, correctText))
            }
            
        } catch (e: Exception) {
            Log.e("QuestionParser", "Error parsing XML", e)
        } finally {
            inputStream.close()
        }
        
        return questions
    }
    
    private fun parseOptions(optionsNodeList: NodeList): MutableList<Option> {
        val options = mutableListOf<Option>()
        for (j in 0 until optionsNodeList.length) {
            val optionElement = optionsNodeList.item(j) as Element
            val optionText = optionElement.textContent
            val isCorrect = optionElement.getAttribute("correct").equals("true", ignoreCase = true)
            options.add(Option(optionText, isCorrect))
        }
        return options
    }
} 