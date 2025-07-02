package healthcare.service

import healthcare.model._
import zio._

object HealthService {
  
  private def calculateBMIStatus(bmi: Double): HealthStatus = bmi match {
    case bmi if bmi < 18.5 => HealthStatus.Underweight
    case bmi if bmi < 25.0 => HealthStatus.Normal
    case bmi if bmi < 30.0 => HealthStatus.Overweight
    case _ => HealthStatus.Obese
  }
  
  private def generateRecommendation(status: HealthStatus): String = status match {
    case HealthStatus.Underweight => "Consider consulting with a nutritionist to gain healthy weight."
    case HealthStatus.Normal => "Maintain your current healthy habits."
    case HealthStatus.Overweight => "Consider adopting a balanced diet and regular exercise."
    case HealthStatus.Obese => "Please consult with healthcare professionals for a personalized plan."
  }
  
  def calculateHealthStatus(data: HealthData): Task[HealthResult] =
    ZIO.fromEither(data.validate)
      .mapError(HealthError.ValidationError.apply)
      .map { validData =>
        val status = calculateBMIStatus(validData.bmi)
        val recommendation = generateRecommendation(status)
        HealthResult(validData, status, recommendation)
      }
  
  def calculateBMI(heightCm: Double, weightKg: Double): UIO[Double] =
    ZIO.succeed(weightKg / ((heightCm / 100) * (heightCm / 100)))
  
  def validateHealthData(data: HealthData): IO[HealthError.ValidationError, HealthData] =
    ZIO.fromEither(data.validate).mapError(HealthError.ValidationError.apply)
}
