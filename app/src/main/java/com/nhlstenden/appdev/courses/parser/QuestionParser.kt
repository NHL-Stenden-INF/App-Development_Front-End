package com.nhlstenden.appdev.courses.parser

import android.content.Context
import android.util.Log
import com.nhlstenden.appdev.task.domain.models.Option
import com.nhlstenden.appdev.task.domain.models.Question
import com.nhlstenden.appdev.task.domain.models.QuestionType
import org.json.JSONArray
import org.json.JSONObject
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
            parseQuestionsJson(inputStream)
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
    
    private fun parseQuestionsJson(inputStream: InputStream): List<Question> {
        val questions = mutableListOf<Question>()
        
        try {
            val jsonArray = JSONArray(inputStream.bufferedReader().use { it.readText() })
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                questions.add(parseQuestion(jsonObject))
            }
        } catch (e: Exception) {
            Log.e("QuestionParser", "Error parsing JSON", e)
        } finally {
            inputStream.close()
        }
        
        return questions
    }
    
    fun parseQuestion(jsonObject: JSONObject): Question {
        val id = jsonObject.getString("id")
        val type = QuestionType.valueOf(jsonObject.getString("type"))
        val text = jsonObject.getString("text")
        val options = parseOptions(jsonObject.optJSONArray("options"))
        val correctOptionId = jsonObject.optString("correct_option_id")
        val front = jsonObject.optString("front")
        val back = jsonObject.optString("back")
        val mistakes = jsonObject.optInt("mistakes", 0)
        val correctText = jsonObject.optString("correct_text")

        return Question(
            id = id,
            type = type,
            text = text,
            options = options,
            correctOptionId = correctOptionId,
            front = front,
            back = back,
            mistakes = mistakes,
            correctText = correctText
        )
    }
    
    private fun parseOptions(optionsArray: JSONArray?): List<Question.Option> {
        if (optionsArray == null) return emptyList()
        
        val options = mutableListOf<Question.Option>()
        for (i in 0 until optionsArray.length()) {
            val optionObj = optionsArray.getJSONObject(i)
            options.add(
                Question.Option(
                    id = optionObj.getString("id"),
                    text = optionObj.getString("text"),
                    isCorrect = optionObj.optBoolean("is_correct", false)
                )
            )
        }
        return options
    }
} 