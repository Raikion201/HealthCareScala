package healthcare.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

// Gender enumeration
enum Gender:
  case Male, Female, Other

given Encoder[Gender] = Encoder[String].contramap(_.toString)
given Decoder[Gender] = Decoder[String].emap {
  case "Male" => Right(Gender.Male)
  case "Female" => Right(Gender.Female)
  case "Other" => Right(Gender.Other)
  case other => Left(s"Invalid gender: $other")
}

// Health data class
case class HealthData(
  gender: Gender,
  heightCm: Double,
  weightKg: Double,
  age: Int
) {
  def bmi: Double = weightKg / ((heightCm / 100) * (heightCm / 100))
}

given Encoder[HealthData] = deriveEncoder[HealthData]
given Decoder[HealthData] = deriveDecoder[HealthData]

// Health status based on BMI
enum HealthStatus:
  case Underweight, Normal, Overweight, Obese

case class HealthResult(
  data: HealthData,
  status: HealthStatus,
  recommendation: String
)
