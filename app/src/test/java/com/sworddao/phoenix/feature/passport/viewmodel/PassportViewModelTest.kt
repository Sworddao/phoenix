package com.sworddao.phoenix.feature.passport.viewmodel

import com.sworddao.phoenix.feature.gameplay.data.MockGameProgressRepository
import com.sworddao.phoenix.feature.passport.data.MockPassportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PassportViewModelTest {

    private lateinit var passportRepository: MockPassportRepository
    private lateinit var gameProgressRepository: MockGameProgressRepository

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        passportRepository = MockPassportRepository()
        gameProgressRepository = MockGameProgressRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `earning a stamp syncs game progress total`() = runTest {
        val viewModel = PassportViewModel(passportRepository, gameProgressRepository)
        viewModel.earnStamp("qingyuan_village")

        val progress = gameProgressRepository.getGameProgress().first()
        assertEquals(1, progress.totalPassportStamps)
        assertEquals(1, passportRepository.getPassport().first().totalStamps)
    }

    @Test
    fun `re-earning an already earned stamp does not double sync`() = runTest {
        val viewModel = PassportViewModel(passportRepository, gameProgressRepository)
        viewModel.earnStamp("qingyuan_village")
        viewModel.earnStamp("qingyuan_village")

        val progress = gameProgressRepository.getGameProgress().first()
        assertEquals(1, progress.totalPassportStamps)
        assertEquals(1, passportRepository.getPassport().first().totalStamps)
    }

    @Test
    fun `earning stamps across distinct regions counts each`() = runTest {
        val viewModel = PassportViewModel(passportRepository, gameProgressRepository)
        viewModel.earnStamp("qingyuan_village")
        viewModel.earnStamp("jade_forest")

        val progress = gameProgressRepository.getGameProgress().first()
        assertEquals(2, progress.totalPassportStamps)
        assertEquals(2, passportRepository.getPassport().first().totalStamps)
    }
}
