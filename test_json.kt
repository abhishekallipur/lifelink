import com.emergency.medical.data.EmergencyPayload
import java.time.Instant

fun main() {
    // Create a test emergency payload to see the JSON output
    val testPayload = EmergencyPayload(
        name = "John Doe",
        age = 30,
        phone = "+1234567890",
        bloodGroup = "A+",
        phoneBattery = 75,
        latitude = 37.7749,
        longitude = -122.4194,
        message = "I need help with chest pain",
        currentMedicalIssue = "Chest pain, difficulty breathing",
        status = "Pending",
        priority = "High",
        notes = null,
        timestamp = Instant.now().toString(),
        solvedTimestamp = null
    )
    
    println("JSON output:")
    println(testPayload.toJson())
    
    // Verify it can be parsed back
    val jsonString = testPayload.toJson()
    val parsedPayload = EmergencyPayload.fromJson(jsonString)
    println("\nParsed back successfully: ${parsedPayload != null}")
    
    if (parsedPayload != null) {
        println("Name: ${parsedPayload.name}")
        println("Age: ${parsedPayload.age}")
        println("Phone: ${parsedPayload.phone}")
        println("Blood Group: ${parsedPayload.bloodGroup}")
        println("Phone Battery: ${parsedPayload.phoneBattery}")
        println("Latitude: ${parsedPayload.latitude}")
        println("Longitude: ${parsedPayload.longitude}")
        println("Message: ${parsedPayload.message}")
        println("Current Medical Issue: ${parsedPayload.currentMedicalIssue}")
        println("Status: ${parsedPayload.status}")
        println("Priority: ${parsedPayload.priority}")
        println("Notes: ${parsedPayload.notes}")
        println("Timestamp: ${parsedPayload.timestamp}")
        println("Solved Timestamp: ${parsedPayload.solvedTimestamp}")
    }
}
