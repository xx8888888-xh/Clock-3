package com.clock3.pet

import com.clock3.pet.data.model.Pet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetModelTest {

    @Test
    fun getExpForNextLevel_level1() {
        val pet = Pet(level = 1)
        assertEquals(100, pet.getExpForNextLevel())
    }

    @Test
    fun getExpForNextLevel_level5() {
        val pet = Pet(level = 5)
        assertEquals(500, pet.getExpForNextLevel())
    }

    @Test
    fun addExp_noLevelUp() {
        val pet = Pet(level = 1, exp = 50)
        val (updatedPet, messages) = pet.addExp(30)
        assertEquals(80, updatedPet.exp)
        assertEquals(1, updatedPet.level)
        assertTrue(messages.isEmpty())
    }

    @Test
    fun addExp_exactLevelUp() {
        val pet = Pet(level = 1, exp = 50)
        val (updatedPet, messages) = pet.addExp(50)
        assertEquals(0, updatedPet.exp)
        assertEquals(2, updatedPet.level)
        assertEquals(1, messages.size)
    }

    @Test
    fun addExp_multipleLevelUps() {
        val pet = Pet(level = 1, exp = 0)
        val (updatedPet, messages) = pet.addExp(300)
        assertEquals(3, updatedPet.level)
        assertEquals(0, updatedPet.exp)
        assertEquals(2, messages.size)
    }

    @Test
    fun addExp_zero() {
        val pet = Pet(level = 3, exp = 50)
        val (updatedPet, messages) = pet.addExp(0)
        assertEquals(50, updatedPet.exp)
        assertEquals(3, updatedPet.level)
        assertTrue(messages.isEmpty())
    }

    @Test
    fun interact_increasesTotalInteractions() {
        val pet = Pet(totalInteractions = 5)
        val (updatedPet, _) = pet.interact()
        assertEquals(6, updatedPet.totalInteractions)
    }

    @Test
    fun petDefaultValues() {
        val pet = Pet()
        assertEquals(1, pet.id)
        assertEquals(Pet.PetMood.HAPPY, pet.mood)
        assertEquals(1, pet.level)
        assertEquals(0, pet.exp)
        assertEquals("小宠物", pet.name)
    }
}
