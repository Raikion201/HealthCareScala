package healthcare.service

import healthcare.model._
import zio._

// Health service interface
trait HealthService {
  def calculateHealthStatus(data: HealthData): Task[HealthResult]
}

// Implementation of the health service
class HealthServiceImpl extends HealthService {
  def calculateHealthStatus(data: HealthData): Task[HealthResult] = ZIO.succeed {
    val bmi = data.bmi
    
    val status = bmi match {
      case bmi if bmi < 18.5 => HealthStatus.Underweight
      case bmi if bmi < 25.0 => HealthStatus.Normal
      case bmi if bmi < 30.0 => HealthStatus.Overweight
      case _ => HealthStatus.Obese
    }
    
    val recommendation = status match {
      case HealthStatus.Underweight => "Consider consulting with a nutritionist to gain healthy weight."
      case HealthStatus.Normal => "Maintain your current healthy habits."
      case HealthStatus.Overweight => "Consider adopting a balanced diet and regular exercise."
      case HealthStatus.Obese => "Please consult with healthcare professionals for a personalized plan."
    }
    
    HealthResult(data, status, recommendation)
  }
}

// ZIO accessor pattern for the health service
object HealthService {
  def calculateHealthStatus(data: HealthData): ZIO[HealthService, Throwable, HealthResult] =
    ZIO.serviceWithZIO[HealthService](_.calculateHealthStatus(data))
    
  // Layer for dependency injection
  val live: ULayer[HealthService] = ZLayer.succeed(new HealthServiceImpl())
}
