package com.embedsuite.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedsuite.app.core.orchestrator.DuckyBlock
import com.embedsuite.app.core.orchestrator.Key
import com.embedsuite.app.ui.theme.*

@Composable
fun BadUsbBlockEditor(
    blocks: List<DuckyBlock>,
    onBlocksChange: (List<DuckyBlock>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdd by remember { mutableStateOf(false) }
    Column(modifier) {
        Text("Bloques visuales", color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        LazyColumn(
            modifier = Modifier.heightIn(max = 220.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(blocks) { index, block ->
                Row(
                    Modifier.fillMaxWidth().background(DarkSurfaceElevated).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(blockLabel(block), color = MatrixGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        onBlocksChange(blocks.filterIndexed { i, _ -> i != index })
                    }) { Icon(Icons.Default.Delete, null, tint = NeonRed, modifier = Modifier.size(18.dp)) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
            OutlinedButton(onClick = { showAdd = true }) { Text("+ Bloque", fontSize = 9.sp) }
            OutlinedButton(onClick = { onBlocksChange(blocks + DuckyBlock.Delay(500)) }) { Text("+ Esperar", fontSize = 9.sp) }
            OutlinedButton(onClick = { onBlocksChange(blocks + DuckyBlock.KeyPress(Key.ENTER)) }) { Text("+ ENTER", fontSize = 9.sp) }
        }
    }
    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Añadir bloque", fontFamily = FontFamily.Monospace) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        onBlocksChange(blocks + DuckyBlock.StringText("texto"))
                        showAdd = false
                    }) { Text("Escribir texto") }
                    OutlinedButton(onClick = {
                        onBlocksChange(blocks + DuckyBlock.Delay(1000))
                        showAdd = false
                    }) { Text("Esperar 1s") }
                    OutlinedButton(onClick = {
                        onBlocksChange(blocks + DuckyBlock.KeyPress(Key.ENTER))
                        showAdd = false
                    }) { Text("Presionar ENTER") }
                }
            },
            confirmButton = { TextButton(onClick = { showAdd = false }) { Text("Cerrar") } }
        )
    }
}

private fun blockLabel(block: DuckyBlock): String = when (block) {
    is DuckyBlock.Comment -> "REM ${block.text}"
    is DuckyBlock.Delay -> "Esperar ${block.ms}ms"
    is DuckyBlock.StringText -> "Escribir: \"${block.text}\""
    is DuckyBlock.KeyPress -> "Tecla ${block.key.token}"
    is DuckyBlock.Combo -> block.keys.joinToString("+") { it.token }
    is DuckyBlock.Repeat -> "Repetir ${block.count}x"
}
