package com.clock3.pet.ui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.clock3.pet.R
import com.clock3.pet.data.model.Pet
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.service.PetService
import com.clock3.pet.utils.ThemeManager
import com.clock3.pet.widget.CirclePetView
import kotlinx.coroutines.launch

class PetActivity : AppCompatActivity() {
    private lateinit var repository: Clock3Repository
    private lateinit var petService: PetService
    private lateinit var petImage: CirclePetView
    private lateinit var petName: TextView
    private lateinit var moodText: TextView
    private lateinit var levelText: TextView
    private lateinit var expText: TextView
    private lateinit var messageText: TextView
    private lateinit var interactionsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this, ThemeManager.getCurrentTheme(this))
        setContentView(R.layout.activity_pet)

        repository = Clock3Repository(this)
        petService = PetService.getInstance(this)

        initViews()
        loadPetData()
        setupListeners()
    }

    private fun initViews() {
        petImage = findViewById(R.id.petImage)
        petName = findViewById(R.id.petName)
        moodText = findViewById(R.id.moodText)
        levelText = findViewById(R.id.levelText)
        expText = findViewById(R.id.expText)
        messageText = findViewById(R.id.messageText)
        interactionsText = findViewById(R.id.interactionsText)
    }

    private fun setupListeners() {
        findViewById<LinearLayout>(R.id.backButton).setOnClickListener { finish() }

        findViewById<LinearLayout>(R.id.interactButton).setOnClickListener {
            performAction { petService.interact() }
        }

        findViewById<LinearLayout>(R.id.feedButton).setOnClickListener {
            performAction { petService.feedPet() }
        }

        findViewById<LinearLayout>(R.id.playButton).setOnClickListener {
            performAction { petService.playWithPet() }
        }
    }

    private fun performAction(action: suspend () -> Map<String, Any>) {
        lifecycleScope.launch {
            val result = action()
            showResult(result)
            loadPetData()
        }
    }

    private fun loadPetData() {
        lifecycleScope.launch {
            val pet = petService.loadPet()
            updateUI(pet)
        }
    }

    private fun updateUI(pet: Pet) {
        petName.text = pet.name
        moodText.text = getString(R.string.pet_mood_format, getMoodName(pet.mood))
        levelText.text = getString(R.string.pet_level_format, pet.level)
        expText.text = getString(R.string.pet_exp_format, pet.exp, pet.getExpForNextLevel())
        messageText.text = pet.getRandomMessage()
        interactionsText.text = getString(R.string.pet_interactions_format, pet.totalInteractions)
        petImage.setMood(PetMoodMapper.map(pet.mood))
    }

    private fun getMoodName(mood: Pet.PetMood): String {
        return getString(when (mood) {
            Pet.PetMood.HAPPY -> R.string.mood_happy
            Pet.PetMood.SAD -> R.string.mood_sad
            Pet.PetMood.SLEEPY -> R.string.mood_sleepy
            Pet.PetMood.EXCITED -> R.string.mood_excited
            Pet.PetMood.HUNGRY -> R.string.mood_hungry
            Pet.PetMood.BORED -> R.string.mood_bored
        })
    }

    private fun showResult(result: Map<String, Any>) {
        val message = result["message"] as? String ?: ""
        messageText.text = message

        val levelUp = result["level_up"] as? List<*>
        if (!levelUp.isNullOrEmpty()) {
            val messages = levelUp.mapNotNull { it as? String }
            Toast.makeText(this, messages.joinToString("\n"), Toast.LENGTH_SHORT).show()
        }
    }
}
