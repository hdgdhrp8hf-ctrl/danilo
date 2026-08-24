package it.tifototitrovo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    TifotoTiTrovoApp()
                }
            }
        }
    }
}

data class ImageResult(
    val text: String,
    val confidence: Float
)

@Composable
fun TifotoTiTrovoApp() {

    val context = LocalContext.current

    var bitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var results by remember {
        mutableStateOf<List<ImageResult>>(emptyList())
    }

    var isAnalyzing by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val imageLabeler: ImageLabeler = remember {
        ImageLabeling.getClient(
            ImageLabelerOptions.DEFAULT_OPTIONS
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            imageLabeler.close()
        }
    }

    fun analyzeImage(newBitmap: Bitmap) {

        bitmap = newBitmap
        results = emptyList()
        errorMessage = null
        isAnalyzing = true

        val image = InputImage.fromBitmap(
            newBitmap,
            0
        )

        imageLabeler.process(image)
            .addOnSuccessListener { labels ->

                results = labels
                    .sortedByDescending { it.confidence }
                    .take(10)
                    .map {
                        ImageResult(
                            text = it.text,
                            confidence = it.confidence
                        )
                    }

                isAnalyzing = false
            }
            .addOnFailureListener { exception ->

                errorMessage =
                    exception.message
                        ?: "Errore durante l'analisi della foto."

                isAnalyzing = false
            }
    }

    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { capturedBitmap ->

            if (capturedBitmap != null) {
                analyzeImage(capturedBitmap)
            }
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                try {

                    val inputStream =
                        context.contentResolver.openInputStream(uri)

                    val selectedBitmap =
                        BitmapFactory.decodeStream(inputStream)

                    inputStream?.close()

                    if (selectedBitmap != null) {
                        analyzeImage(selectedBitmap)
                    } else {
                        errorMessage =
                            "Impossibile leggere la foto."
                    }

                } catch (exception: Exception) {

                    errorMessage =
                        exception.message
                            ?: "Errore durante l'apertura della foto."
                }
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        Text(
            text = "📸 TifotoTiTrovo",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Fotografa o scegli una foto.\nL'AI proverà a riconoscere cosa contiene.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = {
                cameraLauncher.launch(null)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📷 FAI UNA FOTO")
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Button(
            onClick = {
                galleryLauncher.launch("image/*")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🖼️ SCEGLI UNA FOTO")
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        bitmap?.let { imageBitmap ->

            Image(
                bitmap = imageBitmap.asImageBitmap(),
                contentDescription = "Foto analizzata",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }

        if (isAnalyzing) {

            CircularProgressIndicator()

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = "🤖 Analisi in corso..."
            )
        }

        errorMessage?.let { message ->

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = "⚠️ $message",
                color = MaterialTheme.colorScheme.error
            )
        }

        if (results.isNotEmpty()) {

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = "🔎 Ho trovato:",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {

                items(results) { result ->

                    Text(
                        text = "${result.text} — ${(result.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(
                            vertical = 6.dp
                        )
                    )
                }
            }
        }
    }
}
