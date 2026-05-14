package com.clock3.pet.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.clock3.pet.R
import com.clock3.pet.data.model.Pet
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.service.PetService
import kotlinx.coroutines.launch

class PetActivity : AppCompatActivity() {
    private lateinit var repository: Clock3Repository
    private lateinit var petService: PetService

    private lateinit var petImage: ImageView
    private lateinit var petName: TextView
    private lateinit var moodText: TextView
    private lateinit var levelText: TextView
    private lateinit var expText: TextView
    private lateinit var messageText: TextView
    private lateinit var interactionsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        findViewById<Button>(R.id.backButton).setOnClickListener { finish() }

        findViewById<Button>(R.id.interactButton).setOnClickListener {
            lifecycleScope.launch {
                val result = petService.interact()
                showResult(result)
                loadPetData()
            }
        }

        findViewById<Button>(R.id.feedButton).setOnClickListener {
            lifecycleScope.launch {
                val result = petService.feedPet()
                showResult(result)
                loadPetData()
            }
        }

        findViewById<Button>(R.id.playButton).setOnClickListener {
            lifecycleScope.launch {
                val result = petService.playWithPet()
                showResult(result)
                loadPetData()
            }
        }
    }

    private fun loadPetData() {
        lifecycleScope.launch {
            val pet = petService.loadPet()
            updateUI(pet)
        }
    }

    private fun updateUI(pet: Pet) {
        petName.text = "${pet.mood.emoji} ${pet.name}"
        moodText.text = "心情: ${getMoodName(pet.mood)}"
        levelText.text = "等级: ${pet.level}"
        expText.text = "经验: ${pet.exp}/${pet.getExpForNextLevel()}"
        messageText.text = pet.getRandomMessage()
        interactionsText.text = "交互次数: ${pet.totalInteractions}"
    }

    private fun getMoodName(mood: Pet.PetMood): String {
        return when (mood) {
            Pet.PetMood.HAPPY -> "开心 😊"
            Pet.PetMood.SAD -> "难过 😢"
            Pet.PetMood.SLEEPY -> "困倦 😴"
            Pet.PetMood.EXCITED -> "兴奋 🤩"
            Pet.PetMood.HUNGRY -> "饥饿 🤤"
            Pet.PetMood.BORED -> "无聊 😐"
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun showResult(result: Map<String, Any>) {
        val message = result["message"] as? String ?: ""
        messageText.text = message

        val levelUp = result["level_up"] as? List<String>
        if (!levelUp.isNullOrEmpty()) {
            Toast.makeText(this, levelUp.joinToString("\n"), Toast.LENGTH_SHORT).show()
        }
    }
}
