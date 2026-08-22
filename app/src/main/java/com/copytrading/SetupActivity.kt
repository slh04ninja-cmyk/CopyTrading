package com.copytrading

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.copytrading.api.ApiClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SetupActivity : AppCompatActivity() {

    private lateinit var etHost: TextInputEditText
    private lateinit var etPort: TextInputEditText
    private lateinit var etToken: TextInputEditText
    private lateinit var btnConnect: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        etHost = findViewById(R.id.etHost)
        etPort = findViewById(R.id.etPort)
        etToken = findViewById(R.id.etToken)
        btnConnect = findViewById(R.id.btnConnect)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)

        // Charger les valeurs sauvegardées
        val prefs = getSharedPreferences("copytrading", Context.MODE_PRIVATE)
        etHost.setText(prefs.getString("server_host", ""))
        etPort.setText(prefs.getString("server_port", "8000"))
        etToken.setText(prefs.getString("api_token", ""))

        // Si déjà configuré, tester directement
        if (prefs.getString("server_host", "")?.isNotEmpty() == true) {
            testAndNavigate()
        }

        btnConnect.setOnClickListener {
            val host = etHost.text.toString().trim()
            val port = etPort.text.toString().trim()
            val token = etToken.text.toString().trim()

            if (host.isEmpty()) {
                etHost.error = "Requis"
                return@setOnClickListener
            }

            // Sauvegarder
            prefs.edit()
                .putString("server_host", host)
                .putString("server_port", port.ifEmpty { "8000" })
                .putString("api_token", token)
                .apply()

            testAndNavigate()
        }
    }

    private fun testAndNavigate() {
        progressBar.visibility = View.VISIBLE
        tvStatus.visibility = View.VISIBLE
        tvStatus.text = "Connexion au serveur..."
        btnConnect.isEnabled = false

        val client = ApiClient(this)
        lifecycleScope.launch {
            val ok = client.testConnection()
            if (ok) {
                tvStatus.text = "✅ Connecté !"
                progressBar.visibility = View.GONE
                startActivity(Intent(this@SetupActivity, MainActivity::class.java))
                finish()
            } else {
                progressBar.visibility = View.GONE
                tvStatus.text = "❌ Impossible de se connecter au serveur"
                tvStatus.setTextColor(getColor(R.color.danger))
                btnConnect.isEnabled = true
            }
        }
    }
}
