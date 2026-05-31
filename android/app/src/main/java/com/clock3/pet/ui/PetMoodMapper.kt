package com.clock3.pet.ui

import com.clock3.pet.data.model.Pet
import com.clock3.pet.widget.CirclePetView

object PetMoodMapper {
    fun map(mood: Pet.PetMood): CirclePetView.Mood {
        return when (mood) {
            Pet.PetMood.HAPPY -> CirclePetView.Mood.HAPPY
            Pet.PetMood.SAD -> CirclePetView.Mood.BORED
            Pet.PetMood.SLEEPY -> CirclePetView.Mood.RESTING
            Pet.PetMood.EXCITED -> CirclePetView.Mood.EXCITED
            Pet.PetMood.HUNGRY -> CirclePetView.Mood.HUNGRY
            Pet.PetMood.BORED -> CirclePetView.Mood.BORED
        }
    }
}