package com.example.habittracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habittracker.util.WeatherAdvice

private val palette = listOf(
    Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFFE91E63), Color(0xFF9C27B0)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(vm: TodayViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<HabitItem?>(null) }
    var showCity by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Today") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add habit")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { WeatherCard(state, onChangeCity = { showCity = true }) }

            if (state.habits.isEmpty()) {
                item { Text("No habits yet. Tap + to add your first one.") }
            }
            items(state.habits, key = { it.habit.id }) { item ->
                HabitCard(
                    item = item,
                    onToggle = { vm.toggle(item, it) },
                    onDelete = { pendingDelete = item }
                )
            }
        }
    }

    if (showAdd) {
        AddHabitDialog(
            onDismiss = { showAdd = false },
            onSave = { name, colour, outdoor -> vm.addHabit(name, colour, outdoor); showAdd = false }
        )
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete habit?") },
            text = { Text("\"${item.habit.name}\" and its history will be removed.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteHabit(item.habit); pendingDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
    if (showCity) {
        CityDialog(
            error = state.cityError,
            onDismiss = { vm.clearCityError(); showCity = false },
            onSave = { city -> vm.setCity(city) { showCity = false } }
        )
    }
}

@Composable
private fun WeatherCard(state: TodayUiState, onChangeCity: () -> Unit) {
    val w = state.weather
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (w == null) {
                Text("Set your city to get weather-based habit tips.")
            } else {
                Text(
                    "${WeatherAdvice.emoji(w.weatherCode)} ${w.city} · ${Math.round(w.temperatureC)}°C",
                    style = MaterialTheme.typography.titleMedium
                )
                state.weatherAdvice?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (state.weatherFailed) {
                    Text("Offline: showing last saved weather.", style = MaterialTheme.typography.labelSmall)
                }
            }
            TextButton(onClick = onChangeCity) { Text(if (w == null) "Set city" else "Change city") }
            // Open-Meteo's free tier asks for attribution
            Text("Weather data by Open-Meteo.com", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CityDialog(error: String?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var city by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your city") },
        text = {
            OutlinedTextField(
                value = city,
                onValueChange = { city = it.take(60) },
                label = { Text("City name") },
                singleLine = true,
                isError = error != null,
                supportingText = { if (error != null) Text(error) }
            )
        },
        confirmButton = {
            TextButton(enabled = city.isNotBlank(), onClick = { onSave(city) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun HabitCard(item: HabitItem, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(item.habit.colour)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "🔥 ${item.currentStreak} day streak · best ${item.bestStreak}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (item.habit.outdoor) {
                    Text("🌳 Outdoor", style = MaterialTheme.typography.labelSmall)
                }
            }
            Checkbox(checked = item.doneToday, onCheckedChange = onToggle)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete habit")
            }
        }
    }
}

@Composable
private fun Box(argb: Int) {
    androidx.compose.foundation.layout.Box(
        Modifier.size(12.dp, 48.dp).clip(CircleShape).background(Color(argb))
    )
}

@Composable
private fun AddHabitDialog(onDismiss: () -> Unit, onSave: (String, Int, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var colour by remember { mutableStateOf(palette.first()) }
    var outdoor by remember { mutableStateOf(false) }
    val invalid = name.isBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Habit name") },
                    singleLine = true,
                    isError = invalid,
                    supportingText = { if (invalid) Text("Please enter a name") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    palette.forEach { c ->
                        androidx.compose.foundation.layout.Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (c == colour) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { colour = c }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Outdoor habit", Modifier.weight(1f))
                    Switch(checked = outdoor, onCheckedChange = { outdoor = it })
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !invalid, onClick = { onSave(name, colour.toArgb(), outdoor) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
