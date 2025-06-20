package com.nhlstenden.appdev.features.course.utils

import android.content.Context
import android.util.Log
import com.nhlstenden.appdev.features.task.models.MultipleChoiceOption
import com.nhlstenden.appdev.features.task.models.Question
import java.io.InputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class QuestionParser(private val context: Context) {
    
    fun loadQuestionsForTask(tasksId: String): List<Question> {
        val resourceId =
            context.resources.getIdentifier("${tasksId}_questions", "raw", context.packageName)
        if (resourceId == 0) {
            Log.e("QuestionParser", "No resource found for task: $tasksId")
            return emptyList()
        }

        try {
            val xmlStream = context.resources.openRawResource(resourceId)
            val result = parseQuestionsXml(xmlStream)
            xmlStream.close()
            return result
        } catch (e: Exception) {
            Log.e("QuestionParser", "Error loading questions for task: $tasksId", e)
            return emptyList()
        }
    }

    private fun parseQuestionsXml(inputStream: InputStream): List<Question> {
        val questions = mutableListOf<Question>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            var currentQuestionId = 1

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            // MULTIPLE CHOICE
                            "multiple_choice_question" -> {
                                var questionText: String = ""
                                var explanation: String? = null


                                val options = mutableListOf<MultipleChoiceOption>()

                                while (!(parser.eventType == XmlPullParser.END_TAG && parser.name == "multiple_choice_question")) {
                                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "option") {
                                        val isCorrect = parser.getAttributeValue(null, "correct") == "true"
                                        parser.next()
                                        val optionText = parser.text ?: ""
                                        val optionId = (options.size + 1).toString()
                                        options.add(MultipleChoiceOption(optionId, optionText, isCorrect))
                                    }

                                    parseCommonFields(parser, {questionText = it}, {explanation = it})
                                    parser.next()
                                }

                                questions.add(
                                    Question.MultipleChoiceQuestion(
                                        id = currentQuestionId.toString(),
                                        question = questionText,
                                        options = options,
                                        explanation = explanation
                                    )
                                )
                                currentQuestionId++
                            }

                            // FLIPCARD
                            "flipcard_question" -> {
                                var questionText: String = ""
                                var explanation: String? = null

                                var front: String = ""
                                var back: String = ""

                                while (!(parser.eventType == XmlPullParser.END_TAG && parser.name == "flipcard_question")) {
                                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "front") {
                                        parser.next()
                                        front = parser.text
                                    }
                                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "back") {
                                        parser.next()
                                        back = parser.text
                                    }

                                    parseCommonFields(parser, {questionText = it}, {explanation = it})
                                    parser.next()
                                }

                                questions.add(
                                    Question.FlipCardQuestion(
                                        id = currentQuestionId.toString(),
                                        question = questionText,
                                        front = front,
                                        back = back,
                                        explanation = explanation
                                    )
                                )
                                currentQuestionId++
                            }

                            // PRESS MISTAKE
                            "press_mistake_question" -> {
                                var questionText = ""
                                var explanation: String? = null

                                var displayedText = ""
                                val mistakes = mutableSetOf<Int>()


                                while (!(parser.eventType == XmlPullParser.END_TAG && parser.name == "press_mistake_question")) {
                                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "displayed_text") {
                                        parser.next()
                                        displayedText = parser.text
                                    }
                                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "mistake") {
                                        val mistakePosition = parser.getAttributeValue(null, "position")
                                        if (mistakePosition != null)
                                            mistakes.add(mistakePosition.toInt())
                                    }

                                    parseCommonFields(parser, {questionText = it}, {explanation = it})
                                    parser.next()
                                }

                                questions.add(
                                    Question.PressMistakeQuestion(
                                        id = currentQuestionId.toString(),
                                        question = questionText,
                                        displayedText = displayedText,
                                        mistakes = mistakes,
                                        explanation = explanation
                                    )
                                )
                                currentQuestionId++
                            }

                            // EDIT TEXT
                            "edit_text_question" -> {
                                var questionText = ""
                                var explanation: String? = null

                                var displayedText: String = ""
                                var correctText: String = ""

                                while (!(parser.eventType == XmlPullParser.END_TAG && parser.name == "edit_text_question")) {
                                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "displayed_text") {
                                        parser.next()
                                        displayedText = parser.text
                                    }
                                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "correct_text") {
                                        parser.next()
                                        correctText = parser.text
                                    }

                                    parseCommonFields(parser, {questionText = it}, {explanation = it})
                                    parser.next()
                                }

                                questions.add(
                                    Question.EditTextQuestion(
                                        id = currentQuestionId.toString(),
                                        question = questionText,
                                        displayedText = displayedText,
                                        correctText = correctText,
                                        explanation = explanation
                                    )
                                )
                                currentQuestionId++
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

    private fun parseCommonFields(
        parser: XmlPullParser,
        setQuestionText: (String) -> Unit,
        setExplanation: (String?) -> Unit
    ) {
        if (parser.eventType == XmlPullParser.START_TAG) {
            when (parser.name) {
                "question_text" -> {
                    parser.next()
                    setQuestionText(parser.text ?: "")
                }
                "explanation" -> {
                    parser.next()
                    setExplanation(parser.text)
                }
            }
        }
    }

} 