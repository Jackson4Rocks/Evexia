package com.jackson4rocks.evexia

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.jackson4rocks.evexia.ui.AppViewModel
import com.jackson4rocks.evexia.ui.EvexiaRoot

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]
        handleIntent(intent)
        setContent {
            EvexiaRoot(viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val target = intent ?: return
        val openTasks = target.getBooleanExtra("open_tasks", false)
        val openTimetable = target.getBooleanExtra("open_timetable", false)
        if (openTasks || openTimetable) {
            viewModel.openFromIntent(openTasks, openTimetable)
            target.removeExtra("open_tasks")
            target.removeExtra("open_timetable")
        }
    }
}
