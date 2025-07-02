package healthcare.model

import zio._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._


// Gender enumeration - functional style
enum Gender:
  case Male, Female, Other

given Encoder[Gender] = Encoder[String].contramap(_.toString)
given Decoder[Gender] = Decoder[String].emap {
  case "Male" => Right(Gender.Male)
  case "Female" => Right(Gender.Female)
  case "Other" => Right(Gender.Other)
  case other => Left(s"Invalid gender: $other")
}

final case class HealthData(
  gender: Gender,
  heightCm: Double,
  weightKg: Double,
  age: Int
) {
  val bmi: Double = weightKg / ((heightCm / 100) * (heightCm / 100))
  
  // Validation as pure function
  def validate: Either[String, HealthData] = 
    if (heightCm <= 0) Left("Height must be positive")
    else if (weightKg <= 0) Left("Weight must be positive") 
    else if (age <= 0) Left("Age must be positive")
    else Right(this)
}

given Encoder[HealthData] = deriveEncoder[HealthData]
given Decoder[HealthData] = deriveDecoder[HealthData]

enum HealthStatus:
  case Underweight, Normal, Overweight, Obese

final case class HealthResult(
  data: HealthData,
  status: HealthStatus,
  recommendation: String
)

sealed trait HealthError extends Throwable {
  def message: String
  override def getMessage: String = message
}

object HealthError {
  final case class ValidationError(message: String) extends HealthError
  final case class CalculationError(message: String) extends HealthError
  final case class AIServiceError(message: String) extends HealthError
  final case class ConfigurationError(message: String) extends HealthError
}
