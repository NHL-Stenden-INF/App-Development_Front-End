package com.nhlstenden.appdev.features.home

import com.nhlstenden.appdev.R
import android.content.Context
import android.util.Log
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class ChallengeParser(private val context: Context) {
    fun loadAllChallenges(): List<DailyChallenge> {
        val resourceId = context.resources.openRawResource(R.raw.challenges)

        return try {
            parseChallengesXML(resourceId)
        } catch (e: Exception) {
            Log.e("ChallengeParser", "Failed to parse challenges", e)
            emptyList<DailyChallenge>()
        }
    }
    
    private fun parseChallengesXML(inputStream: InputStream): List<DailyChallenge> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
        val tasksElement = document.documentElement
        val taskElements = tasksElement.getElementsByTagName("challenge")

        return List(taskElements.length) { index ->
            val taskElement = taskElements.item(index) as Element
            parseChallenge(taskElement, index)
        }
    }
    
    private fun parseChallenge(challengeElement: Element, index: Int): DailyChallenge {
        val title = challengeElement.getElementsByTagName("title").item(0).textContent
        val description = challengeElement.getElementsByTagName("description").item(0).textContent
        val buggedCode = challengeElement.getElementsByTagName("buggedCode").item(0).textContent
        val correctedCode = challengeElement.getElementsByTagName("correctedCode").item(0).textContent
        
        return DailyChallenge(
            title = title,
            description = description,
            buggedCode = buggedCode,
            correctedCode = correctedCode,
        )
    }
}
