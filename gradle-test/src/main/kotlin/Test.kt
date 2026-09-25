import org.springframework.ai.ollama.api.*
import com.serranofp.builder.lambda.build

fun test() {
    val options = build<OllamaChatOptions, *> {
        model = OllamaModel.LLAMA3_1
        temperature = 0.4
    }
}
