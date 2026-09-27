import org.springframework.ai.ollama.api.*
import ai.djl.fasttext.*
import com.serranofp.builder.lambda.build
import kotlin.io.path.Path

fun test() {
    val options = build<OllamaChatOptions, *> {
        model(OllamaModel.LLAMA3_1)
        temperature = 0.4
    }
    val text = build<FtTrainingConfig, *> {
        modelName = "hello"
        outputDir = Path("./output")
        threads = 4
    }
}
