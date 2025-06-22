package healthcare.web

import healthcare.model._
import healthcare.service._
import zio._
import zio.http._

object WebServer {
  private val commonStyles =
    """
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            background-color: #f4f7f6;
            margin: 0;
            padding: 40px 20px;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            box-sizing: border-box;
        }
        .container {
            background-color: #ffffff;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
            width: 100%;
            max-width: 500px;
        }
        h1 {
            color: #333;
            text-align: center;
            margin-bottom: 30px;
        }
        button, .button-link {
            display: block;
            width: 100%;
            padding: 12px;
            background-color: #007bff;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 16px;
            text-align: center;
            text-decoration: none;
            transition: background-color 0.3s ease;
        }
        button:hover, .button-link:hover {
            background-color: #0056b3;
        }
        .form-group {
            margin-bottom: 20px;
        }
        label {
            display: block;
            margin-bottom: 8px;
            font-weight: 600;
            color: #555;
        }
        input[type="number"], select {
            width: 100%;
            padding: 10px;
            border: 1px solid #ccc;
            border-radius: 4px;
            box-sizing: border-box;
        }
        .result {
            margin: 20px 0;
            padding: 20px;
            border: 1px solid #e0e0e0;
            border-radius: 8px;
            background-color: #fafafa;
        }
        .result p {
            font-size: 1.1em;
            color: #333;
            line-height: 1.6;
        }
        .status {
            font-weight: bold;
            color: #007bff;
        }
        .back {
            margin-top: 30px;
        }
    </style>
    """

  private val healthForm = 
    s"""
    <!DOCTYPE html>
    <html>
    <head>
        <title>Health Status Calculator</title>
        $commonStyles
    </head>
    <body>
        <div class="container">
            <h1>Health Status Calculator</h1>
            <form action="/calculate" method="post">
                <div class="form-group">
                    <label for="gender">Gender:</label>
                    <select name="gender" required>
                        <option value="Male">Male</option>
                        <option value="Female">Female</option>
                        <option value="Other">Other</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="height">Height (cm):</label>
                    <input type="number" name="height" step="0.01" required>
                </div>
                <div class="form-group">
                    <label for="weight">Weight (kg):</label>
                    <input type="number" name="weight" step="0.01" required>
                </div>
                <button type="submit">Calculate</button>
            </form>
        </div>
    </body>
    </html>
    """

  private def resultPage(result: HealthResult): String = 
    s"""
    <!DOCTYPE html>
    <html>
    <head>
        <title>Health Status Result</title>
        $commonStyles
    </head>
    <body>
        <div class="container">
            <h1>Your Health Status</h1>
            <div class="result">
                <p>BMI: ${Math.round(result.data.bmi)}</p>
                <p>Status: <span class="status">${result.status}</span></p>
                <p>Recommendation: ${result.recommendation}</p>
            </div>
            <div class="back">
                <a href="/" class="button-link">Calculate Another</a>
            </div>
        </div>
    </body>
    </html>
    """

  val app: Http[HealthService, Response, Request, Response] = Http.collectZIO[Request] {
    // Serve the form on GET /
    case Method.GET -> Root => 
      ZIO.succeed(Response.html(healthForm))
    
    // Process form data on POST /calculate
    case req @ Method.POST -> Root / "calculate" => 
      (for {
        formData <- req.body.asURLEncodedForm
        
        genderStr = formData.get("gender").flatMap(_.stringValue).getOrElse("Other")
        gender = genderStr match {
          case "Male" => Gender.Male
          case "Female" => Gender.Female
          case _ => Gender.Other
        }
        
        height = formData.get("height").flatMap(_.stringValue).flatMap(_.toDoubleOption).getOrElse(0.0)
        weight = formData.get("weight").flatMap(_.stringValue).flatMap(_.toDoubleOption).getOrElse(0.0)
        
        healthData = HealthData(gender, height, weight)
        result <- HealthService.calculateHealthStatus(healthData)
      } yield Response.html(resultPage(result)))
        .catchAll(e => ZIO.succeed(Response.text(e.getMessage).withStatus(Status.InternalServerError)))
  }
}
