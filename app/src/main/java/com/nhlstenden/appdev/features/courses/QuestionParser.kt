package com.nhlstenden.appdev.features.courses

import android.content.Context
import android.util.Log
import com.nhlstenden.appdev.features.task.models.Question
import com.nhlstenden.appdev.features.task.models.QuestionType
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import org.json.JSONException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class QuestionParser(private val context: Context) {
    
    fun loadQuestionsForTask(tasksId: String): List<Question> {
        val resourceId = context.resources.getIdentifier("${tasksId}_questions", "raw", context.packageName)
        if (resourceId == 0) {
            Log.e("QuestionParser", "No resource found for task: $tasksId")
            return emptyList()
        }
        
        var inputStream: InputStream? = null
        try {
            inputStream = context.resources.openRawResource(resourceId)
            // Try to parse as JSON, fallback to XML if it fails
            return try {
                parseQuestionsJson(inputStream)
            } catch (jsonEx: JSONException) {
                inputStream.close()
                // Reopen stream for XML parsing
                val xmlStream = context.resources.openRawResource(resourceId)
                val result = parseQuestionsXml(xmlStream)
                xmlStream.close()
                result
            }
        } catch (e: Exception) {
            Log.e("QuestionParser", "Error loading questions for task: $tasksId", e)
            return emptyList()
        } finally {
            inputStream?.close()
        }
    }
    
    private fun parseQuestionsJson(inputStream: InputStream): List<Question> {
        val questions = mutableListOf<Question>()
        try {
            val jsonArray = JSONArray(inputStream.bufferedReader().use { it.readText() })
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                questions.add(parseQuestion(jsonObject))
            }
        } catch (e: JSONException) {
            throw e
        } catch (e: Exception) {
            Log.e("QuestionParser", "Error parsing JSON", e)
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

    private fun parseQuestionsXml(inputStream: InputStream): List<Question> {
        val questions = mutableListOf<Question>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(inputStream, null)
            var eventType = parser.eventType
            var currentQuestion: Question? = null
            var currentOptions = mutableListOf<Question.Option>()
            var correctOptionId: String? = null
            var questionId = 1
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "multiple_choice_question" -> {
                                currentOptions = mutableListOf()
                                correctOptionId = null
                            }
                            "question_text" -> {
                                parser.next()
                                val text = parser.text
                                currentQuestion = Question(
                                    id = questionId.toString(),
                                    type = QuestionType.MULTIPLE_CHOICE,
                                    text = text ?: "",
                                    options = emptyList()
                                )
                            }
                            "option" -> {
                                val isCorrect = parser.getAttributeValue(null, "correct") == "true"
                                parser.next()
                                val text = parser.text ?: ""
                                val optionId = (currentOptions.size + 1).toString()
                                currentOptions.add(Question.Option(optionId, text, isCorrect))
                                if (isCorrect) correctOptionId = optionId
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "multiple_choice_question" -> {
                                if (currentQuestion != null) {
                                    questions.add(currentQuestion.copy(options = currentOptions.toList(), correctOptionId = correctOptionId))
                                    questionId++
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("QuestionParser", "Error parsing XML", e)
        } finally {
            inputStream.close()
        }
        return questions
    }
} 