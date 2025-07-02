package healthcare.mcp

import healthcare.model._
import healthcare.service._
import healthcare.config.AppConfig
import chimp._
import io.circe.Codec
import sttp.tapir.Schema
import zio._

case class HealthAnalysisInput(
  gender: String,
  heightCm: Double,
  weightKg: Double,
  age: Int
) derives Codec, Schema

case class BMICalculationInput(
  heightCm: Double,
  weightKg: Double
) derives Codec, Schema

object HealthMCPTools {
  
  def createTools = {
    
    val healthAnalysisTool = tool("health_analysis")
      .description("Analyzes health data and provides AI-powered diet recommendations with calorie breakdown")
      .input[HealthAnalysisInput]
      .handle { input =>
        try {
          val gender = input.gender.toLowerCase match {
            case "male" => Gender.Male
            case "female" => Gender.Female
            case _ => Gender.Other
          }
          
          val healthData = HealthData(gender, input.heightCm, input.weightKg, input.age)
          
          val bmi = healthData.bmi
          val status = bmi match {
            case bmi if bmi < 18.5 => "Underweight"
            case bmi if bmi < 25.0 => "Normal weight"
            case bmi if bmi < 30.0 => "Overweight"
            case _ => "Obese"
          }
          
          val basicRecommendation = status match {
            case "Underweight" => "Consider consulting with a nutritionist to gain healthy weight through proper diet."
            case "Normal weight" => "Maintain your current healthy habits with balanced nutrition and regular exercise."
            case "Overweight" => "Consider adopting a calorie-controlled diet and increasing physical activity."
            case "Obese" => "Please consult with healthcare professionals for a personalized weight management plan."
          }
          
          Right(s"Health Analysis Results:\nBMI: ${Math.round(bmi * 100.0) / 100.0} ($status)\nRecommendation: $basicRecommendation")
        } catch {
          case e: Exception => Left(s"Error analyzing health data: ${e.getMessage}")
        }
      }
    
    // Simple BMI calculation tool
    val bmiCalculatorTool = tool("calculate_bmi")
      .description("Calculates BMI (Body Mass Index) from height and weight")
      .input[BMICalculationInput]
      .handle { input =>
        try {
          val bmi = input.weightKg / ((input.heightCm / 100) * (input.heightCm / 100))
          val rounded = Math.round(bmi * 100.0) / 100.0
          
          val category = bmi match {
            case bmi if bmi < 18.5 => "Underweight"
            case bmi if bmi < 25.0 => "Normal weight"
            case bmi if bmi < 30.0 => "Overweight"
            case _ => "Obese"
          }
          
          Right(s"BMI: $rounded ($category)")
        } catch {
          case e: Exception => Left(s"Error calculating BMI: ${e.getMessage}")
        }
      }
    
    // Health status assessment tool
    val healthStatusTool = tool("health_status")
      .description("Provides basic health status assessment based on BMI and age")
      .input[HealthAnalysisInput]
      .handle { input =>
        try {
          val gender = input.gender.toLowerCase match {
            case "male" => Gender.Male
            case "female" => Gender.Female
            case _ => Gender.Other
          }
          
          val healthData = HealthData(gender, input.heightCm, input.weightKg, input.age)
          val bmi = healthData.bmi
          
          val status = bmi match {
            case bmi if bmi < 18.5 => HealthStatus.Underweight
            case bmi if bmi < 25.0 => HealthStatus.Normal
            case bmi if bmi < 30.0 => HealthStatus.Overweight
            case _ => HealthStatus.Obese
          }
          
          val ageCategory = input.age match {
            case age if age < 18 => "Minor"
            case age if age <= 64 => "Adult"
            case _ => "Senior"
          }
          
          val assessment = s"""
Health Assessment Results:
- Gender: ${input.gender}
- Age: ${input.age} years ($ageCategory)
- Height: ${input.heightCm} cm
- Weight: ${input.weightKg} kg
- BMI: ${Math.round(bmi * 100.0) / 100.0}
- Status: $status

Basic Recommendation: ${status match {
            case HealthStatus.Underweight => "Consider consulting with a nutritionist to gain healthy weight through proper diet and exercise."
            case HealthStatus.Normal => "Maintain your current healthy habits with balanced nutrition and regular exercise."
            case HealthStatus.Overweight => "Consider adopting a calorie-controlled diet and increasing physical activity."
            case HealthStatus.Obese => "Please consult with healthcare professionals for a personalized weight management plan."
          }}
          """.trim
          
          Right(assessment)
        } catch {
          case e: Exception => Left(s"Error calculating health status: ${e.getMessage}")
        }
      }
    
    List(healthAnalysisTool, bmiCalculatorTool, healthStatusTool)
  }
}
