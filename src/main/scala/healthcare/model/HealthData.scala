package healthcare.model

// Gender enumeration
enum Gender:
  case Male, Female, Other

// Health data class
case class HealthData(
  gender: Gender,
  heightCm: Double,
  weightKg: Double
) {
  def bmi: Double = weightKg / ((heightCm / 100) * (heightCm / 100))
}

// Health status based on BMI
enum HealthStatus:
  case Underweight, Normal, Overweight, Obese

case class HealthResult(
  data: HealthData,
  status: HealthStatus,
  recommendation: String
)
