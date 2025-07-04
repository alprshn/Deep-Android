package com.kami_apps.deepwork.deep_work_app.domain.usecases

import com.kami_apps.deepwork.deep_work_app.data.local.entities.Sessions
import com.kami_apps.deepwork.deep_work_app.data.local.entities.Tags
import com.kami_apps.deepwork.deep_work_app.domain.repository.SessionsRepository
import com.kami_apps.deepwork.deep_work_app.domain.repository.TagsRepository
import com.kami_apps.deepwork.deep_work_app.presentation.timeline_screen.components.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Date
import javax.inject.Inject
import android.util.Log
import com.kami_apps.deepwork.deep_work_app.data.util.parseTagColor

class GetTimelineEventsUseCase @Inject constructor(
    private val sessionsRepository: SessionsRepository,
    private val tagsRepository: TagsRepository
) {
    suspend operator fun invoke(startDate: Date, endDate: Date): Flow<List<Event>> {
        Log.d("GetTimelineEventsUseCase", "invoke called with startDate: $startDate, endDate: $endDate")
        return combine(
            sessionsRepository.getSessionsByDate(startDate, endDate),
            tagsRepository.getAllTags()
        ) { sessions, tags ->
            Log.d("GetTimelineEventsUseCase", "Got ${sessions.size} sessions and ${tags.size} tags")
            val tagsMap = tags.associateBy { it.tagId }
            
            sessions.forEach { session ->
                Log.d("GetTimelineEventsUseCase", "Session: id=${session.sessionId}, tagId=${session.tagId}, start=${session.startTime}, end=${session.finishTime}")
            }
            
            tags.forEach { tag ->
                Log.d("GetTimelineEventsUseCase", "Tag: id=${tag.tagId}, name=${tag.tagName}, emoji=${tag.tagEmoji}")
            }
            
            val events = sessions.mapNotNull { session ->
                val tag = tagsMap[session.tagId]
                if (tag != null && session.startTime != null && session.finishTime != null) {
                    Log.d("GetTimelineEventsUseCase", "Creating event for session ${session.sessionId} with tag ${tag.tagName}")
                    mapToEvent(session, tag)
                } else {
                    Log.w("GetTimelineEventsUseCase", "Skipping session ${session.sessionId}: tag=$tag, startTime=${session.startTime}, finishTime=${session.finishTime}")
                    null
                }
            }
            
            Log.d("GetTimelineEventsUseCase", "Created ${events.size} events")
            events
        }
    }
    
    private fun mapToEvent(session: Sessions, tag: Tags): Event {
        val startDateTime = session.startTime!!.toLocalDateTime()
        val endDateTime = session.finishTime!!.toLocalDateTime()
        
        Log.d("GetTimelineEventsUseCase", "Mapping session ${session.sessionId}: timestamp ${session.startTime!!.time} -> LocalDateTime $startDateTime")
        
        return Event(
            name = tag.tagName,
            color = parseTagColor(tag.tagColor),
            start = startDateTime,
            end = endDateTime,
            emoji = tag.tagEmoji.takeIf { it.isNotBlank() } ?: "📖"
        )
    }
}

// Extension function to convert Date to LocalDateTime
private fun Date.toLocalDateTime(): java.time.LocalDateTime {
    return java.time.LocalDateTime.ofInstant(
        this.toInstant(),
        java.time.ZoneId.systemDefault()
    )
} 