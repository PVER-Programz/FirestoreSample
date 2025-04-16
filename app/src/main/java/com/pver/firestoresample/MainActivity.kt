package com.pver.firestoresample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val ageInput = findViewById<EditText>(R.id.ageInput)
        val saveBtn = findViewById<Button>(R.id.saveBtn)
        val fetchBtn = findViewById<Button>(R.id.fetchBtn)
        val outputText = findViewById<TextView>(R.id.outputText)

        db = FirebaseFirestore.getInstance()

        saveBtn.setOnClickListener {
            val name = nameInput.text.toString()
            val age = ageInput.text.toString().toIntOrNull()

            if (name.isEmpty() || age == null) {
                Toast.makeText(this, "Enter valid data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = hashMapOf(
                "name" to name,
                "age" to age
            )

            db.collection("users")
                .add(user)
                .addOnSuccessListener {
                    Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show()
                }
        }

        fetchBtn.setOnClickListener {
            db.collection("users")
                .get()
                .addOnSuccessListener { result ->
                    var data = ""
                    for (document in result) {
                        val name = document.getString("name")
                        val age = document.getLong("age")
                        data += "Name: $name, Age: $age\n"
                    }
                    outputText.text = data
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to fetch", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
