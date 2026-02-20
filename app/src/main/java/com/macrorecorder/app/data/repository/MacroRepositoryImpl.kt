package com.macrorecorder.app.data.repository

import com.google.gson.Gson
import com.macrorecorder.app.data.local.db.MacroDao
import com.macrorecorder.app.data.local.storage.TouchEventStorage
import com.macrorecorder.app.data.mapper.toDomain
import com.macrorecorder.app.data.mapper.toEntity
import com.macrorecorder.app.domain.model.Macro
import com.macrorecorder.app.domain.model.TouchEvent
import com.macrorecorder.app.domain.repository.MacroRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MacroRepositoryImpl(
    private val macroDao: MacroDao,
    private val touchEventStorage: TouchEventStorage,
    private val gson: Gson = Gson()
) : MacroRepository {

    override fun getAllMacros(): Flow<List<Macro>> =
        macroDao.getAllMacros().map { entities -> entities.map { it.toDomain(gson) } }

    override fun searchMacros(query: String): Flow<List<Macro>> =
        macroDao.searchMacros(query).map { entities -> entities.map { it.toDomain(gson) } }

    override suspend fun getMacroById(id: String): Macro? =
        macroDao.getMacroById(id)?.toDomain(gson)

    override suspend fun saveMacro(macro: Macro, events: List<TouchEvent>) {
        touchEventStorage.save(macro.id, events)
        macroDao.insertMacro(macro.toEntity(gson))
    }

    override suspend fun updateMacro(macro: Macro) {
        macroDao.updateMacro(macro.toEntity(gson))
    }

    override suspend fun deleteMacro(id: String) {
        touchEventStorage.delete(id)
        macroDao.deleteMacroById(id)
    }

    override suspend fun loadEvents(macroId: String): List<TouchEvent> =
        touchEventStorage.load(macroId)
}
