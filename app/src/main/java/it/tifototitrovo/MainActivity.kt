package it.tifototitrovo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TifotoTiTrovo() }
    }
}

@Composable
fun TifotoTiTrovo() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var labels by remember { mutableStateOf<List<String>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }

    fun analyze(bmp: Bitmap) {
        bitmap = bmp
        labels = emptyList()
        busy = true
        val image = InputImage.fromBitmap(bmp, 0)
        ImageLabeling.getClient().process(image)
            .addOnSuccessListener { result ->
                labels = result.sortedByDescending { it.confidence }
                    .take(10)
                    .filter { it.confidence >= 0.35f }
                    .map { "${it.text} — ${(it.confidence * 100).toInt()}%" }
                busy = false
            }
            .addOnFailureListener {
                labels = listOf("Errore: ${it.message ?: "analisi non riuscita"}")
                busy = false
            }
    }

    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp -> if (bmp != null) analyze(bmp) }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) camera.launch(null) }

    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("TifotoTiTrovo") }) }
        ) { padding ->
            LazyColumn(
                Modifier.padding(padding).fillMaxSize().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("📸 Fai solo la foto", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Primo test: l'AI riconosce automaticamente ciò che vede. Non devi scrivere nulla.",
                        fontSize = 14.sp
                    )
                }
                item {
                    Button(
                        onClick = {
                            if (context.checkSelfPermission(Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_GRANTED) camera.launch(null)
                            else permission.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("FOTOGRAFA") }
                }
                item {
                    if (busy) CircularProgressIndicator()
                    if (labels.isNotEmpty()) {
                        Column(Modifier.fillMaxWidth()) {
                            Text("🤖 Riconosciuto", style = MaterialTheme.typography.titleLarge)
                            labels.forEach { Text("• $it") }
                        }
                    }
                }
            }
        }
    }
}
