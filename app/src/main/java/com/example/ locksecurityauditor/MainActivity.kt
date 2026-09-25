package com.example.locksecurityauditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LockAuditorScreen()
                }
            }
        }
    }
}

@Composable
fun LockAuditorScreen() {
    var pin by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("4") }
    var pinResult by remember { mutableStateOf("") }
    var patternResult by remember { mutableStateOf("") }
    var simResult by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Lock Security Auditor", style = MaterialTheme.typography.headlineMedium)
        Text("Educational audit for your own device. No data leaves this app.", style = MaterialTheme.typography.bodySmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PIN Strength", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(12) },
                    label = { Text("Enter PIN (digits only)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { pinResult = analyzePin(pin) }) {
                    Text("Analyze PIN")
                }
                if (pinResult.isNotBlank()) {
                    Text(pinResult)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pattern Strength", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Pattern e.g. 0-1-2-4-6-7-8") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { patternResult = analyzePattern(pattern) }) {
                    Text("Analyze Pattern")
                }
                if (patternResult.isNotBlank()) {
                    Text(patternResult)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Brute-force Time Simulation", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = length,
                    onValueChange = { length = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("PIN length") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = {
                    val len = length.toIntOrNull() ?: 4
                    simResult = simulateBruteForce(len)
                }) {
                    Text("Simulate")
                }
                if (simResult.isNotBlank()) {
                    Text(simResult)
                }
            }
        }

        Text(
            "Disclaimer: This tool does not bypass locks or access other people's devices. It simulates modern Android security policies (Android 12+). Older devices may have weaker lockout rules.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

private val COMMON_PINS = setOf("1234", "0000", "1111", "1212", "7777", "1004", "2000", "4444", "2222", "6969")

fun shannonEntropy(pin: String): Double {
    if (pin.isEmpty()) return 0.0
    val counts = pin.groupingBy { it }.eachCount()
    val len = pin.length
    return -counts.values.sumOf { c ->
        val p = c.toDouble() / len
        p * log2(p)
    } * len
}

fun isCommon(pin: String) = pin in COMMON_PINS

fun isSequence(pin: String): Boolean {
    if (pin.length < 2) return false
    val asc = (0 until pin.length - 1).all { pin[it + 1].digitToInt() == pin[it].digitToInt() + 1 }
    val desc = (0 until pin.length - 1).all { pin[it + 1].digitToInt() == pin[it].digitToInt() - 1 }
    return asc || desc
}

fun isRepeated(pin: String) = pin.isNotEmpty() && pin.all { it == pin[0] }

fun analyzePin(pin: String): String {
    if (pin.isEmpty()) return "Enter a PIN."
    val entropy = shannonEntropy(pin)
    val common = isCommon(pin)
    val seq = isSequence(pin)
    val rep = isRepeated(pin)
    val score = when {
        common || rep || seq -> 1
        pin.length < 6 -> 2
        pin.length < 8 -> 3
        else -> 4
    }
    return buildString {
        appendLine("PIN: $pin")
        appendLine("Length: ${pin.length}")
        appendLine("Entropy: ${"%.2f".format(entropy)} bits")
        appendLine("Common PIN: $common")
        appendLine("Sequence: $seq")
        appendLine("Repeated: $rep")
        appendLine("Score: $score / 4")
        appendLine("Advice: " + when (score) {
            1 -> "Very weak. Change it."
            2 -> "Weak. Use at least 6 digits, avoid patterns."
            3 -> "Moderate. Consider 8+ random digits."
            else -> "Strong."
        })
    }
}

fun analyzePattern(pattern: String): String {
    val nodes = pattern.split("-").mapNotNull { it.trim().toIntOrNull() }
    if (nodes.isEmpty()) return "Enter a pattern like 0-1-2-4-6-7-8."
    val unique = nodes.toSet().size
    val length = nodes.size
    val score = when {
        unique <= 2 -> 1
        length < 6 -> 2
        else -> 3
    }
    return buildString {
        appendLine("Pattern: $pattern")
        appendLine("Length: $length")
        appendLine("Unique nodes: $unique")
        appendLine("Score: $score / 3")
        appendLine("Advice: " + when (score) {
            1 -> "Very weak. Use more unique dots."
            2 -> "Weak. Use at least 6 dots with overlaps."
            else -> "Good."
        })
    }
}

fun simulateBruteForce(length: Int): String {
    val total = 10.0.pow(length)
    val first = minOf(total, 20.0)
    val second = minOf(total - first, 20.0).coerceAtLeast(0.0)
    val remaining = (total - first - second).coerceAtLeast(0.0)
    var log10Seconds = 0.0
    if (remaining == 0.0) {
        val seconds = first * 1.0 + second * 30.0
        log10Seconds = log10(seconds)
    } else {
        log10Seconds = log10(300.0) + remaining * log10(2.0)
    }
    val log10Years = log10Seconds - log10(60.0 * 60 * 24 * 365)
    return buildString {
        appendLine("PIN length: $length")
        appendLine("Total combinations: ${"%.0f".format(total)}")
        if (remaining == 0.0) {
            val seconds = first * 1.0 + second * 30.0
            appendLine("Estimated time: ${"%.2f".format(seconds)} seconds")
            appendLine("Estimated years: ${"%.2e".format(seconds / (60*60*24*365))}")
        } else {
            appendLine("Estimated years: 10^${"%.1f".format(log10Years)} years")
            appendLine("(Practically infinite under Android 12+ rate limiting.)")
        }
        appendLine("Note: Android 12+ uses hardware-backed rate limiting (GateKeeper).")
    }
}
