package healthcare.service

import healthcare.model._
import healthcare.config.AppConfig
import zio._
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import sttp.client4._
import sttp.client4.circe._

case class DeepSeekMessage(role: String, content: String)
case class DeepSeekRequest(model: String, messages: List[DeepSeekMessage], temperature: Double = 0.7)
case class DeepSeekChoice(message: DeepSeekMessage)
case class DeepSeekResponse(choices: List[DeepSeekChoice])

// Error response structure
case class DeepSeekError(message: String, `type`: String, code: String)
case class DeepSeekErrorResponse(error: DeepSeekError)

given Codec[DeepSeekMessage] = Codec.from(
  Decoder.forProduct2("role", "content")(DeepSeekMessage.apply),
  Encoder.forProduct2("role", "content")(m => (m.role, m.content))
)

given Codec[DeepSeekRequest] = Codec.from(
  Decoder.forProduct3("model", "messages", "temperature")(DeepSeekRequest.apply),
  Encoder.forProduct3("model", "messages", "temperature")(r => (r.model, r.messages, r.temperature))
)

given Codec[DeepSeekChoice] = Codec.from(
  Decoder.forProduct1("message")(DeepSeekChoice.apply),
  Encoder.forProduct1("message")(_.message)
)

given Codec[DeepSeekResponse] = Codec.from(
  Decoder.forProduct1("choices")(DeepSeekResponse.apply),
  Encoder.forProduct1("choices")(_.choices)
)

given Codec[DeepSeekError] = Codec.from(
  Decoder.forProduct3("message", "type", "code")(DeepSeekError.apply),
  Encoder.forProduct3("message", "type", "code")(e => (e.message, e.`type`, e.code))
)

given Codec[DeepSeekErrorResponse] = Codec.from(
  Decoder.forProduct1("error")(DeepSeekErrorResponse.apply),
  Encoder.forProduct1("error")(_.error)
)

trait AIAssistantService {
  def analyzeHealthAndProvideDiet(healthData: HealthData): Task[String]
}

class AIAssistantServiceImpl(config: AppConfig) extends AIAssistantService {
  
  def analyzeHealthAndProvideDiet(healthData: HealthData): Task[String] = {
    val prompt = createHealthAnalysisPrompt(healthData)
    
    ZIO.attemptBlocking {
      val backend = DefaultSyncBackend()
      
      try {
        val request = DeepSeekRequest(
          model = config.deepSeek.model,
          messages = List(
            DeepSeekMessage("system", "You are a professional nutritionist and health advisor. Provide detailed, personalized health analysis and diet recommendations."),
            DeepSeekMessage("user", prompt)
          )
        )
        
        val httpRequest = basicRequest
          .post(uri"${config.deepSeek.baseUrl}/chat/completions")
          .header("Authorization", s"Bearer ${config.deepSeek.apiKey}")
          .header("Content-Type", "application/json")
          .body(request.asJson.noSpaces)
          .response(asStringAlways)
        
        val response = httpRequest.send(backend)
        
        // Handle response based on status code
        response.code.code match {
          case 200 =>
            // Parse successful response
            parse(response.body).flatMap(_.as[DeepSeekResponse]) match {
              case Right(deepSeekResponse) =>
                deepSeekResponse.choices.headOption
                  .map(_.message.content)
                  .getOrElse("Sorry, I couldn't generate a response. Please try again.")
              case Left(_) =>
                throw new RuntimeException("AI_SERVICE_UNAVAILABLE")
            }
          
          case 402 =>
            // Insufficient balance
            parse(response.body).flatMap(_.as[DeepSeekErrorResponse]) match {
              case Right(errorResponse) =>
                throw new RuntimeException(s"AI_SERVICE_BILLING_ERROR: ${errorResponse.error.message}")
              case Left(_) =>
                throw new RuntimeException("AI_SERVICE_BILLING_ERROR: Insufficient Balance")
            }
          
          case 401 =>
            // Invalid API key
            throw new RuntimeException("AI_SERVICE_AUTH_ERROR: Invalid API Key")
          
          case 429 =>
            // Rate limit exceeded
            throw new RuntimeException("AI_SERVICE_RATE_LIMIT: Rate limit exceeded")
          
          case _ =>
            // Other errors
            parse(response.body).flatMap(_.as[DeepSeekErrorResponse]) match {
              case Right(errorResponse) =>
                throw new RuntimeException(s"AI_SERVICE_ERROR: ${errorResponse.error.message}")
              case Left(_) =>
                throw new RuntimeException(s"AI_SERVICE_ERROR: HTTP ${response.code.code}")
            }
        }
      } finally {
        backend.close()
      }
    }.catchAll { error =>
      val errorMessage = error.getMessage
      ZIO.logError(s"AI Assistant Service Error: $errorMessage") *>
      ZIO.fail(new RuntimeException(errorMessage))
    }
  }
  
  private def createHealthAnalysisPrompt(healthData: HealthData): String = {
    val bmi = healthData.bmi
    val bmiStatus = bmi match {
      case bmi if bmi < 18.5 => "Underweight"
      case bmi if bmi < 25.0 => "Normal weight"
      case bmi if bmi < 30.0 => "Overweight"
      case _ => "Obese"
    }
    
    s"""
Please analyze the following health data and provide a comprehensive assessment in PLAIN TEXT format without any emojis or special characters:

Personal Information:
- Gender: ${healthData.gender}
- Age: ${healthData.age} years
- Height: ${healthData.heightCm} cm
- Weight: ${healthData.weightKg} kg
- BMI: ${Math.round(bmi * 100.0) / 100.0} ($bmiStatus)

Please provide your response in the following EXACT format using simple text and line breaks:

HEALTH STATUS ASSESSMENT
Based on your BMI of ${Math.round(bmi * 100.0) / 100.0}, you are classified as $bmiStatus.
[Provide detailed health assessment here]

DAILY DIET RECOMMENDATIONS

BREAKFAST
- [List breakfast items]

LUNCH  
- [List lunch items]

DINNER
- [List dinner items]

SNACKS
- [List healthy snack options]

CALORIE BREAKDOWN
Total Daily Calories: [amount] kcal
- Breakfast: [amount] kcal
- Lunch: [amount] kcal  
- Dinner: [amount] kcal
- Snacks: [amount] kcal

HEALTH TIPS
- [Tip 1]
- [Tip 2]
- [Tip 3]

IMPORTANT: Do NOT use any emojis, special symbols, markdown formatting, or tables. Use only plain text with simple line breaks and dashes for lists. Keep the response clear and well-structured but simple.
"""
  }
}

object AIAssistantService {
  def analyzeHealthAndProvideDiet(healthData: HealthData): ZIO[AIAssistantService, Throwable, String] =
    ZIO.serviceWithZIO[AIAssistantService](_.analyzeHealthAndProvideDiet(healthData))
  
  val live: ZLayer[AppConfig, Throwable, AIAssistantService] = ZLayer.fromZIO {
    for {
      config <- ZIO.service[AppConfig]
    } yield new AIAssistantServiceImpl(config)
  }
}