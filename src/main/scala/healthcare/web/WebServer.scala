package healthcare.web

import healthcare.model._
import healthcare.service._
import zio._
import zio.http._
import zio.stream._
import io.circe.generic.auto._

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
            max-width: 600px;
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
            margin: 8px 0;
        }
        .status {
            font-weight: bold;
            color: #007bff;
        }
        .ai-recommendation {
            margin-top: 20px;
            padding: 20px;
            border: 2px solid #28a745;
            border-radius: 8px;
            background-color: #f8fff9;
        }
        .ai-recommendation h3 {
            color: #28a745;
            margin-top: 0;
            margin-bottom: 15px;
        }
        .ai-recommendation .ai-content {
            white-space: pre-line;
            font-family: inherit;
            margin: 0;
            color: #333;
            line-height: 1.8;
            font-size: 14px;
        }
        .ai-recommendation .ai-content strong {
            color: #2c5530;
            font-weight: 600;
        }
        .back {
            margin-top: 30px;
        }
        .loading {
            text-align: center;
            color: #666;
            font-style: italic;
            padding: 20px;
        }
        .loading-spinner {
            display: inline-block;
            width: 20px;
            height: 20px;
            border: 3px solid #f3f3f3;
            border-top: 3px solid #28a745;
            border-radius: 50%;
            animation: spin 1s linear infinite;
            margin-right: 10px;
        }
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
        .typing-effect {
            border-right: 2px solid #28a745;
            animation: blink 1s infinite;
        }
        @keyframes blink {
            0%, 50% { border-color: #28a745; }
            51%, 100% { border-color: transparent; }
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
            <h1>AI-Powered Health Status Calculator</h1>
            <p style="text-align: center; color: #666; margin-bottom: 30px;">
                Get personalized health analysis and diet recommendations powered by AI
            </p>
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
                <div class="form-group">
                    <label for="age">Age:</label>
                    <input type="number" name="age" min="1" max="150" required>
                </div>
                <button type="submit">Get AI Health Analysis & Diet Plan</button>
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
        <title>Your Health Analysis Results</title>
        $commonStyles
    </head>
    <body>
        <div class="container">
            <h1>Your Health Status Results</h1>
            
            <!-- Basic Health Metrics -->
            <div class="result">
                <p><strong>Age:</strong> ${result.data.age} years</p>
                <p><strong>BMI:</strong> ${Math.round(result.data.bmi * 100.0) / 100.0}</p>
                <p><strong>Status:</strong> <span class="status">${result.status}</span></p>
                <p><strong>Basic Recommendation:</strong> ${result.recommendation}</p>
            </div>
            
            <!-- AI-Powered Recommendations -->
            <div class="ai-recommendation" id="ai-section">
                <h3>AI-Powered Health Analysis & Diet Plan</h3>
                <div id="ai-content" class="loading">
                    <div class="loading-spinner"></div>
                    <span>AI is analyzing your health data and preparing personalized recommendations...</span>
                </div>
            </div>
            
            <div class="back">
                <a href="/" class="button-link">Calculate Another</a>
            </div>
        </div>
        
        <script>
            // Health data for AI analysis
            const healthData = {
                gender: "${result.data.gender}",
                heightCm: ${result.data.heightCm},
                weightKg: ${result.data.weightKg},
                age: ${result.data.age}
            };
            
            // Fetch AI recommendations
            async function loadAIRecommendations() {
                try {
                    const response = await fetch('/ai-recommendations', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(healthData)
                    });
                    
                    if (!response.ok) {
                        throw new Error('AI service temporarily unavailable');
                    }
                    
                    const reader = response.body.getReader();
                    const decoder = new TextDecoder();
                    let aiContent = '';
                    
                    // Clear loading message
                    document.getElementById('ai-content').innerHTML = '<div class="ai-content typing-effect"></div>';
                    const contentDiv = document.querySelector('.ai-content');
                    
                    // Stream the response
                    while (true) {
                        const { done, value } = await reader.read();
                        if (done) break;
                        
                        const chunk = decoder.decode(value, { stream: true });
                        aiContent += chunk;
                        
                        // Update content with typing effect
                        contentDiv.innerHTML = aiContent.replace(/\\n/g, '<br>');
                        
                        // Small delay for typing effect
                        await new Promise(resolve => setTimeout(resolve, 50));
                    }
                    
                    // Remove typing cursor
                    contentDiv.classList.remove('typing-effect');
                    
                } catch (error) {
                    console.error('AI service error:', error);
                    
                    const errorMessage = error.message.includes('billing') ? 
                        'AI service is temporarily unavailable due to billing issues. Please try again later.' :
                        error.message.includes('auth') ?
                        'AI service authentication failed. Please check the API key configuration.' :
                        error.message.includes('rate') ?
                        'AI service is busy. Please try again in a few moments.' :
                        'AI-powered recommendations are currently unavailable. Please try again later.';
                    
                    document.getElementById('ai-section').innerHTML = 
                        '<h3 style="color: #856404;">AI Analysis Status</h3>' +
                        '<p style="color: #856404;">' + errorMessage + '</p>' +
                        '<p style="color: #6c757d; font-size: 0.9em; margin-top: 15px;">' +
                        'Don\\'t worry! The basic health analysis above is still accurate and helpful.' +
                        '</p>';
                    
                    document.getElementById('ai-section').style.borderColor = '#ffc107';
                    document.getElementById('ai-section').style.backgroundColor = '#fffbf0';
                }
            }
            
            // Start loading AI recommendations when page loads
            loadAIRecommendations();
        </script>
    </body>
    </html>
    """

  val app: Http[HealthService with AIAssistantService, Response, Request, Response] = Http.collectZIO[Request] {
    // Serve the form on GET /
    case Method.GET -> Root => 
      ZIO.succeed(Response.html(healthForm))
    
    // Process form data on POST /calculate - show immediate BMI results
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
        age = formData.get("age").flatMap(_.stringValue).flatMap(_.toIntOption).getOrElse(0)
        
        healthData = HealthData(gender, height, weight, age)
        
        // Get basic health status immediately
        basicResult <- HealthService.calculateHealthStatus(healthData)
        
      } yield Response.html(resultPage(basicResult)))
        .catchAll(e => ZIO.succeed(Response.text(e.getMessage).withStatus(Status.InternalServerError)))
    
    // New endpoint for AI recommendations streaming
    case req @ Method.POST -> Root / "ai-recommendations" =>
      (for {
        bodyString <- req.body.asString
        
        // Parse as a simpler JSON structure first
        jsonData <- ZIO.fromEither(io.circe.parser.parse(bodyString))
          .mapError(e => new RuntimeException(s"Invalid JSON: ${e.getMessage}"))
        
        // Extract fields manually to avoid enum issues
        genderStr <- ZIO.fromEither(jsonData.hcursor.get[String]("gender"))
          .mapError(_ => new RuntimeException("Missing gender field"))
        heightCm <- ZIO.fromEither(jsonData.hcursor.get[Double]("heightCm"))
          .mapError(_ => new RuntimeException("Missing heightCm field"))
        weightKg <- ZIO.fromEither(jsonData.hcursor.get[Double]("weightKg"))
          .mapError(_ => new RuntimeException("Missing weightKg field"))
        age <- ZIO.fromEither(jsonData.hcursor.get[Int]("age"))
          .mapError(_ => new RuntimeException("Missing age field"))
        
        // Convert gender string to enum
        gender = genderStr match {
          case "Male" => Gender.Male
          case "Female" => Gender.Female
          case _ => Gender.Other
        }
        
        healthData = HealthData(gender, heightCm, weightKg, age)
        
        // Log the health data for debugging
        _ <- ZIO.logInfo(s"Processing AI request for: gender=${healthData.gender}, age=${healthData.age}, BMI=${healthData.bmi}")
        
        // Get AI recommendations and stream response
        aiResponse <- AIAssistantService.analyzeHealthAndProvideDiet(healthData)
          .tapError(e => ZIO.logError(s"AI service error: ${e.getMessage}"))
        
        _ <- ZIO.logInfo(s"AI response received, length: ${aiResponse.length}")
        
      } yield {
        // Create streaming response
        val byteStream = ZStream.fromIterable(aiResponse.getBytes("UTF-8"))
        
        Response(
          status = Status.Ok,
          headers = Headers(
            Header.ContentType(MediaType.text.plain),
            Header.Custom("Cache-Control", "no-cache"),
            Header.Custom("Connection", "keep-alive")
          ),
          body = Body.fromStream(byteStream)
        )
      }).catchAll { error =>
        // Log the error for debugging
        val errorMsg = error.getMessage
        val logMsg = s"AI service failed: $errorMsg"
        
        val statusCode = if (errorMsg.contains("AI_SERVICE_BILLING_ERROR")) {
          ZIO.logError(s"$logMsg - Billing issue") *> ZIO.succeed(Status.PaymentRequired)
        } else if (errorMsg.contains("AI_SERVICE_AUTH_ERROR")) {
          ZIO.logError(s"$logMsg - Authentication issue") *> ZIO.succeed(Status.Unauthorized)
        } else if (errorMsg.contains("AI_SERVICE_RATE_LIMIT")) {
          ZIO.logError(s"$logMsg - Rate limit exceeded") *> ZIO.succeed(Status.TooManyRequests)
        } else {
          ZIO.logError(s"$logMsg - General service error") *> ZIO.succeed(Status.ServiceUnavailable)
        }
        
        statusCode.map(code => Response.text(errorMsg).withStatus(code))
      }
  }
}