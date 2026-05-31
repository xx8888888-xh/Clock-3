package com.clock3.pet.ui

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clock3.pet.R
import com.clock3.pet.data.ShopRepository
import com.clock3.pet.model.ItemType
import com.clock3.pet.model.ShopItem
import com.clock3.pet.utils.ThemeManager
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class ShopActivity : AppCompatActivity() {

    private lateinit var repository: ShopRepository
    private lateinit var currentExpText: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShopAdapter

    private var allItems: List<ShopItem> = emptyList()
    private var currentTab: Int = 0
    private var isPurchasing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this, ThemeManager.getCurrentTheme(this))
        setContentView(R.layout.activity_shop)

        repository = ShopRepository(this)

        initViews()
        setupTabs()
        setupRecyclerView()
        loadItems()
        updateExpDisplay()
    }

    private fun initViews() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        currentExpText = findViewById(R.id.currentExpText)
        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.shopRecyclerView)
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.shop_tab_all)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.shop_tab_themes)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.shop_tab_breaks)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.shop_tab_rewards)))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                filterItems()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        val unlockedItems = repository.getUnlockedItems().toSet()
        adapter = ShopAdapter(allItems, unlockedItems) { item ->
            purchaseItem(item)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadItems() {
        allItems = repository.defaultItems
        filterItems()
    }

    private fun filterItems() {
        val unlockedItems = repository.getUnlockedItems().toSet()
        val filteredItems = when (currentTab) {
            0 -> allItems
            1 -> allItems.filter { it.type == ItemType.THEME }
            2 -> allItems.filter { it.type == ItemType.BREAK_TIME }
            3 -> allItems.filter { it.type == ItemType.REWARD }
            else -> allItems
        }
        adapter.updateItems(filteredItems, unlockedItems)
    }

    private fun updateExpDisplay() {
        lifecycleScope.launch {
            val totalExp = repository.getTotalExp()
            currentExpText.text = totalExp.toString()
        }
    }

    private fun purchaseItem(item: ShopItem) {
        if (isPurchasing) return
        if (repository.isItemUnlocked(item.id)) {
            if (item.type == ItemType.THEME) {
                showThemeSelectionDialog(item)
            } else {
                Toast.makeText(this, getString(R.string.shop_already_owned), Toast.LENGTH_SHORT).show()
            }
            return
        }

        isPurchasing = true
        lifecycleScope.launch {
            try {
                val currentExp = repository.getTotalExp()
                if (currentExp < item.cost) {
                    Toast.makeText(this@ShopActivity, getString(R.string.shop_not_enough_exp, item.cost), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (repository.spendExp(item.cost)) {
                    repository.unlockItem(item.id)

                    if (item.type == ItemType.BREAK_TIME) {
                        val minutes = extractMinutes(item.id)
                        repository.addBreakTicket(minutes)
                    }

                    Toast.makeText(this@ShopActivity, getString(R.string.shop_purchase_success, item.name), Toast.LENGTH_SHORT).show()
                    updateExpDisplay()
                    filterItems()
                } else {
                    Toast.makeText(this@ShopActivity, getString(R.string.shop_purchase_failed), Toast.LENGTH_SHORT).show()
                }
            } finally {
                isPurchasing = false
            }
        }
    }
    
    private fun showThemeSelectionDialog(item: ShopItem) {
        val currentTheme = repository.getCurrentTheme()
        val isCurrent = currentTheme == item.id
        
        val options = if (isCurrent) {
            arrayOf(getString(R.string.shop_use_default_theme))
        } else {
            arrayOf(getString(R.string.shop_apply_theme, item.name), getString(R.string.shop_use_default_theme))
        }
        
        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(options) { _, which ->
                if (isCurrent && which == 0) {
                    repository.setCurrentTheme(ShopRepository.THEME_DEFAULT)
                    Toast.makeText(this, getString(R.string.shop_default_theme), Toast.LENGTH_SHORT).show()
                    recreate()
                } else if (!isCurrent && which == 0) {
                    repository.setCurrentTheme(item.id)
                    Toast.makeText(this, getString(R.string.shop_theme_applied, item.name), Toast.LENGTH_SHORT).show()
                    recreate()
                } else if (!isCurrent && which == 1) {
                    repository.setCurrentTheme(ShopRepository.THEME_DEFAULT)
                    Toast.makeText(this, getString(R.string.shop_default_theme), Toast.LENGTH_SHORT).show()
                    recreate()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun extractMinutes(id: String): Int {
        return when (id) {
            "break_30min" -> 30
            "break_60min" -> 60
            else -> 10
        }
    }

    override fun onResume() {
        super.onResume()
        updateExpDisplay()
        filterItems()
    }
}
