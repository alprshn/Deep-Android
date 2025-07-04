package com.kami_apps.deepwork.deep_work_app.domain.usecases

import com.kami_apps.deepwork.deep_work_app.data.local.entities.Sessions
import com.kami_apps.deepwork.deep_work_app.domain.repository.SessionsRepository
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject

class GetSessionsByDateUseCase @Inject constructor (private val sessionsRepository: SessionsRepository) {
    suspend operator fun invoke(startDate: Date, endDate: Date): Flow<List<Sessions>> {
        return sessionsRepository.getSessionsByDate(startDate, endDate)
    }

}